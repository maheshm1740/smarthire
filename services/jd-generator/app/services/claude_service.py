import logging
from typing import AsyncIterator

import anthropic

from app.core.config import settings
from app.schemas.models import EnhanceJDRequest, ExperienceLevel, GenerateJDRequest

logger = logging.getLogger(__name__)

_client: anthropic.AsyncAnthropic | None = None


def get_client() -> anthropic.AsyncAnthropic:
    global _client
    if _client is None:
        _client = anthropic.AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
    return _client


SYSTEM_PROMPT = """You are an expert technical recruiter and copywriter specialising in writing 
compelling, inclusive, and accurate job descriptions for software engineering roles.

Your job descriptions:
- Open with an engaging company/team context (2-3 sentences)
- Clearly describe the role and its impact
- List responsibilities as concrete, specific actions (not vague duties)
- Separate "Required" from "Nice to have" skills
- Include a benefits/culture section that feels authentic, not generic
- Use inclusive language — avoid gendered terms and unnecessary jargon
- Are formatted in clean Markdown with clear section headers

Never include salary ranges unless explicitly provided. 
Never use buzzwords like "rockstar", "ninja", "guru", or "passionate".
Write in second person ("You will", "You have") to make it personal.
"""


def _build_generate_prompt(req: GenerateJDRequest) -> str:
    skills_text = ", ".join(req.skills) if req.skills else "relevant technical skills"
    company_text = (
        f"Company: {req.companyName}\n{req.companyDescription}"
        if req.companyName
        else "a fast-growing technology company"
    )
    context = f"\n\nAdditional context: {req.additionalContext}" if req.additionalContext else ""

    return f"""Write a complete job description for the following role:

Title: {req.title}
Experience Level: {req.experienceLevel.value}
Years of Experience: {req.experienceYears}+
Key Skills: {skills_text}
{company_text}{context}

Write a complete, well-structured job description in Markdown."""


def _build_enhance_prompt(req: EnhanceJDRequest) -> str:
    skills_text = ", ".join(req.skills) if req.skills else "as listed"
    return f"""Enhance and improve this existing job description for a {req.title} role.

Current description:
{req.existingDescription}

Key skills to emphasise: {skills_text}
Experience level: {req.experienceLevel.value}

Rewrite it to be more compelling, specific, and inclusive while keeping the core requirements.
Format in clean Markdown with clear sections."""


async def stream_jd(req: GenerateJDRequest) -> AsyncIterator[str]:
    """
    Stream a generated job description token by token using Claude API.
    Yields raw text chunks as they arrive — the router wraps these in SSE format.
    """
    client = get_client()
    prompt = _build_generate_prompt(req)

    logger.info(
        "Streaming JD generation",
        extra={"title": req.title, "level": req.experienceLevel},
    )

    async with client.messages.stream(
        model=settings.CLAUDE_MODEL,
        max_tokens=settings.CLAUDE_MAX_TOKENS,
        system=SYSTEM_PROMPT,
        messages=[{"role": "user", "content": prompt}],
    ) as stream:
        async for text in stream.text_stream:
            yield text

    logger.info("JD generation complete", extra={"title": req.title})


async def generate_jd_full(req: GenerateJDRequest) -> str:
    """
    Non-streaming version — collects the full JD and returns it as a string.
    Used by the Kafka consumer for batch enhancement.
    """
    client = get_client()
    prompt = _build_generate_prompt(req)

    message = await client.messages.create(
        model=settings.CLAUDE_MODEL,
        max_tokens=settings.CLAUDE_MAX_TOKENS,
        system=SYSTEM_PROMPT,
        messages=[{"role": "user", "content": prompt}],
    )
    return message.content[0].text


async def enhance_jd_full(req: EnhanceJDRequest) -> str:
    """
    Enhance an existing job description — non-streaming, used by Kafka consumer.
    """
    client = get_client()
    prompt = _build_enhance_prompt(req)

    logger.info(
        "Enhancing existing JD",
        extra={"jobId": req.jobId, "title": req.title},
    )

    message = await client.messages.create(
        model=settings.CLAUDE_MODEL,
        max_tokens=settings.CLAUDE_MAX_TOKENS,
        system=SYSTEM_PROMPT,
        messages=[{"role": "user", "content": prompt}],
    )

    enhanced = message.content[0].text
    logger.info("JD enhancement complete", extra={"jobId": req.jobId})
    return enhanced
