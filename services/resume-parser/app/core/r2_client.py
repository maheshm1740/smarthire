import logging

import boto3
from botocore.client import Config

from app.core.config import settings

logger = logging.getLogger(__name__)


def get_r2_client():
    return boto3.client(
        "s3",
        endpoint_url=settings.R2_ENDPOINT,
        aws_access_key_id=settings.R2_ACCESS_KEY_ID,
        aws_secret_access_key=settings.R2_SECRET_ACCESS_KEY,
        config=Config(signature_version="s3v4"),
        region_name="auto",
    )


async def download_resume_bytes(object_key: str) -> bytes:
    """
    Download a resume PDF from Cloudflare R2.
    object_key is the full key stored in the event, e.g. 'resumes/abc123.pdf'
    """
    client = get_r2_client()
    logger.info("Downloading resume from R2", extra={"key": object_key})

    response = client.get_object(Bucket=settings.R2_BUCKET_NAME, Key=object_key)
    pdf_bytes = response["Body"].read()

    logger.info(
        "Resume downloaded",
        extra={"key": object_key, "size_bytes": len(pdf_bytes)},
    )
    return pdf_bytes
