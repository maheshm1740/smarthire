import logging

from aiokafka import AIOKafkaProducer
from aiokafka.helpers import create_ssl_context

from app.core.config import settings
from app.schemas.events import ResumeParsedEvent

logger = logging.getLogger(__name__)

_producer: AIOKafkaProducer | None = None


async def get_producer() -> AIOKafkaProducer:
    global _producer
    if _producer is None:
        # with:
        use_sasl = bool(settings.KAFKA_SASL_USERNAME)

        producer_kwargs = dict(
            bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
            value_serializer=lambda v: v.encode("utf-8"),
            key_serializer=lambda k: k.encode("utf-8") if k else None,
        )

        if use_sasl:
            producer_kwargs.update(
                security_protocol="SASL_SSL",
                sasl_mechanism="SCRAM-SHA-256",
                sasl_plain_username=settings.KAFKA_SASL_USERNAME,
                sasl_plain_password=settings.KAFKA_SASL_PASSWORD,
                ssl_context=create_ssl_context(),
            )

        _producer = AIOKafkaProducer(**producer_kwargs)
        await _producer.start()
        logger.info("Kafka producer started")
    return _producer


async def publish_resume_parsed(event: ResumeParsedEvent) -> None:
    producer = await get_producer()
    payload = event.model_dump_json()

    await producer.send_and_wait(
        topic=settings.KAFKA_TOPIC_RESUME_PARSED,
        key=event.applicationId,
        value=payload,
    )

    logger.info(
        "Published resume.parsed",
        extra={
            "applicationId": event.applicationId,
            "candidateId": event.candidateId,
            "skills_count": len(event.parsedResume.skills),
            "experience_years": event.parsedResume.experience_years,
        },
    )


async def close_producer() -> None:
    global _producer
    if _producer:
        await _producer.stop()
        _producer = None
        logger.info("Kafka producer stopped")
