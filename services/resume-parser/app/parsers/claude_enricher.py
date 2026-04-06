import json
import logging

import anthropic

from app.core.config import settings
from app.schemas.events import Education, ParsedResume

logger = logging.getLogger(__name__)

_client: anthropic.AsyncAnthropic | None = None


def get_client() -> anthropic.AsyncAnthropic:
    global _client
    if _client is None:
        _client = anthropic.AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
    return _client


SYSTEM_PROMPT = """You are a resume parser. Extract structured information from the resume text provided.
Return ONLY valid JSON — no markdown, no explanation, no preamble.

JSON schema:
{
  "name": "string or null",
  "email": "string or null",
  "phone": "string or null",
  "skills": ["list of technical skills as lowercase strings"],
  "experience_years": number (float, e.g. 3.5),
  "education": [
    {"degree": "string", "institution": "string or null", "year": "string or null"}
  ],
  "summary": "2-3 sentence professional summary of this candidate"
}

Rules:
- skills: include ALL technical skills, frameworks, languages, tools, and platforms mentioned
- experience_years: best estimate of total professional experience in years; 0 if unclear
- summary: write as if for a recruiter — highlight seniority level, primary domain, and key strengths
- Return null for any field you cannot determine with confidence
"""


async def enrich_with_claude(raw_text: str, spacy_result: ParsedResume) -> ParsedResume:
    """
    Second pass: send the raw resume text to Claude for semantic enrichment.
    Claude's output overrides or extends the spaCy extraction.
    """
    client = get_client()

    # Truncate to ~6000 tokens worth of text to stay within limits
    truncated_text = raw_text[:24_000]

    try:
        message = await client.messages.create(
            model="claude-sonnet-4-20250514",
            max_tokens=1024,
            system=SYSTEM_PROMPT,
            messages=[
                {
                    "role": "user",
                    "content": f"Parse this resume:\n\n{truncated_text}",
                }
            ],
        )

        raw_json = message.content[0].text.strip()
        data = json.loads(raw_json)

        # Merge: Claude wins on most fields; fall back to spaCy where Claude returns null
        education = [
            Education(
                degree=e.get("degree"),
                institution=e.get("institution"),
                year=e.get("year"),
            )
            for e in (data.get("education") or [])
        ]

        return ParsedResume(
            name=data.get("name") or spacy_result.name,
            email=data.get("email") or spacy_result.email,
            phone=data.get("phone") or spacy_result.phone,
            skills=data.get("skills") or spacy_result.skills,
            experience_years=data.get("experience_years") or spacy_result.experience_years,
            education=education or spacy_result.education,
            summary=data.get("summary"),
            raw_text_length=spacy_result.raw_text_length,
        )

    except (json.JSONDecodeError, KeyError, IndexError) as exc:
        logger.warning(
            "Claude parse failed — falling back to spaCy result",
            extra={"error": str(exc)},
        )
        return spacy_result
    except anthropic.APIError as exc:
        logger.error(
            "Anthropic API error — falling back to spaCy result",
            extra={"error": str(exc)},
        )
        return spacy_result
