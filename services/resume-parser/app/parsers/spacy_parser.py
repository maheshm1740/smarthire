import logging
import re

import spacy

from app.core.config import settings
from app.schemas.events import Education, ParsedResume

logger = logging.getLogger(__name__)

# Load model once at module level (expensive — do NOT reload per request)
_nlp = None


def get_nlp():
    global _nlp
    if _nlp is None:
        logger.info("Loading spaCy model", extra={"model": settings.SPACY_MODEL})
        _nlp = spacy.load(settings.SPACY_MODEL)
    return _nlp


# Common technical skills to scan for (extend as needed)
SKILL_KEYWORDS: set[str] = {
    "python", "java", "javascript", "typescript", "kotlin", "go", "rust", "c++", "c#",
    "fastapi", "spring boot", "django", "flask", "react", "angular", "vue", "node.js",
    "kafka", "rabbitmq", "redis", "postgresql", "mongodb", "mysql", "elasticsearch",
    "docker", "kubernetes", "terraform", "aws", "gcp", "azure",
    "machine learning", "deep learning", "nlp", "pytorch", "tensorflow", "scikit-learn",
    "spark", "airflow", "dbt", "sql", "graphql", "rest", "grpc",
    "git", "ci/cd", "jenkins", "github actions",
}

EXPERIENCE_PATTERNS = [
    r"(\d+)\+?\s*years?\s*of\s*(professional\s*)?experience",
    r"(\d+)\+?\s*years?\s*experience",
    r"experience[:\s]+(\d+)\+?\s*years?",
]

DEGREE_KEYWORDS = [
    "bachelor", "master", "phd", "doctorate", "b.sc", "m.sc", "b.e", "m.e",
    "b.tech", "m.tech", "mba", "associate", "diploma",
]


def _extract_skills(text: str) -> list[str]:
    lower = text.lower()
    found = []
    for skill in SKILL_KEYWORDS:
        if skill in lower:
            found.append(skill)
    return sorted(set(found))


def _extract_experience_years(text: str) -> float:
    lower = text.lower()
    for pattern in EXPERIENCE_PATTERNS:
        match = re.search(pattern, lower)
        if match:
            return float(match.group(1))
    return 0.0


def _extract_education(doc) -> list[Education]:
    """
    Heuristic: look for lines containing degree keywords near ORG entities.
    spaCy's en_core_web_sm will tag universities as ORG.
    """
    education: list[Education] = []
    lines = doc.text.split("\n")

    for i, line in enumerate(lines):
        lower_line = line.lower()
        if any(deg in lower_line for deg in DEGREE_KEYWORDS):
            # Try to find an ORG entity near this line
            institution = None
            for ent in doc.ents:
                if ent.label_ == "ORG" and abs(ent.start_char - doc.text.find(line)) < 300:
                    institution = ent.text
                    break

            # Extract year from the same line or nearby lines
            year_match = re.search(r"\b(19|20)\d{2}\b", " ".join(lines[max(0, i-1):i+3]))
            year = year_match.group(0) if year_match else None

            education.append(
                Education(
                    degree=line.strip()[:120] if line.strip() else None,
                    institution=institution,
                    year=year,
                )
            )

    return education[:5]  # cap at 5


def _extract_email(text: str) -> str | None:
    match = re.search(r"[\w.+-]+@[\w-]+\.[a-zA-Z]{2,}", text)
    return match.group(0) if match else None


def _extract_phone(text: str) -> str | None:
    match = re.search(r"[\+]?[(]?[0-9]{1,4}[)]?[-\s\.]?[(]?[0-9]{1,3}[)]?[-\s\.]?[0-9]{3,4}[-\s\.]?[0-9]{3,4}", text)
    return match.group(0) if match else None


def parse_with_spacy(raw_text: str) -> ParsedResume:
    """
    First-pass extraction: structured data from the resume text using spaCy NER
    and regex heuristics. The result is enriched by the Claude pass.
    """
    nlp = get_nlp()
    doc = nlp(raw_text[:100_000])  # spaCy limit guard

    # Extract name: first PERSON entity in the doc is usually the candidate
    name = None
    for ent in doc.ents:
        if ent.label_ == "PERSON":
            name = ent.text
            break

    return ParsedResume(
        name=name,
        email=_extract_email(raw_text),
        phone=_extract_phone(raw_text),
        skills=_extract_skills(raw_text),
        experience_years=_extract_experience_years(raw_text),
        education=_extract_education(doc),
        raw_text_length=len(raw_text),
    )
