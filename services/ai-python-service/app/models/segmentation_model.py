from pathlib import Path

import cv2
import numpy as np
import torch

from .mobileunetv3 import MobileUNetv3

WEIGHTS_PATH = Path(__file__).parent.parent.parent / "weights" / "mobileunetv3_best.pth"
INPUT_SIZE = 512
THRESHOLD = 0.5
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

_MEAN = np.array([0.485, 0.456, 0.406], dtype=np.float32)
_STD  = np.array([0.229, 0.224, 0.225], dtype=np.float32)

_model: MobileUNetv3 = None


def load_model() -> None:
    global _model
    m = MobileUNetv3(n_classes=1, pretrained=False)
    state = torch.load(WEIGHTS_PATH, map_location=DEVICE)
    m.load_state_dict(state)
    m.to(DEVICE)
    m.eval()
    _model = m


def _apply_clahe(img_rgb: np.ndarray) -> np.ndarray:
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    channels = list(cv2.split(img_rgb))
    return cv2.merge([clahe.apply(ch) for ch in channels])


def predict(image_bytes: bytes) -> tuple:
    if _model is None:
        raise RuntimeError("Model is not loaded. Call load_model() first.")

    arr = np.frombuffer(image_bytes, np.uint8)
    img_bgr = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img_bgr is None:
        raise ValueError("Could not decode image bytes.")

    img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
    img_rgb = cv2.resize(img_rgb, (INPUT_SIZE, INPUT_SIZE), interpolation=cv2.INTER_LINEAR)
    img_rgb = _apply_clahe(img_rgb)

    img_f = img_rgb.astype(np.float32) / 255.0
    img_f = (img_f - _MEAN) / _STD

    tensor = torch.from_numpy(img_f.transpose(2, 0, 1)).unsqueeze(0).to(DEVICE)

    with torch.no_grad():
        output = _model(tensor)
        probs = torch.sigmoid(output["out"]).squeeze().cpu().numpy()

    mask = (probs >= THRESHOLD).astype(np.uint8) * 255

    fg = probs[mask > 0]
    confidence = float(fg.mean()) if len(fg) > 0 else 0.0

    return mask, confidence
