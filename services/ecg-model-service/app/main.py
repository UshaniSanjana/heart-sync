"""
ECG Model Service — FastAPI Application

A lightweight microservice that wraps the Swin Transformer ECG classifier.
Accepts ECG image uploads and returns classification predictions.

Endpoints:
  GET  /health   — Health check (includes model load status)
  POST /predict  — Classify an ECG image
"""

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse

from app.model import EcgClassifier

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
)
logger = logging.getLogger(__name__)

# Global model instance
classifier = EcgClassifier()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load the model at startup, cleanup at shutdown."""
    logger.info("Starting ECG Model Service — loading model...")
    classifier.load()
    logger.info("Model ready. Service is accepting requests.")
    yield
    logger.info("Shutting down ECG Model Service.")


app = FastAPI(
    title="ECG Model Service",
    description="Swin Transformer ECG Image Classifier for HeartSync",
    version="1.0.0",
    lifespan=lifespan,
)


@app.get("/health")
async def health():
    """Health check endpoint for Docker and service discovery."""
    return {
        "status": "ok",
        "model_loaded": classifier.is_loaded,
        "model": "swin-tiny-patch4-window7-224-finetuned-ecg-classification",
    }


@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    """
    Classify an ECG image.

    Accepts: multipart/form-data with a 'file' field containing the ECG image.
    Supported formats: PNG, JPG, JPEG, BMP, TIFF

    Returns JSON with prediction, label, confidence, and all class probabilities.
    """
    if not classifier.is_loaded:
        raise HTTPException(status_code=503, detail="Model not loaded yet")

    # Validate content type
    content_type = file.content_type or ""
    if not content_type.startswith("image/"):
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type: {content_type}. Expected an image file.",
        )

    try:
        image_bytes = await file.read()
        if len(image_bytes) == 0:
            raise HTTPException(status_code=400, detail="Empty file uploaded")

        result = classifier.predict(image_bytes)
        return JSONResponse(content=result)

    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Prediction failed")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")
