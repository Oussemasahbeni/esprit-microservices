import time

import httpx
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import jwt
from jose.exceptions import JOSEError

from .config import KEYCLOAK_ISSUER_URI, KEYCLOAK_JWKS_URI

_bearer = HTTPBearer(auto_error=False)

_jwks_cache: dict = {"keys": [], "fetched_at": 0.0}
_JWKS_TTL_SECONDS = 300


async def _get_jwks() -> dict:
    """Fetches signing keys directly from KEYCLOAK_JWKS_URI — no OIDC discovery
    call against the issuer, since the issuer address may not be network-reachable
    from inside the container (it's the browser-facing address, not the internal one)."""
    if time.time() - _jwks_cache["fetched_at"] < _JWKS_TTL_SECONDS and _jwks_cache["keys"]:
        return _jwks_cache
    async with httpx.AsyncClient() as client:
        jwks = (await client.get(KEYCLOAK_JWKS_URI)).json()
    _jwks_cache["keys"] = jwks["keys"]
    _jwks_cache["fetched_at"] = time.time()
    return _jwks_cache


async def get_current_claims(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer),
) -> dict:
    """Validates the Keycloak-issued bearer token, mirroring the gateway's
    oauth2ResourceServer.jwt configuration (same issuer, same JWKS)."""
    if credentials is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Missing bearer token")

    token = credentials.credentials
    try:
        jwks = await _get_jwks()
        claims = jwt.decode(
            token,
            jwks,
            algorithms=["RS256"],
            issuer=KEYCLOAK_ISSUER_URI,
            options={"verify_aud": False},
        )
    except JOSEError as exc:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, f"Invalid token: {exc}") from exc

    return claims
