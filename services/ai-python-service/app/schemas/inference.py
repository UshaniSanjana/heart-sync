from pydantic import BaseModel
from typing import Optional, List


class SegmentationRequest(BaseModel):
    image_base64: str
    patient_id: str
    record_id: str


class SegmentationResponse(BaseModel):
    mask_base64: str
    confidence: float
    processing_time_ms: int


class QcaRequest(BaseModel):
    mask_base64: str
    image_base64: Optional[str] = None
    patient_id: str
    record_id: str
    px_to_mm: Optional[float] = None


class LesionResult(BaseModel):
    rank: int
    ds_percent: float
    severity: str
    mld_px: float
    rvd_px: float
    length_px: float
    mld_mm: Optional[float] = None
    rvd_mm: Optional[float] = None
    length_mm: Optional[float] = None
    narrowest_point: List[int]


class QcaResponse(BaseModel):
    lesions: List[LesionResult]
    total_branches: int
    overall_risk: str
    calibrated: bool
    overlay_base64: Optional[str] = None
    processing_time_ms: int
