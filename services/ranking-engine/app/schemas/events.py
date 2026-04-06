from datetime import datetime

from pydantic import BaseModel, Field


# ── Inbound: resume.parsed (mirrors Resume Parser output) ────────────────────

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
    timestamp: datetime


# ── Outbound: candidate.ranked ───────────────────────────────────────────────

class ScoreBreakdown(BaseModel):
    skills_score: float        # 0.0 – 1.0
    experience_score: float    # 0.0 – 1.0
    education_score: float     # 0.0 – 1.0
    semantic_score: float      # 0.0 – 1.0
    final_score: float         # weighted composite, 0.0 – 1.0


class CandidateRankedEvent(BaseModel):
    applicationId: str
    candidateId: str
    jobId: str
    score: float                         # final_score, top-level for easy querying
    scoreBreakdown: ScoreBreakdown
    rankingDurationMs: int
    timestamp: datetime = Field(default_factory=datetime.utcnow)
