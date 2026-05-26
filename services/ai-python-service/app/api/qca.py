import base64
import time

import cv2
import numpy as np
from fastapi import APIRouter, HTTPException

from ..models.qca_pipeline import (
    QCAConfig,
    draw_overlay,
    morph_cleanup,
    qca_from_mask,
    to_binary_mask,
)
from ..schemas.inference import LesionResult, QcaRequest, QcaResponse

router = APIRouter()


def _overall_risk(lesions: list) -> str:
    if not lesions:
        return "LOW"
    max_ds = max(l["DS_percent"] for l in lesions)
    if max_ds >= 50.0:
        return "HIGH"
    if max_ds >= 30.0:
        return "MODERATE"
    return "LOW"


@router.post("/qca", response_model=QcaResponse)
async def run_qca(request: QcaRequest):
    t0 = time.time()

    try:
        mask_bytes = base64.b64decode(request.mask_base64)
        arr = np.frombuffer(mask_bytes, np.uint8)
        mask_img = cv2.imdecode(arr, cv2.IMREAD_GRAYSCALE)
        if mask_img is None:
            raise ValueError("Could not decode mask image.")
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid mask data: {e}")

    cfg = QCAConfig(px_to_mm=request.px_to_mm)
    bw = to_binary_mask(mask_img)
    bw = morph_cleanup(bw, cfg)

    try:
        branches, lesions, dt = qca_from_mask(bw, cfg)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"QCA analysis failed: {e}")

    results = []
    for i, les in enumerate(lesions[:10], start=1):
        results.append(LesionResult(
            rank=i,
            ds_percent=round(les["DS_percent"], 2),
            severity=les["severity"],
            mld_px=round(les["MLD_px"], 3),
            rvd_px=round(les["RVD_px"], 3),
            length_px=round(les["length_px"], 3),
            mld_mm=round(les["MLD_mm"], 3) if les["MLD_mm"] is not None else None,
            rvd_mm=round(les["RVD_mm"], 3) if les["RVD_mm"] is not None else None,
            length_mm=round(les["length_mm"], 3) if les["length_mm"] is not None else None,
            narrowest_point=list(les["min_pt"]),
        ))

    overlay_b64 = None
    if request.image_base64:
        try:
            img_bytes = base64.b64decode(request.image_base64)
            img_arr = np.frombuffer(img_bytes, np.uint8)
            angio = cv2.imdecode(img_arr, cv2.IMREAD_GRAYSCALE)
            if angio is not None:
                overlay = draw_overlay(angio, bw, branches, lesions)
                _, buf = cv2.imencode(".png", overlay)
                overlay_b64 = base64.b64encode(buf.tobytes()).decode()
        except Exception:
            pass

    return QcaResponse(
        lesions=results,
        total_branches=len(branches),
        overall_risk=_overall_risk(lesions),
        calibrated=request.px_to_mm is not None,
        overlay_base64=overlay_b64,
        processing_time_ms=int((time.time() - t0) * 1000),
    )
