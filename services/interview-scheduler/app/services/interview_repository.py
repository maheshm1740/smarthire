import logging
from datetime import datetime

from bson import ObjectId

from app.core.config import settings
from app.core.mongo_client import get_db
from app.schemas.models import InterviewResponse, InterviewStatus

logger = logging.getLogger(__name__)


def _to_response(doc: dict) -> InterviewResponse:
    return InterviewResponse(
        interviewId=str(doc["_id"]),
        candidateId=doc["candidateId"],
        jobId=doc["jobId"],
        recruiterId=doc["recruiterId"],
        status=doc["status"],
        startTime=doc["startTime"],
        endTime=doc["endTime"],
        googleEventId=doc.get("googleEventId"),
        googleMeetLink=doc.get("googleMeetLink"),
        createdAt=doc["createdAt"],
        updatedAt=doc["updatedAt"],
    )


async def create_interview(data: dict) -> InterviewResponse:
    db = get_db()
    now = datetime.utcnow()
    doc = {**data, "createdAt": now, "updatedAt": now}
    result = await db[settings.MONGO_COLLECTION_INTERVIEWS].insert_one(doc)
    doc["_id"] = result.inserted_id
    logger.info("Interview created", extra={"interviewId": str(result.inserted_id)})
    return _to_response(doc)


async def get_interview(interview_id: str) -> InterviewResponse | None:
    db = get_db()
    doc = await db[settings.MONGO_COLLECTION_INTERVIEWS].find_one(
        {"_id": ObjectId(interview_id)}
    )
    return _to_response(doc) if doc else None


async def update_interview(interview_id: str, updates: dict) -> InterviewResponse | None:
    db = get_db()
    updates["updatedAt"] = datetime.utcnow()
    await db[settings.MONGO_COLLECTION_INTERVIEWS].update_one(
        {"_id": ObjectId(interview_id)}, {"$set": updates}
    )
    return await get_interview(interview_id)


async def list_interviews_for_candidate(candidate_id: str) -> list[InterviewResponse]:
    db = get_db()
    cursor = db[settings.MONGO_COLLECTION_INTERVIEWS].find({"candidateId": candidate_id})
    return [_to_response(doc) async for doc in cursor]


async def list_interviews_for_job(job_id: str) -> list[InterviewResponse]:
    db = get_db()
    cursor = db[settings.MONGO_COLLECTION_INTERVIEWS].find({"jobId": job_id})
    return [_to_response(doc) async for doc in cursor]
