import logging

from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase

from app.core.config import settings

logger = logging.getLogger(__name__)

_client: AsyncIOMotorClient | None = None


def get_db() -> AsyncIOMotorDatabase:
    global _client
    if _client is None:
        _client = AsyncIOMotorClient(settings.MONGO_URI)
        logger.info("MongoDB client initialised", extra={"db": settings.MONGO_DB})
    return _client[settings.MONGO_DB]


async def close_db() -> None:
    global _client
    if _client:
        _client.close()
        _client = None
