# -------------------------------------------------------
#  ECG Model Service — Python/FastAPI
#
#  Wraps the Swin Transformer ECG classifier.
#  CPU-only build to keep the image lightweight.
#
#  Build:  docker compose build ecg-model-service
#  Run:    docker compose up ecg-model-service
# -------------------------------------------------------

FROM python:3.11-slim

# Prevent Python from writing .pyc files and enable unbuffered output
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# Install system dependencies needed by Pillow
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        libjpeg62-turbo-dev \
        libpng-dev \
        curl \
    && rm -rf /var/lib/apt/lists/*

# Install CPU-only PyTorch FIRST (separate layer, ~200MB instead of ~2GB)
# Using --index-url forces pip to ONLY use the CPU wheel index
RUN pip install --no-cache-dir \
    torch==2.4.0+cpu \
    --index-url https://download.pytorch.org/whl/cpu

# Install remaining Python dependencies (cached layer)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app/ ./app/

# HuggingFace model cache directory (mounted as volume for persistence)
ENV HF_HOME=/app/.cache/huggingface

EXPOSE 8086

# Health check — verify the service is up and model is loaded
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:8086/health || exit 1

# Start FastAPI with uvicorn
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8086"]
