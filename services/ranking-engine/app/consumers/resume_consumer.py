import asyncio
import json
import logging
import time

from aiokafka import AIOKafkaConsumer
from aiokafka.helpers import create_ssl_context

from app.core.config import settings
from app.publishers.ranking_publisher import close_producer, publish_candidate_ranked
from app.rankers.mongo_repository import fetch_job, store_candidate_score
from app.rankers.scorer import compute_score
from app.schemas.events import CandidateRankedEvent, ResumeParsedEvent

logger = logging.getLogger(__name__)


class ResumeConsumer:
    def __init__(self):
        self.running = False
        self._consumer: AIOKafkaConsumer | None = None

    async def start(self) -> None:
        use_sasl = bool(settings.KAFKA_SASL_USERNAME)

        consumer_kwargs = dict(
            bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
            group_id=settings.KAFKA_CONSUMER_GROUP,
            auto_offset_reset="earliest",
            enable_auto_commit=False,
            value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        )

        if use_sasl:
            consumer_kwargs.update(
                security_protocol="SASL_SSL",
                sasl_mechanism="SCRAM-SHA-256",
                sasl_plain_username=settings.KAFKA_SASL_USERNAME,
                sasl_plain_password=settings.KAFKA_SASL_PASSWORD,
                ssl_context=create_ssl_context(),
            )

        self._consumer = AIOKafkaConsumer(
            settings.KAFKA_TOPIC_RESUME_PARSED,
            **consumer_kwargs,
        )

        await self._consumer.start()
        self.running = True
        logger.info(
            "Consumer started",
            extra={"topic": settings.KAFKA_TOPIC_RESUME_PARSED},
        )

        try:
            async for message in self._consumer:
                await self._handle_message(message)
        except asyncio.CancelledError:
            logger.info("Consumer cancelled")
        except Exception as exc:
            logger.exception("Consumer crashed", extra={"error": str(exc)})
        finally:
            self.running = False

    async def stop(self) -> None:
        self.running = False
        if self._consumer:
            await self._consumer.stop()
        await close_producer()

    async def _handle_message(self, message) -> None:
        start_ms = int(time.time() * 1000)

        try:
            event = ResumeParsedEvent(**message.value)
        except Exception as exc:
            logger.error("Invalid event schema — skipping", extra={"error": str(exc)})
            await self._consumer.commit()
            return

        logger.info(
            "Ranking candidate",
            extra={
                "applicationId": event.applicationId,
                "candidateId": event.candidateId,
                "jobId": event.jobId,
            },
        )

        try:
            # Step 1: Fetch job requirements from MongoDB
            job = await fetch_job(event.jobId)

            job_requirements: list[str] = []
            job_description: str = ""
            required_experience: float = 0.0

            if job:
                job_requirements = job.get("requirements", [])
                job_description = job.get("description", "")
                required_experience = float(job.get("experienceYears", 0))

            # Step 2: Compute score
            breakdown = compute_score(
                candidate_resume=event.parsedResume,
                job_requirements=job_requirements,
                job_description=job_description,
                required_experience_years=required_experience,
            )

            # Step 3: Write score to MongoDB candidate document
            ranked_event = CandidateRankedEvent(
                applicationId=event.applicationId,
                candidateId=event.candidateId,
                jobId=event.jobId,
                score=breakdown.final_score,
                scoreBreakdown=breakdown,
                rankingDurationMs=int(time.time() * 1000) - start_ms,
            )
            await store_candidate_score(ranked_event)

            # Step 4: Publish candidate.ranked
            await publish_candidate_ranked(ranked_event)

            await self._consumer.commit()

            logger.info(
                "Ranking complete",
                extra={
                    "applicationId": event.applicationId,
                    "score": breakdown.final_score,
                    "duration_ms": ranked_event.rankingDurationMs,
                },
            )

        except Exception as exc:
            logger.exception(
                "Failed to rank candidate — will retry on restart",
                extra={"applicationId": event.applicationId, "error": str(exc)},
            )