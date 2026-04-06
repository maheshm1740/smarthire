import math

import pytest

from app.rankers.scorer import (
    _education_score,
    _experience_score,
    _skills_score,
    compute_score,
)
from app.schemas.events import Education, ParsedResume


# ── Skills score ─────────────────────────────────────────────────────────────

class TestSkillsScore:
    def test_perfect_match(self):
        score = _skills_score(["python", "fastapi", "docker"], ["python", "fastapi", "docker"])
        assert score == 1.0

    def test_partial_match(self):
        score = _skills_score(["python", "fastapi"], ["python", "fastapi", "docker", "kafka"])
        assert score == pytest.approx(0.5)

    def test_no_match(self):
        score = _skills_score(["cobol", "fortran"], ["python", "fastapi"])
        assert score == 0.0

    def test_extra_skills_do_not_penalise(self):
        # Candidate has more than required — should not exceed 1.0
        score = _skills_score(["python", "java", "go", "rust", "kafka"], ["python"])
        assert score == 1.0

    def test_case_insensitive(self):
        score = _skills_score(["Python", "FastAPI"], ["python", "fastapi"])
        assert score == 1.0

    def test_no_job_requirements_returns_neutral(self):
        score = _skills_score(["python"], [])
        assert score == 0.5


# ── Experience score ─────────────────────────────────────────────────────────

class TestExperienceScore:
    def test_meets_requirement_approx(self):
        # Exactly meeting the requirement scores ~0.68 (sigmoid centred at 0.75 ratio)
        score = _experience_score(5.0, 5.0)
        assert 0.60 <= score <= 0.80

    def test_overqualified_caps_at_one(self):
        score = _experience_score(20.0, 3.0)
        assert score <= 1.0
        assert score > 0.95

    def test_underqualified(self):
        score = _experience_score(1.0, 5.0)
        assert score < 0.5

    def test_zero_experience_zero_required(self):
        score = _experience_score(0.0, 0.0)
        assert score == 0.0  # log1p(0) = 0

    def test_no_requirement_rewards_experience(self):
        low = _experience_score(1.0, 0.0)
        high = _experience_score(8.0, 0.0)
        assert high > low

    def test_score_bounded(self):
        for years in [0, 1, 3, 5, 10, 20]:
            score = _experience_score(float(years), 3.0)
            assert 0.0 <= score <= 1.0


# ── Education score ───────────────────────────────────────────────────────────

class TestEducationScore:
    def test_phd_scores_highest(self):
        edu = [Education(degree="PhD in Computer Science", institution="MIT")]
        assert _education_score(edu) == 1.0

    def test_masters_scores_high(self):
        edu = [Education(degree="Master of Science in Software Engineering")]
        assert _education_score(edu) == pytest.approx(0.85)

    def test_bachelors(self):
        edu = [Education(degree="Bachelor of Technology")]
        assert _education_score(edu) == pytest.approx(0.70)

    def test_no_education_returns_default(self):
        assert _education_score([]) == pytest.approx(0.3)

    def test_unrecognised_degree_returns_default(self):
        edu = [Education(degree="School of Hard Knocks")]
        assert _education_score(edu) == pytest.approx(0.3)

    def test_takes_highest_when_multiple(self):
        edu = [
            Education(degree="Bachelor of Science"),
            Education(degree="Master of Business Administration"),
        ]
        assert _education_score(edu) == pytest.approx(0.85)


# ── Full compute_score ────────────────────────────────────────────────────────

class TestComputeScore:
    def _make_resume(self, **kwargs) -> ParsedResume:
        defaults = dict(
            name="Jane Smith",
            skills=["python", "fastapi", "kafka", "docker"],
            experience_years=5.0,
            education=[Education(degree="Bachelor of Technology")],
            summary="Experienced backend engineer specialising in distributed systems.",
        )
        defaults.update(kwargs)
        return ParsedResume(**defaults)

    def test_all_scores_bounded(self):
        resume = self._make_resume()
        breakdown = compute_score(
            candidate_resume=resume,
            job_requirements=["python", "fastapi"],
            job_description="We need a Python developer for our platform team.",
            required_experience_years=3.0,
        )
        for field in ["skills_score", "experience_score", "education_score", "semantic_score", "final_score"]:
            val = getattr(breakdown, field)
            assert 0.0 <= val <= 1.0, f"{field} out of range: {val}"

    def test_final_score_is_weighted_sum(self):
        resume = self._make_resume(
            skills=["python"],
            experience_years=5.0,
            education=[Education(degree="Master of Science")],
            summary="Python expert",
        )
        breakdown = compute_score(
            candidate_resume=resume,
            job_requirements=["python"],
            job_description="Python developer role",
            required_experience_years=5.0,
        )
        expected = (
            breakdown.skills_score * 0.40
            + breakdown.experience_score * 0.30
            + breakdown.education_score * 0.20
            + breakdown.semantic_score * 0.10
        )
        assert breakdown.final_score == pytest.approx(expected, abs=0.001)

    def test_perfect_match_scores_high(self):
        resume = self._make_resume(
            skills=["python", "fastapi", "kafka"],
            experience_years=7.0,
            education=[Education(degree="Master of Science in Computer Science")],
            summary="Senior Python engineer with FastAPI and Kafka expertise.",
        )
        breakdown = compute_score(
            candidate_resume=resume,
            job_requirements=["python", "fastapi", "kafka"],
            job_description="Senior Python engineer needed for FastAPI microservices with Kafka.",
            required_experience_years=5.0,
        )
        assert breakdown.final_score >= 0.75

    def test_poor_match_scores_low(self):
        resume = self._make_resume(
            skills=["cobol", "fortran"],
            experience_years=0.0,
            education=[],
            summary="Entry level developer.",
        )
        breakdown = compute_score(
            candidate_resume=resume,
            job_requirements=["python", "fastapi", "kafka", "docker", "kubernetes"],
            job_description="Senior Python cloud engineer with 8 years experience.",
            required_experience_years=8.0,
        )
        assert breakdown.final_score < 0.45
