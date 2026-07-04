import logging

import httpx

from .config import CONSUL_HOST, CONSUL_PORT, SERVICE_HOST, SERVICE_NAME, SERVICE_PORT

logger = logging.getLogger("ai-service.consul")

_SERVICE_ID = f"{SERVICE_NAME}-{SERVICE_HOST}-{SERVICE_PORT}"


def _consul_url(path: str) -> str:
    return f"http://{CONSUL_HOST}:{CONSUL_PORT}{path}"


async def register() -> None:
    payload = {
        "ID": _SERVICE_ID,
        "Name": SERVICE_NAME,
        "Address": SERVICE_HOST,
        "Port": SERVICE_PORT,
        "Tags": ["python", "ai", "fastapi"],
        "Check": {
            "HTTP": f"http://{SERVICE_HOST}:{SERVICE_PORT}/api/ai/health",
            "Interval": "10s",
            "Timeout": "5s",
            "DeregisterCriticalServiceAfter": "1m",
        },
    }
    async with httpx.AsyncClient() as client:
        resp = await client.put(_consul_url("/v1/agent/service/register"), json=payload)
        resp.raise_for_status()
    logger.info("Registered %s with Consul at %s:%s", _SERVICE_ID, CONSUL_HOST, CONSUL_PORT)


async def deregister() -> None:
    async with httpx.AsyncClient() as client:
        resp = await client.put(_consul_url(f"/v1/agent/service/deregister/{_SERVICE_ID}"))
        resp.raise_for_status()
    logger.info("Deregistered %s from Consul", _SERVICE_ID)
