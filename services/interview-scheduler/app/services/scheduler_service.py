import logging
import uuid
from datetime import timedelta

from app.publishers.interview_publisher import (
    publish_interview_cancelled,
    publish_interview_rescheduled,
    publish_interview_scheduled,
)
from app.schemas.models import (
    InterviewCancelledEvent,
    InterviewResponse,
    InterviewRescheduledEvent,
    InterviewScheduledEvent,
    InterviewStatus,
    RescheduleInterviewRequest,
    ScheduleInterviewRequest,
    TimeWindow,
)
from app.services.google_calendar import (
    check_availability,
    create_calendar_event,
    delete_calendar_event,
    update_calendar_event,
)
from app.services.interview_repository import (
    create_interview,
    get_interview,
    update_interview,
)

logger = logging.getLogger(__name__)


def _find_available_slot(
    windows: list[TimeWindow],
    duration_minutes: int,
) -> TimeWindow | None:
    """
    Iterate through the preferred windows and return the first one
    that is free on the recruiter's Google Calendar.
    """
    for window in windows:
        end = window.start + timedelta(minutes=duration_minutes)
        try:
            if check_availability(window.start, end):
                return TimeWindow(start=window.start, end=end)
        except Exception as exc:
            logger.warning(
                "Availability check failed for window",
                extra={"start": str(window.start), "error": str(exc)},
            )
    return None


async def schedule_interview(req: ScheduleInterviewRequest) -> InterviewResponse:
    """
    1. Find first available slot from preferred windows
    2. Create Google Calendar event with Meet link
    3. Store interview document in MongoDB
    4. Publish interview.scheduled to Kafka
    """
    slot = _find_available_slot(req.preferredWindows, req.durationMinutes)
    if not slot:
        raise ValueError("No available slots found in the provided time windows.")

    # Create Google Calendar event
    google_event = None
    google_event_id = None
    google_meet_link = None

    try:
        google_event = create_calendar_event(
            title=req.title,
            description=req.description,
            start=slot.start,
            end=slot.end,
            attendee_emails=[req.candidateEmail, req.recruiterEmail],
        )
        google_event_id = google_event.get("id")
        google_meet_link = (
            google_event.get("conferenceData", {})
            .get("entryPoints", [{}])[0]
            .get("uri")
        )
    except Exception as exc:
        logger.warning(
            "Google Calendar event creation failed — proceeding without it",
            extra={"error": str(exc)},
        )

    # Store in MongoDB
    interview = await create_interview(
        {
            "candidateId": req.candidateId,
            "jobId": req.jobId,
            "recruiterId": req.recruiterId,
            "candidateEmail": req.candidateEmail,
            "recruiterEmail": req.recruiterEmail,
            "status": InterviewStatus.SCHEDULED,
            "startTime": slot.start,
            "endTime": slot.end,
            "durationMinutes": req.durationMinutes,
            "googleEventId": google_event_id,
            "googleMeetLink": google_meet_link,
        }
    )

    # Publish Kafka event
    await publish_interview_scheduled(
        InterviewScheduledEvent(
            interviewId=interview.interviewId,
            candidateId=req.candidateId,
            recruiterId=req.recruiterId,
            jobId=req.jobId,
            startTime=slot.start,
            endTime=slot.end,
            googleEventId=google_event_id,
            googleMeetLink=google_meet_link,
            candidateEmail=req.candidateEmail,
            recruiterEmail=req.recruiterEmail,
        )
    )

    return interview


async def cancel_interview(interview_id: str, reason: str = "") -> InterviewResponse:
    """
    1. Delete Google Calendar event
    2. Update MongoDB status to CANCELLED
    3. Publish interview.cancelled to Kafka
    """
    interview = await get_interview(interview_id)
    if not interview:
        raise ValueError(f"Interview {interview_id} not found.")

    if interview.googleEventId:
        try:
            delete_calendar_event(interview.googleEventId)
        except Exception as exc:
            logger.warning(
                "Failed to delete calendar event",
                extra={"eventId": interview.googleEventId, "error": str(exc)},
            )

    updated = await update_interview(
        interview_id, {"status": InterviewStatus.CANCELLED}
    )

    await publish_interview_cancelled(
        InterviewCancelledEvent(
            interviewId=interview_id,
            candidateId=interview.candidateId,
            recruiterId=interview.recruiterId,
            jobId=interview.jobId,
            reason=reason,
        )
    )

    return updated


async def reschedule_interview(
    interview_id: str,
    req: RescheduleInterviewRequest,
) -> InterviewResponse:
    """
    1. Find new available slot
    2. Update Google Calendar event
    3. Update MongoDB document
    4. Publish interview.rescheduled to Kafka
    """
    interview = await get_interview(interview_id)
    if not interview:
        raise ValueError(f"Interview {interview_id} not found.")

    old_start = interview.startTime
    duration = int((interview.endTime - interview.startTime).total_seconds() / 60)

    slot = _find_available_slot(req.preferredWindows, duration)
    if not slot:
        raise ValueError("No available slots found in the provided time windows.")

    # Update Google Calendar event
    if interview.googleEventId:
        try:
            update_calendar_event(interview.googleEventId, slot.start, slot.end)
        except Exception as exc:
            logger.warning(
                "Failed to update calendar event",
                extra={"eventId": interview.googleEventId, "error": str(exc)},
            )

    updated = await update_interview(
        interview_id,
        {
            "status": InterviewStatus.RESCHEDULED,
            "startTime": slot.start,
            "endTime": slot.end,
        },
    )

    await publish_interview_rescheduled(
        InterviewRescheduledEvent(
            interviewId=interview_id,
            candidateId=interview.candidateId,
            recruiterId=interview.recruiterId,
            jobId=interview.jobId,
            oldStartTime=old_start,
            newStartTime=slot.start,
            newEndTime=slot.end,
            googleEventId=interview.googleEventId,
        )
    )

    return updated
