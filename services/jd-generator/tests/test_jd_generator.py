from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from app.schemas.models import (
    EnhanceJDRequest,
    ExperienceLevel,
    GenerateJDRequest,
    JobCreatedEvent,
)
from app.services.claude_service import _build_enhance_prompt, _build_generate_prompt


# ── Schema validation ─────────────────────────────────────────────────────────

class TestSchemas:
    def test_generate_request_defaults(self):
        req = GenerateJDRequest(title="Backend Engineer")
        assert req.experienceLevel == ExperienceLevel.MID
        assert req.experienceYears == 3
        assert req.skills == []
        assert req.companyName == ""

    def test_generate_request_with_skills(self):
        req = GenerateJDRequest(
            title="Senior Python Engineer",
            skills=["python", "fastapi", "kafka"],
            experienceLevel=ExperienceLevel.SENIOR,
            experienceYears=5,
            companyName="SmartHire",
        )
        assert req.title == "Senior Python Engineer"
        assert len(req.skills) == 3
        assert req.experienceLevel == ExperienceLevel.SENIOR

    def test_experience_level_enum_values(self):
        assert ExperienceLevel.JUNIOR == "junior"
        assert ExperienceLevel.MID == "mid"
        assert ExperienceLevel.SENIOR == "senior"
        assert ExperienceLevel.LEAD == "lead"
        assert ExperienceLevel.PRINCIPAL == "principal"

    def test_enhance_request(self):
        req = EnhanceJDRequest(
            jobId="job-123",
            existingDescription="We are looking for a developer...",
            title="Full Stack Engineer",
            skills=["react", "node.js"],
        )
        assert req.jobId == "job-123"
        assert req.experienceLevel == ExperienceLevel.MID


# ── Prompt building ───────────────────────────────────────────────────────────

class TestPromptBuilding:
    def test_generate_prompt_contains_title(self):
        req = GenerateJDRequest(title="Senior Python Engineer")
        prompt = _build_generate_prompt(req)
        assert "Senior Python Engineer" in prompt

    def test_generate_prompt_contains_skills(self):
        req = GenerateJDRequest(
            title="Backend Engineer",
            skills=["python", "fastapi", "postgresql"],
        )
        prompt = _build_generate_prompt(req)
        assert "python" in prompt
        assert "fastapi" in prompt
        assert "postgresql" in prompt

    def test_generate_prompt_contains_experience_level(self):
        req = GenerateJDRequest(title="Engineer", experienceLevel=ExperienceLevel.SENIOR)
        prompt = _build_generate_prompt(req)
        assert "senior" in prompt.lower()

    def test_generate_prompt_contains_company(self):
        req = GenerateJDRequest(
            title="Engineer",
            companyName="Acme Corp",
            companyDescription="A leading tech company.",
        )
        prompt = _build_generate_prompt(req)
        assert "Acme Corp" in prompt
        assert "A leading tech company." in prompt

    def test_generate_prompt_without_company(self):
        req = GenerateJDRequest(title="Engineer")
        prompt = _build_generate_prompt(req)
        assert "fast-growing technology company" in prompt

    def test_generate_prompt_contains_additional_context(self):
        req = GenerateJDRequest(
            title="Engineer",
            additionalContext="Remote-first team, async culture.",
        )
        prompt = _build_generate_prompt(req)
        assert "Remote-first team" in prompt

    def test_enhance_prompt_contains_existing_description(self):
        req = EnhanceJDRequest(
            jobId="j1",
            existingDescription="We need a great developer with 5 years experience.",
            title="Backend Engineer",
        )
        prompt = _build_enhance_prompt(req)
        assert "We need a great developer" in prompt

    def test_enhance_prompt_contains_title(self):
        req = EnhanceJDRequest(
            jobId="j1",
            existingDescription="Some description",
            title="Lead ML Engineer",
        )
        prompt = _build_enhance_prompt(req)
        assert "Lead ML Engineer" in prompt

    def test_enhance_prompt_contains_skills(self):
        req = EnhanceJDRequest(
            jobId="j1",
            existingDescription="Some description",
            title="Engineer",
            skills=["pytorch", "tensorflow"],
        )
        prompt = _build_enhance_prompt(req)
        assert "pytorch" in prompt
        assert "tensorflow" in prompt


# ── Claude service (mocked) ───────────────────────────────────────────────────

class TestClaudeService:
    @pytest.mark.asyncio
    async def test_generate_jd_full_returns_text(self):
        mock_response = MagicMock()
        mock_response.content = [MagicMock(text="# Senior Python Engineer\n\nGreat role...")]

        with patch(
            "app.services.claude_service.get_client"
        ) as mock_get_client:
            mock_client = AsyncMock()
            mock_client.messages.create = AsyncMock(return_value=mock_response)
            mock_get_client.return_value = mock_client

            from app.services.claude_service import generate_jd_full
            req = GenerateJDRequest(title="Senior Python Engineer")
            result = await generate_jd_full(req)

        assert "Senior Python Engineer" in result

    @pytest.mark.asyncio
    async def test_enhance_jd_full_returns_text(self):
        mock_response = MagicMock()
        mock_response.content = [MagicMock(text="# Enhanced JD\n\nImproved description...")]

        with patch(
            "app.services.claude_service.get_client"
        ) as mock_get_client:
            mock_client = AsyncMock()
            mock_client.messages.create = AsyncMock(return_value=mock_response)
            mock_get_client.return_value = mock_client

            from app.services.claude_service import enhance_jd_full
            req = EnhanceJDRequest(
                jobId="job-1",
                existingDescription="Original description",
                title="Backend Engineer",
            )
            result = await enhance_jd_full(req)

        assert "Enhanced JD" in result

    @pytest.mark.asyncio
    async def test_stream_jd_yields_chunks(self):
        async def mock_text_stream():
            for chunk in ["# Senior ", "Python ", "Engineer\n\n", "Great role..."]:
                yield chunk

        mock_stream = MagicMock()
        mock_stream.__aenter__ = AsyncMock(return_value=mock_stream)
        mock_stream.__aexit__ = AsyncMock(return_value=None)
        mock_stream.text_stream = mock_text_stream()

        with patch("app.services.claude_service.get_client") as mock_get_client:
            mock_client = MagicMock()
            mock_client.messages.stream = MagicMock(return_value=mock_stream)
            mock_get_client.return_value = mock_client

            from app.services.claude_service import stream_jd
            req = GenerateJDRequest(title="Senior Python Engineer")
            chunks = [chunk async for chunk in stream_jd(req)]

        assert len(chunks) == 4
        assert "".join(chunks).startswith("# Senior ")


# ── Kafka consumer (mocked) ───────────────────────────────────────────────────

class TestJobCreatedConsumer:
    @pytest.mark.asyncio
    async def test_handle_message_valid_event(self):
        from datetime import datetime
        from app.consumers.job_consumer import JobCreatedConsumer

        consumer = JobCreatedConsumer()
        consumer._consumer = AsyncMock()

        message = MagicMock()
        message.value = {
            "jobId": "job-1",
            "title": "Backend Engineer",
            "description": "We need a Python developer.",
            "requirements": ["python", "fastapi"],
            "companyId": "company-1",
            "timestamp": datetime.utcnow().isoformat(),
        }

        with patch(
            "app.consumers.job_consumer.enhance_jd_full",
            new_callable=AsyncMock,
            return_value="# Enhanced JD\n\nGreat role.",
        ):
            await consumer._handle_message(message)
            consumer._consumer.commit.assert_called_once()

    @pytest.mark.asyncio
    async def test_handle_message_invalid_schema_commits_and_skips(self):
        from app.consumers.job_consumer import JobCreatedConsumer

        consumer = JobCreatedConsumer()
        consumer._consumer = AsyncMock()

        message = MagicMock()
        message.value = {"invalid": "data"}

        await consumer._handle_message(message)
        consumer._consumer.commit.assert_called_once()
