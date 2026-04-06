import uuid
import logging

from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import RedirectResponse

from app.schemas.models import (
    InterviewResponse,
    RescheduleInterviewRequest,
    ScheduleInterviewRequest,
)
from app.services.google_calendar import get_auth_url, exchange_code_for_token
from app.services.interview_repository import (
    get_interview,
    list_interviews_for_candidate,
    list_interviews_for_job,
)
from app.services.scheduler_service import (
    cancel_interview,
    reschedule_interview,
    schedule_interview,
)

logger = logging.getLogger(__name__)

# ── Interview routes ──────────────────────────────────────────────────────────

interview_router = APIRouter(prefix="/interviews", tags=["interviews"])


@interview_router.post("/schedule", response_model=InterviewResponse, status_code=201)
async def schedule(req: ScheduleInterviewRequest):
    """
    Schedule an interview. Checks Google Calendar availability across
    the provided preferred windows and books the first free slot.
    """
    try:
        return await schedule_interview(req)
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc))
    except Exception as exc:
        logger.exception("Unexpected error scheduling interview")
        raise HTTPException(status_code=500, detail="Failed to schedule interview")


@interview_router.delete("/{interview_id}", response_model=InterviewResponse)
async def cancel(interview_id: str, reason: str = Query(default="")):
    """Cancel an interview and delete the Google Calendar event."""
    try:
        return await cancel_interview(interview_id, reason)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc))
    except Exception as exc:
        logger.exception("Unexpected error cancelling interview")
        raise HTTPException(status_code=500, detail="Failed to cancel interview")


@interview_router.patch("/{interview_id}/reschedule", response_model=InterviewResponse)
async def reschedule(interview_id: str, req: RescheduleInterviewRequest):
    """Reschedule an interview to a new time slot."""
    try:
        return await reschedule_interview(interview_id, req)
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc))
    except Exception as exc:
        logger.exception("Unexpected error rescheduling interview")
        raise HTTPException(status_code=500, detail="Failed to reschedule interview")


@interview_router.get("/{interview_id}", response_model=InterviewResponse)
async def get_one(interview_id: str):
    interview = await get_interview(interview_id)
    if not interview:
        raise HTTPException(status_code=404, detail="Interview not found")
    return interview


@interview_router.get("/candidate/{candidate_id}", response_model=list[InterviewResponse])
async def get_by_candidate(candidate_id: str):
    return await list_interviews_for_candidate(candidate_id)


@interview_router.get("/job/{job_id}", response_model=list[InterviewResponse])
async def get_by_job(job_id: str):
    return await list_interviews_for_job(job_id)


# ── Google OAuth2 routes ──────────────────────────────────────────────────────

auth_router = APIRouter(prefix="/auth", tags=["auth"])


@auth_router.get("/google")
async def google_auth():
    """
    Step 1: Redirect recruiter to Google's OAuth2 consent screen.
    Visit this URL once to authorise the service to use Google Calendar.
    """
    state = str(uuid.uuid4())
    auth_url = get_auth_url(state)
    return RedirectResponse(url=auth_url)


@auth_router.get("/google/callback")
async def google_callback(code: str, state: str):
    """
    Step 2: Google redirects here with the auth code.
    We exchange it for tokens and save them to token.json.
    """
    try:
        exchange_code_for_token(code)
        return {"message": "Google Calendar authorised successfully. You can close this tab."}
    except Exception as exc:
        logger.exception("OAuth2 callback failed")
        raise HTTPException(status_code=400, detail=f"OAuth2 failed: {exc}")
