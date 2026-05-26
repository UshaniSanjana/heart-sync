from contextlib import asynccontextmanager

from fastapi import FastAPI

from .api import qca, segmentation
from .models import segmentation_model


@asynccontextmanager
async def lifespan(app: FastAPI):
    segmentation_model.load_model()
    yield


app = FastAPI(
    title="HeartSync AI Python Service",
    version="1.0.0",
    description="Coronary segmentation (MobileUNetv3) and QCA analysis",
    lifespan=lifespan,
)

app.include_router(segmentation.router)
app.include_router(qca.router)


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "model_loaded": segmentation_model._model is not None,
    }
