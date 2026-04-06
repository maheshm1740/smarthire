import pytest

from app.parsers.spacy_parser import (
    _extract_email,
    _extract_experience_years,
    _extract_phone,
    _extract_skills,
)
from app.schemas.events import ParsedResume


# ── Email extraction ─────────────────────────────────────────────────────────

def test_extract_email_found():
    text = "Contact me at john.doe@example.com for more info."
    assert _extract_email(text) == "john.doe@example.com"


def test_extract_email_not_found():
    assert _extract_email("No email here") is None


# ── Phone extraction ─────────────────────────────────────────────────────────

def test_extract_phone_us_format():
    text = "Phone: +1 (415) 555-0172"
    result = _extract_phone(text)
    assert result is not None
    assert "415" in result


# ── Experience extraction ────────────────────────────────────────────────────

@pytest.mark.parametrize("text,expected", [
    ("5 years of professional experience in software development", 5.0),
    ("8+ years experience building distributed systems", 8.0),
    ("Experience: 3 years", 3.0),
    ("No experience info here", 0.0),
])
def test_extract_experience_years(text, expected):
    assert _extract_experience_years(text) == expected


# ── Skills extraction ────────────────────────────────────────────────────────

def test_extract_skills_basic():
    text = "Proficient in Python, FastAPI, PostgreSQL, and Docker. Some Kubernetes experience."
    skills = _extract_skills(text)
    assert "python" in skills
    assert "fastapi" in skills
    assert "postgresql" in skills
    assert "docker" in skills
    assert "kubernetes" in skills


def test_extract_skills_deduplication():
    text = "Python python PYTHON"
    skills = _extract_skills(text)
    assert skills.count("python") == 1


def test_extract_skills_empty():
    assert _extract_skills("I like hiking and cooking") == []


# ── ParsedResume schema ──────────────────────────────────────────────────────

def test_parsed_resume_defaults():
    resume = ParsedResume()
    assert resume.skills == []
    assert resume.education == []
    assert resume.experience_years == 0.0
    assert resume.name is None


def test_parsed_resume_with_data():
    resume = ParsedResume(
        name="Jane Smith",
        email="jane@example.com",
        skills=["python", "fastapi"],
        experience_years=4.5,
    )
    assert resume.name == "Jane Smith"
    assert "python" in resume.skills
    assert resume.experience_years == 4.5
