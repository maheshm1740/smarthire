import asyncio
import json
import logging

from aiokafka import AIOKafkaConsumer
from aiokafka.helpers import create_ssl_context

from app.core.config import settings
from app.schemas.models import EnhanceJDRequest, ExperienceLevel, JobCreatedEvent
from app.services.claude_service import enhance_jd_full

logger = logging.getLogger(__name__)


class JobCreatedConsumer:
    """
    Optional Kafka consumer — listens to job.created events and
    auto-enhances the job description using Claude API.

    This runs in the background and does not block the REST API.
    Enhancement failures are logged and skipped — they do not affect
    the job posting itself.
    """

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
            settings.KAFKA_TOPIC_JOB_CREATED,
            **consumer_kwargs,
        )

        await self._consumer.start()
        self.running = True
        logger.info(
            "JD enhancement consumer started",
            extra={"topic": settings.KAFKA_TOPIC_JOB_CREATED},
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

    async def _handle_message(self, message) -> None:
        try:
            event = JobCreatedEvent(**message.value)
        except Exception as exc:
            logger.error("Invalid event schema — skipping", extra={"error": str(exc)})
            await self._consumer.commit()
            return

        logger.info(
            "Auto-enhancing JD for new job",
            extra={"jobId": event.jobId, "title": event.title},
        )

        try:
            enhance_req = EnhanceJDRequest(
                jobId=event.jobId,
                existingDescription=event.description,
                title=event.title,
                skills=event.requirements,
                experienceLevel=ExperienceLevel.MID,
            )
            enhanced = await enhance_jd_full(enhance_req)

            # Log the enhanced JD — in production you would call the
            # Job Service REST API to update the description in PostgreSQL
            logger.info(
                "JD enhanced successfully",
                extra={
                    "jobId": event.jobId,
                    "original_length": len(event.description),
                    "enhanced_length": len(enhanced),
                },
            )

            await self._consumer.commit()

        except Exception as exc:
            logger.exception(
                "JD enhancement failed — skipping (non-critical)",
                extra={"jobId": event.jobId, "error": str(exc)},
            )
            # Commit anyway — JD enhancement failure should not block the pipeline
            await self._consumer.commit()
