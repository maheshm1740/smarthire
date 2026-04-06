import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.consumers.resume_consumer import ResumeConsumer
from app.core.config import settings
from app.core.logging import configure_logging
from app.core.mongo_client import close_db
from app.rankers.embeddings import get_model

configure_logging()
logger = logging.getLogger(__name__)

consumer: ResumeConsumer | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global consumer

    # Pre-load the embedding model on startup so first message isn't slow
    logger.info("Pre-loading embedding model...")
    get_model()

    consumer = ResumeConsumer()
    consumer_task = asyncio.create_task(consumer.start())

    yield

    logger.info("Shutting down Ranking Engine")
    if consumer:
        await consumer.stop()
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        pass
    await close_db()


app = FastAPI(
    title="SmartHire Ranking Engine",
    description="Scores candidates using sentence-transformers embeddings + weighted cosine similarity",
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
