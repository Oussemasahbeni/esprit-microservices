import logging
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI
from pydantic import BaseModel

from . import consul_registration
from .security import get_current_claims
from .sentiment import analyze

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ai-service")


@asynccontextmanager
async def lifespan(_: FastAPI):
    try:
        await consul_registration.register()
    except Exception:
        logger.exception("Failed to register with Consul (will still serve traffic)")
    yield
    try:
        await consul_registration.deregister()
    except Exception:
        logger.exception("Failed to deregister from Consul")


app = FastAPI(title="ai-service", lifespan=lifespan)

# CORS is handled once, by the gateway (the only public-facing entry point).
# ai-service is never called directly by a browser, so it must not add its
# own Access-Control-* headers -- doing so duplicates the gateway's headers
# and browsers reject responses with more than one Access-Control-Allow-Origin
# value, even on an otherwise-successful 200.


class SentimentRequest(BaseModel):
    text: str


class SentimentResponse(BaseModel):
    label: str
    scores: dict


@app.get("/api/ai/health")
def health() -> dict:
    return {"status": "UP"}


@app.post("/api/ai/sentiment", response_model=SentimentResponse)
def sentiment(request: SentimentRequest, claims: dict = Depends(get_current_claims)) -> SentimentResponse:
    """Analyzes the sentiment of customer feedback text (reviews, delivery/reservation comments).

    Secured the same way as the Java services: a valid Keycloak-issued bearer
    token (checked against the same realm's JWKS) is required.
    """
    result = analyze(request.text)
    logger.info("sentiment requested by %s", claims.get("preferred_username", "unknown"))
    return SentimentResponse(**result)
