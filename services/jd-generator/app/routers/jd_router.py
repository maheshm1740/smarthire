import json
import logging

from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from app.schemas.models import EnhanceJDRequest, GenerateJDRequest
from app.services.claude_service import enhance_jd_full, stream_jd

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/jd", tags=["jd-generator"])


async def _sse_generator(req: GenerateJDRequest):
    """
    Wrap Claude's text stream in Server-Sent Events format.

    SSE format:
        data: <chunk>\n\n

    The client reads these with EventSource API or any SSE-capable HTTP client.
    A final [DONE] event signals stream completion.
    """
    try:
        async for chunk in stream_jd(req):
            # Escape newlines inside the chunk so each SSE message stays on one line
            payload = json.dumps({"text": chunk})
            yield f"data: {payload}\n\n"

        yield "data: [DONE]\n\n"

    except Exception as exc:
        logger.exception("SSE stream error", extra={"error": str(exc)})
        error_payload = json.dumps({"error": str(exc)})
        yield f"data: {error_payload}\n\n"


@router.post("/generate")
async def generate(req: GenerateJDRequest):
    """
    Generate a job description and stream it back using Server-Sent Events.

    Connect with EventSource:
        const es = new EventSource('/jd/generate');

    Or with curl:
        curl -X POST http://localhost:8093/jd/generate \\
          -H 'Content-Type: application/json' \\
          -d '{"title": "Senior Python Engineer", "skills": ["python", "fastapi"]}'

    Each SSE message contains a JSON object: {"text": "<chunk>"}
    The final message is: [DONE]
    """
    return StreamingResponse(
        _sse_generator(req),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",   # disable Nginx buffering for SSE
        },
    )


@router.post("/enhance/{job_id}")
async def enhance(job_id: str, req: EnhanceJDRequest):
    """
    Enhance an existing job description (non-streaming).
    Called manually or triggered by the Kafka job.created consumer.
    """
    req.jobId = job_id
    enhanced = await enhance_jd_full(req)
    return {"jobId": job_id, "enhancedDescription": enhanced}
