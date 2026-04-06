from datetime import datetime, timedelta
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.schemas.models import (
    InterviewCancelledEvent,
    InterviewRescheduledEvent,
    InterviewScheduledEvent,
    InterviewStatus,
    RescheduleInterviewRequest,
    ScheduleInterviewRequest,
    TimeWindow,
)
from app.services.scheduler_service import _find_available_slot


# ── Helpers ───────────────────────────────────────────────────────────────────

def make_window(hours_from_now: int, duration: int = 60) -> TimeWindow:
    start = datetime.utcnow() + timedelta(hours=hours_from_now)
    end = start + timedelta(minutes=duration)
    return TimeWindow(start=start, end=end)


# ── Schema validation ─────────────────────────────────────────────────────────

class TestSchemas:
    def test_schedule_request_defaults(self):
        req = ScheduleInterviewRequest(
            candidateId="c1",
            jobId="j1",
            recruiterId="r1",
            candidateEmail="candidate@example.com",
            recruiterEmail="recruiter@example.com",
            preferredWindows=[make_window(24)],
        )
        assert req.durationMinutes == 60
        assert req.title == "SmartHire Interview"

    def test_time_window_valid(self):
        start = datetime.utcnow() + timedelta(hours=2)
        end = start + timedelta(hours=1)
        window = TimeWindow(start=start, end=end)
        assert window.end > window.start

    def test_interview_status_enum(self):
        assert InterviewStatus.SCHEDULED == "SCHEDULED"
        assert InterviewStatus.CANCELLED == "CANCELLED"
        assert InterviewStatus.RESCHEDULED == "RESCHEDULED"

    def test_scheduled_event_schema(self):
        now = datetime.utcnow()
        event = InterviewScheduledEvent(
            interviewId="i1",
            candidateId="c1",
            recruiterId="r1",
            jobId="j1",
            startTime=now,
            endTime=now + timedelta(hours=1),
            candidateEmail="c@example.com",
            recruiterEmail="r@example.com",
        )
        assert event.interviewId == "i1"
        assert event.timestamp is not None

    def test_cancelled_event_schema(self):
        event = InterviewCancelledEvent(
            interviewId="i1",
            candidateId="c1",
            recruiterId="r1",
            jobId="j1",
            reason="Candidate withdrew",
        )
        assert event.reason == "Candidate withdrew"

    def test_rescheduled_event_schema(self):
        now = datetime.utcnow()
        event = InterviewRescheduledEvent(
            interviewId="i1",
            candidateId="c1",
            recruiterId="r1",
            jobId="j1",
            oldStartTime=now,
            newStartTime=now + timedelta(days=1),
            newEndTime=now + timedelta(days=1, hours=1),
        )
        assert event.newStartTime > event.oldStartTime


# ── Slot selection logic ──────────────────────────────────────────────────────

class TestFindAvailableSlot:
    def test_returns_first_available(self):
        windows = [make_window(24), make_window(48)]
        with patch(
            "app.services.scheduler_service.check_availability", return_value=True
        ):
            slot = _find_available_slot(windows, 60)
        assert slot is not None
        assert slot.start == windows[0].start

    def test_skips_busy_returns_second(self):
        windows = [make_window(24), make_window(48)]
        with patch(
            "app.services.scheduler_service.check_availability",
            side_effect=[False, True],
        ):
            slot = _find_available_slot(windows, 60)
        assert slot is not None
        assert slot.start == windows[1].start

    def test_returns_none_when_all_busy(self):
        windows = [make_window(24), make_window(48)]
        with patch(
            "app.services.scheduler_service.check_availability", return_value=False
        ):
            slot = _find_available_slot(windows, 60)
        assert slot is None

    def test_returns_none_when_no_windows(self):
        with patch(
            "app.services.scheduler_service.check_availability", return_value=True
        ):
            slot = _find_available_slot([], 60)
        assert slot is None

    def test_slot_duration_is_correct(self):
        windows = [make_window(24)]
        with patch(
            "app.services.scheduler_service.check_availability", return_value=True
        ):
            slot = _find_available_slot(windows, 90)
        assert slot is not None
        duration = (slot.end - slot.start).total_seconds() / 60
        assert duration == 90

    def test_handles_calendar_error_gracefully(self):
        windows = [make_window(24), make_window(48)]
        with patch(
            "app.services.scheduler_service.check_availability",
            side_effect=[Exception("Calendar API error"), True],
        ):
            slot = _find_available_slot(windows, 60)
        # Should skip the failed window and return the second
        assert slot is not None
        assert slot.start == windows[1].start


# ── Service layer (mocked dependencies) ──────────────────────────────────────

class TestSchedulerService:
    @pytest.mark.asyncio
    async def test_schedule_interview_success(self):
        req = ScheduleInterviewRequest(
            candidateId="c1",
            jobId="j1",
            recruiterId="r1",
            candidateEmail="candidate@example.com",
            recruiterEmail="recruiter@example.com",
            preferredWindows=[make_window(24)],
        )

        mock_interview = MagicMock()
        mock_interview.interviewId = "interview-123"
        mock_interview.candidateId = "c1"
        mock_interview.recruiterId = "r1"
        mock_interview.jobId = "j1"

        with (
            patch("app.services.scheduler_service.check_availability", return_value=True),
            patch("app.services.scheduler_service.create_calendar_event", return_value={"id": "gcal-id", "conferenceData": {"entryPoints": [{"uri": "https://meet.google.com/abc"}]}}),
            patch("app.services.scheduler_service.create_interview", new_callable=AsyncMock, return_value=mock_interview),
            patch("app.services.scheduler_service.publish_interview_scheduled", new_callable=AsyncMock),
        ):
            from app.services.scheduler_service import schedule_interview
            result = await schedule_interview(req)
            assert result.interviewId == "interview-123"

    @pytest.mark.asyncio
    async def test_schedule_raises_when_no_slots(self):
        req = ScheduleInterviewRequest(
            candidateId="c1",
            jobId="j1",
            recruiterId="r1",
            candidateEmail="candidate@example.com",
            recruiterEmail="recruiter@example.com",
            preferredWindows=[make_window(24)],
        )

        with (
            patch("app.services.scheduler_service.check_availability", return_value=False),
        ):
            from app.services.scheduler_service import schedule_interview
            with pytest.raises(ValueError, match="No available slots"):
                await schedule_interview(req)
