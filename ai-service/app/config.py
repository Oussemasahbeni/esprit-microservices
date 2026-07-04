import os

SERVICE_NAME = "ai-service"
SERVICE_PORT = int(os.getenv("SERVICE_PORT", "8090"))
SERVICE_HOST = os.getenv("SERVICE_HOST", "127.0.0.1")

CONSUL_HOST = os.getenv("CONSUL_HOST", "localhost")
CONSUL_PORT = int(os.getenv("CONSUL_PORT", "8500"))

KEYCLOAK_ISSUER_URI = os.getenv("KEYCLOAK_ISSUER_URI", "http://localhost:9000/realms/esprit")
# Physical network address used to fetch the signing keys — deliberately
# decoupled from KEYCLOAK_ISSUER_URI (which stays a pure string compared
# against the token's "iss" claim). In Docker this points at the Keycloak
# container's internal service name/port; the issuer stays localhost:9000
# because Keycloak's KC_HOSTNAME setting pins the issuer regardless of which
# address a backend used to physically reach it.
KEYCLOAK_JWKS_URI = os.getenv(
    "KEYCLOAK_JWKS_URI",
    f"{KEYCLOAK_ISSUER_URI}/protocol/openid-connect/certs",
)
