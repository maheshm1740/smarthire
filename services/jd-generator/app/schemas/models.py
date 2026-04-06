from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


class ExperienceLevel(str, Enum):
    JUNIOR = "junior"
    MID = "mid"
    SENIOR = "senior"
    LEAD = "lead"
    PRINCIPAL = "principal"


# ── REST request / response ───────────────────────────────────────────────────

class GenerateJDRequest(BaseModel):
    title: str
    skills: list[str] = Field(default_factory=list)
    experienceLevel: ExperienceLevel = ExperienceLevel.MID
    experienceYears: int = 3
    companyName: str = ""
    companyDescription: str = ""
    additionalContext: str = ""


class EnhanceJDRequest(BaseModel):
    jobId: str
    existingDescription: str
    title: str
    skills: list[str] = Field(default_factory=list)
    experienceLevel: ExperienceLevel = ExperienceLevel.MID


# ── Kafka inbound: job.created ────────────────────────────────────────────────

class JobCreatedEvent(BaseModel):
    jobId: str
    title: str
    description: str
    requirements: list[str] = Field(default_factory=list)
    companyId: str = ""
    timestamp: datetime
