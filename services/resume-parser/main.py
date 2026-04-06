import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.consumers.application_consumer import ApplicationConsumer
from app.core.config import settings
from app.core.logging import configure_logging

configure_logging()
logger = logging.getLogger(__name__)

consumer: ApplicationConsumer | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global consumer
    logger.info("Starting Resume Parser service", extra={"service": settings.SERVICE_NAME})

    consumer = ApplicationConsumer()
    consumer_task = asyncio.create_task(consumer.start())

    yield

    logger.info("Shutting down Resume Parser service")
    if consumer:
        await consumer.stop()
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        pass


app = FastAPI(
    title="SmartHire Resume Parser",
    description="Parses resumes using spaCy NER + Claude API semantic analysis",
    version="1.0.0",
    lifespan=lifespan,
)

Instrumentator().instrument(app).expose(app)


@app.get("/health")
async def health():
    return {
        "status": "healthy",
        "service": settings.SERVICE_NAME,
        "consumer_running": consumer.running if consumer else False,
    }


@app.get("/")
async def root():
    return {"service": settings.SERVICE_NAME, "version": "1.0.0"}
