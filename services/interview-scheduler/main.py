import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.core.config import settings
from app.core.logging import configure_logging
from app.core.mongo_client import close_db
from app.publishers.interview_publisher import close_producer
from app.routers.interview_router import auth_router, interview_router

configure_logging()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting Interview Scheduler service")
    yield
    logger.info("Shutting down Interview Scheduler service")
    await close_producer()
    await close_db()


app = FastAPI(
    title="SmartHire Interview Scheduler",
    description="Schedules interviews via Google Calendar with Kafka event publishing",
    version="1.0.0",
    lifespan=lifespan,
)

Instrumentator().instrument(app).expose(app)

app.include_router(interview_router)
app.include_router(auth_router)


@app.get("/health")
async def health():
    return {"status": "healthy", "service": settings.SERVICE_NAME}


@app.get("/")
async def root():
    return {"service": settings.SERVICE_NAME, "version": "1.0.0"}
