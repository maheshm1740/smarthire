import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.consumers.job_consumer import JobCreatedConsumer
from app.core.config import settings
from app.core.logging import configure_logging
from app.routers.jd_router import router

configure_logging()
logger = logging.getLogger(__name__)

consumer: JobCreatedConsumer | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global consumer
    logger.info("Starting JD Generator service")

    consumer = JobCreatedConsumer()
    consumer_task = asyncio.create_task(consumer.start())

    yield

    logger.info("Shutting down JD Generator service")
    if consumer:
        await consumer.stop()
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        pass


app = FastAPI(
    title="SmartHire JD Generator",
    description="Generates and enhances job descriptions using Claude API with SSE streaming",
    version="1.0.0",
    lifespan=lifespan,
)

Instrumentator().instrument(app).expose(app)
app.include_router(router)


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
