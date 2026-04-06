import logging

import fitz  # PyMuPDF

logger = logging.getLogger(__name__)


def extract_text_from_pdf(pdf_bytes: bytes) -> str:
    """
    Extract raw text from a PDF using PyMuPDF.
    Preserves page order and merges all pages into a single string.
    """
    doc = fitz.open(stream=pdf_bytes, filetype="pdf")
    pages_text: list[str] = []

    for page_num, page in enumerate(doc):
        text = page.get_text("text")
        if text.strip():
            pages_text.append(text)

    doc.close()
    full_text = "\n".join(pages_text)

    logger.info(
        "PDF text extracted",
        extra={"pages": len(pages_text), "char_count": len(full_text)},
    )
    return full_text
