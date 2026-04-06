import logging
from datetime import datetime

from app.core.mongo_client import get_db
from app.core.config import settings
from app.schemas.events import CandidateRankedEvent

logger = logging.getLogger(__name__)


async def fetch_job(job_id: str) -> dict | None:
    """
    Fetch a job document from MongoDB.
    The Job Service stores jobs in the same Atlas cluster under the 'jobs' collection.
    Returns None if the job is not found.
    """
    db = get_db()
    job = await db[settings.MONGO_COLLECTION_JOBS].find_one({"jobId": job_id})
    if not job:
        logger.warning("Job not found in MongoDB", extra={"jobId": job_id})
    return job


async def store_candidate_score(event: CandidateRankedEvent) -> None:
    """
    Write the score and breakdown into the candidate's application sub-document.
    Uses $set on the matched application array element (arrayFilters).
    """
    db = get_db()

    result = await db[settings.MONGO_COLLECTION_CANDIDATES].update_one(
        {
            "userId": event.candidateId,
            "applications.jobId": event.jobId,
            "applications.applicationId": event.applicationId,
        },
        {
            "$set": {
                "applications.$[app].score": event.score,
                "applications.$[app].scoreBreakdown": event.scoreBreakdown.model_dump(),
                "applications.$[app].rankedAt": datetime.utcnow(),
                "applications.$[app].status": "RANKED",
            }
        },
        array_filters=[{"app.applicationId": event.applicationId}],
    )

    if result.modified_count == 0:
        logger.warning(
            "Candidate document not updated — application not found",
            extra={
                "candidateId": event.candidateId,
                "applicationId": event.applicationId,
            },
        )
    else:
        logger.info(
            "Candidate score stored",
            extra={
                "candidateId": event.candidateId,
                "applicationId": event.applicationId,
                "score": event.score,
            },
        )
