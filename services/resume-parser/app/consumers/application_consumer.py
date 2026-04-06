import asyncio
import json
import logging
import time

from aiokafka import AIOKafkaConsumer
from aiokafka.helpers import create_ssl_context

from app.core.config import settings
from app.core.r2_client import download_resume_bytes
from app.parsers.claude_enricher import enrich_with_claude
from app.parsers.pdf_extractor import extract_text_from_pdf
from app.parsers.spacy_parser import parse_with_spacy
from app.publishers.resume_publisher import close_producer, publish_resume_parsed
from app.schemas.events import ApplicationSubmittedEvent, ResumeParsedEvent

logger = logging.getLogger(__name__)


class ApplicationConsumer:
    def __init__(self):
        self.running = False
        self._consumer: AIOKafkaConsumer | None = None

    async def start(self) -> None:
        # replace this section inside start():
        ssl_context = create_ssl_context()

        self._consumer = AIOKafkaConsumer(
            settings.KAFKA_TOPIC_APPLICATION_SUBMITTED,
            bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
            security_protocol="SASL_SSL",
            sasl_mechanism="SCRAM-SHA-256",
            sasl_plain_username=settings.KAFKA_SASL_USERNAME,
            sasl_plain_password=settings.KAFKA_SASL_PASSWORD,
            ssl_context=ssl_context,
            group_id=settings.KAFKA_CONSUMER_GROUP,
            auto_offset_reset="earliest",
            enable_auto_commit=False,
            value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        )

        await self._consumer.start()
        self.running = True
        logger.info(
            "Consumer started",
            extra={"topic": settings.KAFKA_TOPIC_APPLICATION_SUBMITTED},
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
        logger.info("Consumer stopped")

    async def _handle_message(self, message) -> None:
        start_ms = int(time.time() * 1000)
        event_data = message.value

        try:
            event = ApplicationSubmittedEvent(**event_data)
        except Exception as exc:
            logger.error(
                "Invalid event schema — skipping",
                extra={"error": str(exc), "raw": event_data},
            )
            await self._consumer.commit()
            return

        logger.info(
            "Processing application",
            extra={
                "applicationId": event.applicationId,
                "candidateId": event.candidateId,
                "jobId": event.jobId,
            },
        )

        try:
            # Step 1: Download PDF from R2
            pdf_bytes = await download_resume_bytes(event.resumeUrl)

            # Step 2: Extract raw text with PyMuPDF
            raw_text = extract_text_from_pdf(pdf_bytes)

            if not raw_text.strip():
                logger.warning(
                    "Empty text extracted from PDF — skipping",
                    extra={"applicationId": event.applicationId},
                )
                await self._consumer.commit()
                return

            # Step 3: spaCy NER — first pass
            spacy_result = parse_with_spacy(raw_text)

            # Step 4: Claude API — semantic enrichment
            final_result = await enrich_with_claude(raw_text, spacy_result)

            # Step 5: Publish resume.parsed
            duration_ms = int(time.time() * 1000) - start_ms
            parsed_event = ResumeParsedEvent(
                applicationId=event.applicationId,
                candidateId=event.candidateId,
                jobId=event.jobId,
                parsedResume=final_result,
                parsingDurationMs=duration_ms,
            )
            await publish_resume_parsed(parsed_event)

            # Commit only after successful publish
            await self._consumer.commit()

            logger.info(
                "Resume parsed and published",
                extra={
                    "applicationId": event.applicationId,
                    "duration_ms": duration_ms,
                    "skills": final_result.skills,
                    "experience_years": final_result.experience_years,
                },
            )

        except Exception as exc:
            logger.exception(
                "Failed to process resume — will retry on restart (no commit)",
                extra={"applicationId": event.applicationId, "error": str(exc)},
            )
            # Do NOT commit — Kafka will redeliver this message after consumer restart
