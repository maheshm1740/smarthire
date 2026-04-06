from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field


# ── Inbound: application.submitted ──────────────────────────────────────────

class ApplicationSubmittedEvent(BaseModel):
    applicationId: str
    candidateId: str
    jobId: str
    resumeUrl: str          # R2 object key, e.g. "resumes/abc123.pdf"
    timestamp: datetime


# ── Outbound: resume.parsed ──────────────────────────────────────────────────

class Education(BaseModel):
    degree: str | None = None
    institution: str | None = None
    year: str | None = None


class ParsedResume(BaseModel):
    name: str | None = None
    email: str | None = None
    phone: str | None = None
    skills: list[str] = Field(default_factory=list)
    experience_years: float = 0.0
    education: list[Education] = Field(default_factory=list)
    summary: str | None = None
    raw_text_length: int = 0


class ResumeParsedEvent(BaseModel):
    applicationId: str
    candidateId: str
    jobId: str
    parsedResume: ParsedResume
    parsingDurationMs: int
    timestamp: datetime = Field(default_factory=datetime.utcnow)
