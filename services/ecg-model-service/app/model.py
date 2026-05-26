"""
ECG Image Classification Model

Loads the Swin Transformer model fine-tuned for ECG image classification
from HuggingFace: gianlab/swin-tiny-patch4-window7-224-finetuned-ecg-classification

Classification labels:
  N - Normal beat
  S - Supraventricular premature beat
  V - Premature ventricular contraction
  F - Fusion of ventricular and normal beat
  Q - Unclassifiable beat
  M - Myocardial Infarction
"""

import logging
from io import BytesIO

import torch
from PIL import Image
from transformers import AutoImageProcessor, AutoModelForImageClassification

logger = logging.getLogger(__name__)

MODEL_ID = "gianlab/swin-tiny-patch4-window7-224-finetuned-ecg-classification"

# Human-readable labels for each class code
LABEL_MAP = {
    "N": "Normal beat",
    "S": "Supraventricular premature beat",
    "V": "Premature ventricular contraction",
    "F": "Fusion of ventricular and normal beat",
    "Q": "Unclassifiable beat",
    "M": "Myocardial Infarction",
}


class EcgClassifier:
    """Wraps the HuggingFace Swin Transformer for ECG image classification."""

    def __init__(self):
        self.processor = None
        self.model = None
        self.id2label = None
        self._loaded = False

    def load(self):
        """Download and load the model from HuggingFace (cached after first download)."""
        logger.info("Loading ECG classification model: %s", MODEL_ID)
        self.processor = AutoImageProcessor.from_pretrained(MODEL_ID)
        self.model = AutoModelForImageClassification.from_pretrained(MODEL_ID)
        self.model.eval()
        self.id2label = self.model.config.id2label
        self._loaded = True
        logger.info("Model loaded successfully. Labels: %s", self.id2label)

    @property
    def is_loaded(self) -> bool:
        return self._loaded

    def predict(self, image_bytes: bytes) -> dict:
        """
        Run inference on raw image bytes.

        Returns:
            {
                "prediction": "N",
                "label": "Normal beat",
                "confidence": 0.94,
                "all_predictions": {"N": 0.94, "S": 0.02, ...}
            }
        """
        if not self._loaded:
            raise RuntimeError("Model not loaded. Call load() first.")

        # Open and convert to RGB (handles PNG, JPG, BMP, etc.)
        image = Image.open(BytesIO(image_bytes)).convert("RGB")

        # Preprocess: resize to 224x224 + normalize
        inputs = self.processor(images=image, return_tensors="pt")

        # Inference (no gradient computation needed)
        with torch.no_grad():
            outputs = self.model(**inputs)

        # Convert logits to probabilities
        probs = torch.nn.functional.softmax(outputs.logits, dim=-1)[0]

        # Build results
        all_predictions = {}
        for idx, prob in enumerate(probs):
            class_code = self.id2label.get(idx, str(idx))
            all_predictions[class_code] = round(prob.item(), 4)

        # Top prediction
        top_idx = torch.argmax(probs).item()
        top_code = self.id2label.get(top_idx, str(top_idx))
        top_confidence = probs[top_idx].item()

        return {
            "prediction": top_code,
            "label": LABEL_MAP.get(top_code, top_code),
            "confidence": round(top_confidence, 4),
            "all_predictions": all_predictions,
        }
