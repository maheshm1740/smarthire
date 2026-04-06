import logging
import math

from app.core.config import settings
from app.rankers.embeddings import cosine_similarity, embed
from app.schemas.events import ParsedResume, ScoreBreakdown

logger = logging.getLogger(__name__)

# Education level → numeric weight for scoring
EDUCATION_WEIGHTS: dict[str, float] = {
    "phd": 1.0,
    "doctorate": 1.0,
    "master": 0.85,
    "m.sc": 0.85,
    "m.tech": 0.85,
    "mba": 0.80,
    "bachelor": 0.70,
    "b.sc": 0.70,
    "b.tech": 0.70,
    "b.e": 0.70,
    "associate": 0.50,
    "diploma": 0.40,
}


def _skills_score(candidate_skills: list[str], job_requirements: list[str]) -> float:
    """
    Jaccard-like overlap: matched_skills / total_job_requirements.
    Capped at 1.0 — having extra skills does not penalise.
    If the job has no listed requirements, return 0.5 (neutral).
    """
    if not job_requirements:
        return 0.5

    candidate_lower = {s.lower() for s in candidate_skills}
    required_lower = {r.lower() for r in job_requirements}

    matched = candidate_lower & required_lower
    score = len(matched) / len(required_lower)

    logger.debug(
        "Skills score",
        extra={
            "matched": sorted(matched),
            "required": sorted(required_lower),
            "score": round(score, 3),
        },
    )
    return min(1.0, score)


def _experience_score(candidate_years: float, required_years: float) -> float:
    """
    Sigmoid-shaped score centred on the required years.
    - Exactly meeting the requirement → ~0.85
    - 2× the requirement → ~0.97
    - Half the requirement → ~0.50
    - Zero years, 5 required → ~0.18
    If no requirement is specified, use a gentle log curve.
    """
    if required_years <= 0:
        # No stated requirement — reward experience gently up to ~10 years
        return min(1.0, math.log1p(candidate_years) / math.log1p(10))

    ratio = candidate_years / required_years
    # Sigmoid: 1 / (1 + e^(-3*(ratio - 0.75)))
    score = 1.0 / (1.0 + math.exp(-3.0 * (ratio - 0.75)))
    return round(min(1.0, score), 4)


def _education_score(candidate_education: list) -> float:
    """
    Return the highest education weight found in the candidate's education list.
    Defaults to 0.3 if nothing recognisable is found.
    """
    best = 0.3
    for edu in candidate_education:
        degree_text = (edu.degree or "").lower()
        for keyword, weight in EDUCATION_WEIGHTS.items():
            if keyword in degree_text:
                best = max(best, weight)
    return best


def _semantic_score(candidate_resume: ParsedResume, job_description: str) -> float:
    """
    Cosine similarity between:
      - Candidate text: name + skills + summary
      - Job text: full job description
    """
    candidate_text = " ".join(
        filter(
            None,
            [
                candidate_resume.name or "",
                " ".join(candidate_resume.skills),
                candidate_resume.summary or "",
            ],
        )
    )

    if not candidate_text.strip() or not job_description.strip():
        return 0.5

    candidate_vec = embed(candidate_text)
    job_vec = embed(job_description)
    return cosine_similarity(candidate_vec, job_vec)


def compute_score(
    candidate_resume: ParsedResume,
    job_requirements: list[str],
    job_description: str,
    required_experience_years: float = 0.0,
) -> ScoreBreakdown:
    """
    Compute the four sub-scores and the weighted final score.

    Weights (from config):
      skills       40%
      experience   30%
      education    20%
      semantic     10%
    """
    skills = _skills_score(candidate_resume.skills, job_requirements)
    experience = _experience_score(candidate_resume.experience_years, required_experience_years)
    education = _education_score(candidate_resume.education)
    semantic = _semantic_score(candidate_resume, job_description)

    final = (
        skills * settings.WEIGHT_SKILLS
        + experience * settings.WEIGHT_EXPERIENCE
        + education * settings.WEIGHT_EDUCATION
        + semantic * settings.WEIGHT_SEMANTIC
    )

    breakdown = ScoreBreakdown(
        skills_score=round(skills, 4),
        experience_score=round(experience, 4),
        education_score=round(education, 4),
        semantic_score=round(semantic, 4),
        final_score=round(final, 4),
    )

    logger.info(
        "Score computed",
        extra={
            "skills": breakdown.skills_score,
            "experience": breakdown.experience_score,
            "education": breakdown.education_score,
            "semantic": breakdown.semantic_score,
            "final": breakdown.final_score,
        },
    )
    return breakdown
