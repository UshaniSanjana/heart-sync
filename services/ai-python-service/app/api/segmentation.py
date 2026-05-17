import base64
import time

import cv2
from fastapi import APIRouter, HTTPException

from ..models import segmentation_model
from ..schemas.inference import SegmentationRequest, SegmentationResponse

router = APIRouter()


@router.post("/segment", response_model=SegmentationResponse)
async def segment(request: SegmentationRequest):
    t0 = time.time()

    try:
        image_bytes = base64.b64decode(request.image_base64)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid base64 image data.")

    try:
        mask, confidence = segmentation_model.predict(image_bytes)
    except ValueError as e:
        raise HTTPException(status_code=422, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Segmentation failed: {e}")

    _, buf = cv2.imencode(".png", mask)
    mask_b64 = base64.b64encode(buf.tobytes()).decode()

    return SegmentationResponse(
        mask_base64=mask_b64,
        confidence=round(confidence, 4),
        processing_time_ms=int((time.time() - t0) * 1000),
    )
