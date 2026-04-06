import logging

import numpy as np
from sentence_transformers import SentenceTransformer

from app.core.config import settings

logger = logging.getLogger(__name__)

_model: SentenceTransformer | None = None


def get_model() -> SentenceTransformer:
    """Load the embedding model once at startup — never reload per request."""
    global _model
    if _model is None:
        logger.info("Loading embedding model", extra={"model": settings.EMBEDDING_MODEL})
        _model = SentenceTransformer(settings.EMBEDDING_MODEL)
        logger.info("Embedding model loaded")
    return _model


def embed(text: str) -> np.ndarray:
    """Return a normalised embedding vector for a single text string."""
    model = get_model()
    vector = model.encode(text, normalize_embeddings=True, show_progress_bar=False)
    return vector


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    """
    Cosine similarity between two normalised vectors.
    Both inputs must be L2-normalised (which SentenceTransformer does when
    normalize_embeddings=True), so this reduces to a dot product.
    Result is clamped to [0.0, 1.0].
    """
    raw = float(np.dot(a, b))
    return max(0.0, min(1.0, raw))
