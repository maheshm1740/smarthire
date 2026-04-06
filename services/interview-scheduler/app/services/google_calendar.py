import logging
import os
from datetime import datetime, timedelta

from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import Flow
from googleapiclient.discovery import build

from app.core.config import settings

logger = logging.getLogger(__name__)

SCOPES = ["https://www.googleapis.com/auth/calendar"]

# In-memory store for OAuth2 state (use Redis in production)
_oauth_states: dict[str, str] = {}


def get_google_credentials() -> Credentials | None:
    """
    Load credentials from the local token file.
    Returns None if not yet authorised.
    """
    if not os.path.exists(settings.GOOGLE_TOKEN_FILE):
        return None

    creds = Credentials.from_authorized_user_file(settings.GOOGLE_TOKEN_FILE, SCOPES)

    if creds and creds.expired and creds.refresh_token:
        creds.refresh(Request())
        _save_credentials(creds)

    return creds if creds and creds.valid else None


def _save_credentials(creds: Credentials) -> None:
    with open(settings.GOOGLE_TOKEN_FILE, "w") as f:
        f.write(creds.to_json())
    logger.info("Google credentials saved", extra={"file": settings.GOOGLE_TOKEN_FILE})


def get_auth_url(state: str) -> str:
    """Generate the Google OAuth2 authorisation URL."""
    flow = Flow.from_client_config(
        {
            "web": {
                "client_id": settings.GOOGLE_CLIENT_ID,
                "client_secret": settings.GOOGLE_CLIENT_SECRET,
                "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                "token_uri": "https://oauth2.googleapis.com/token",
                "redirect_uris": [settings.GOOGLE_REDIRECT_URI],
            }
        },
        scopes=SCOPES,
        redirect_uri=settings.GOOGLE_REDIRECT_URI,
    )
    auth_url, _ = flow.authorization_url(
        access_type="offline",
        include_granted_scopes="true",
        state=state,
        prompt="consent",
    )
    _oauth_states[state] = state
    return auth_url


def exchange_code_for_token(code: str) -> Credentials:
    """Exchange the OAuth2 callback code for credentials and save them."""
    flow = Flow.from_client_config(
        {
            "web": {
                "client_id": settings.GOOGLE_CLIENT_ID,
                "client_secret": settings.GOOGLE_CLIENT_SECRET,
                "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                "token_uri": "https://oauth2.googleapis.com/token",
                "redirect_uris": [settings.GOOGLE_REDIRECT_URI],
            }
        },
        scopes=SCOPES,
        redirect_uri=settings.GOOGLE_REDIRECT_URI,
    )
    flow.fetch_token(code=code)
    creds = flow.credentials
    _save_credentials(creds)
    return creds


def get_calendar_service():
    """Return an authorised Google Calendar API service instance."""
    creds = get_google_credentials()
    if not creds:
        raise RuntimeError(
            "Google Calendar not authorised. "
            "Visit /auth/google to complete the OAuth2 flow."
        )
    return build("calendar", "v3", credentials=creds)


def check_availability(
    start: datetime,
    end: datetime,
    calendar_id: str = "primary",
) -> bool:
    """
    Returns True if the time slot is free on the given calendar.
    Uses the freebusy API to check for conflicts.
    """
    service = get_calendar_service()
    body = {
        "timeMin": start.isoformat() + "Z",
        "timeMax": end.isoformat() + "Z",
        "items": [{"id": calendar_id}],
    }
    result = service.freebusy().query(body=body).execute()
    busy_slots = result["calendars"][calendar_id]["busy"]
    return len(busy_slots) == 0


def create_calendar_event(
    title: str,
    description: str,
    start: datetime,
    end: datetime,
    attendee_emails: list[str],
    calendar_id: str = "primary",
) -> dict:
    """
    Create a Google Calendar event with a Meet link and return the event dict.
    """
    service = get_calendar_service()

    event_body = {
        "summary": title,
        "description": description,
        "start": {"dateTime": start.isoformat(), "timeZone": "UTC"},
        "end": {"dateTime": end.isoformat(), "timeZone": "UTC"},
        "attendees": [{"email": email} for email in attendee_emails],
        "conferenceData": {
            "createRequest": {
                "requestId": f"smarthire-{start.timestamp()}",
                "conferenceSolutionKey": {"type": "hangoutsMeet"},
            }
        },
        "reminders": {
            "useDefault": False,
            "overrides": [
                {"method": "email", "minutes": 24 * 60},
                {"method": "popup", "minutes": 30},
            ],
        },
    }

    event = (
        service.events()
        .insert(calendarId=calendar_id, body=event_body, conferenceDataVersion=1)
        .execute()
    )
    logger.info("Calendar event created", extra={"eventId": event["id"]})
    return event


def update_calendar_event(
    event_id: str,
    start: datetime,
    end: datetime,
    calendar_id: str = "primary",
) -> dict:
    """Update the time of an existing calendar event."""
    service = get_calendar_service()

    event = service.events().get(calendarId=calendar_id, eventId=event_id).execute()
    event["start"] = {"dateTime": start.isoformat(), "timeZone": "UTC"}
    event["end"] = {"dateTime": end.isoformat(), "timeZone": "UTC"}

    updated = (
        service.events()
        .update(calendarId=calendar_id, eventId=event_id, body=event)
        .execute()
    )
    logger.info("Calendar event updated", extra={"eventId": event_id})
    return updated


def delete_calendar_event(event_id: str, calendar_id: str = "primary") -> None:
    """Delete a calendar event by ID."""
    service = get_calendar_service()
    service.events().delete(calendarId=calendar_id, eventId=event_id).execute()
    logger.info("Calendar event deleted", extra={"eventId": event_id})
