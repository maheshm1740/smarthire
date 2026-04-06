from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


# ── Enums ────────────────────────────────────────────────────────────────────

class InterviewStatus(str, Enum):
    SCHEDULED = "SCHEDULED"
    CANCELLED = "CANCELLED"
    RESCHEDULED = "RESCHEDULED"


# ── REST request / response ───────────────────────────────────────────────────

class TimeWindow(BaseModel):
    start: datetime
    end: datetime


class ScheduleInterviewRequest(BaseModel):
    candidateId: str
    jobId: str
    recruiterId: str
    candidateEmail: str
    recruiterEmail: str
    preferredWindows: list[TimeWindow]   # recruiter provides 2-3 options
    durationMinutes: int = 60
    title: str = "SmartHire Interview"
    description: str = ""


class RescheduleInterviewRequest(BaseModel):
    preferredWindows: list[TimeWindow]
    reason: str = ""


class InterviewResponse(BaseModel):
    interviewId: str
    candidateId: str
    jobId: str
    recruiterId: str
    status: InterviewStatus
    startTime: datetime
    endTime: datetime
    googleEventId: str | None = None
    googleMeetLink: str | None = None
    createdAt: datetime
    updatedAt: datetime


# ── Kafka events ──────────────────────────────────────────────────────────────

class InterviewScheduledEvent(BaseModel):
    interviewId: str
    candidateId: str
    recruiterId: str
    jobId: str
    startTime: datetime
    endTime: datetime
    googleEventId: str | None = None
    googleMeetLink: str | None = None
    candidateEmail: str
    recruiterEmail: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class InterviewCancelledEvent(BaseModel):
    interviewId: str
    candidateId: str
    recruiterId: str
    jobId: str
    reason: str = ""
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class InterviewRescheduledEvent(BaseModel):
    interviewId: str
    candidateId: str
    recruiterId: str
    jobId: str
    oldStartTime: datetime
    newStartTime: datetime
    newEndTime: datetime
    googleEventId: str | None = None
    timestamp: datetime = Field(default_factory=datetime.utcnow)
