# Dockerization + AI Microservice — Full Session Handoff

This file is a complete context dump of a previous conversation with an LLM
(Claude Code) so that work can continue in a fresh conversation with full
context. Paste this whole file as the first message.

## Project paths

- `C:\Users\omarm\Downloads\espritèproject_rojla\esprit-microservices` — backend monorepo (Maven multi-module: gateway, reservation, employee-management, menu-management, delivery-management) + new `ai-service` (Python) + `compose.yaml`
- `C:\Users\omarm\Downloads\espritèproject_rojla\esprit-microservices-front` — Angular frontend (`esprit-frontend`, pnpm, spartan-ng/hlm components, transloco i18n)
- Root compose file: `esprit-microservices/compose.yaml`
- Consul KV seed file: `esprit-microservices/consul-kv.json` (UTF-16LE with BOM, values are base64-encoded YAML — see "How to edit consul-kv.json" below)
- Realm import: `esprit-microservices/keycloak/kc-realm/esprit-realm.json`

## Original request, in order (what the user actually asked for)

1. **Everything currently runs locally from the IDE** except shared infra
   (Consul, RabbitMQ, Keycloak+Postgres, app-postgres, menu-mysql, mail-dev),
   which already runs in Docker via `compose.yaml`. The user wants **all app
   microservices Dockerized too** and the **whole stack running via one
   `docker compose up`**.
2. **Hard constraint: do NOT change any env var / URL that currently defaults
   to `localhost`** (Keycloak issuer URI, Consul host, DB host, etc.).
   Reason given: changing the Keycloak issuer/URLs is perceived by the user
   as requiring "regenerating the security key" (client secrets), which they
   want to avoid at all costs. This constraint got revised later — see
   point 8.
3. User wants progress **tracked with the memory system and with many
   small tasks** so context is never lost across a long session.
4. **Explicitly rejected an nginx-based static production build** for the
   frontend Docker image ("stop imediatly wtf nginx", "wtf nginx idiot").
   Wants the Angular **dev server** (`ng serve`) running inside the
   container instead, mirroring local dev exactly. This preference is
   permanent — do not reintroduce nginx/static builds for this frontend
   unless the user explicitly asks for a production deployment.
5. **Docker images must NOT be built by `docker compose build`.** The user
   was very emphatic about this ("please, um, do not include docker files
   in the docker compose. This is a huge bad thing... never do... put...
   build inside the Docker compose because not every time I will fucking
   rebuild it"). Workflow must be:
   - Each service has its own `Dockerfile`.
   - Build manually: `docker build -f <service>/Dockerfile -t <image-name> .`
   - `compose.yaml` references pre-built images via `image: <image-name>:latest`
     ONLY — no `build:` key anywhere in the app-services section.
   - After a `git pull`, a contributor rebuilds images themselves using the
     Dockerfiles (which are committed to the repo) and the same image names,
     then `docker compose up -d` picks them up.
6. **A new Python AI microservice** must be added, fully integrated:
   - Registered in Consul (like the Java services).
   - Routed through the API Gateway (`/api/ai/**`).
   - Secured via Keycloak (same JWT validation approach as the Java
     services).
   - Have a frontend page/feature for it.
   - Be tested end-to-end.
   - User's own words: "I don't know what idea, but you are smart... it's
     AI, it uses the libraries of AI... lightweighted, something small...
     not production."
7. **Complaint about the first AI implementation**: the first version used
   `vaderSentiment` (a lexicon/rule-based sentiment library). The user was
   angry that this "looked fake/static" and demanded genuine ML: "there is
   no library actually used in AI and determining something... make it
   somehow look like real." → **Fixed by switching to a real pretrained
   transformer model** (DistilBERT fine-tuned on SST-2, via HuggingFace
   `transformers`), baked into the Docker image at build time so there's no
   runtime download dependency.
8. **Revised the "keep localhost, host networking" plan.** Initially the
   assistant used `network_mode: host` on every app container to preserve
   literal `localhost` in all env var defaults (reasoning: Docker Desktop's
   VM shares a network namespace so `localhost` inside a host-mode container
   can reach infra containers' ports published to the host). This was
   **empirically proven broken**: host-mode containers on this machine's
   Docker Desktop (Windows, WSL2 backend) are **not reachable from Windows
   itself** (browser, curl) unless a specific opt-in Docker Desktop setting
   ("Enable host networking" under Settings → Resources → Network) is
   turned on and Docker Desktop is restarted. Verified via a disposable
   test container (`docker run --rm -d --network host python:3.12-alpine
   python3 -m http.server 8099` → connection refused from the host shell).
   The user was asked to choose between enabling that setting (with a
   Docker Desktop restart, which would also disrupt unrelated
   `planningodeployment-*` containers) or switching to normal Docker bridge
   networking with service names. **User chose bridge networking with
   service names**, while explicitly asking to "be aware of" Keycloak
   issuer URL / secret consistency and to test it all. This led to the
   Keycloak issuer/JWKS decoupling design below — this is the CORRECT,
   permanent solution, not a workaround, and should not be reverted back to
   host networking.

## Current architecture (as of end of session)

### Networking
Standard Docker Compose bridge network (the default network Compose creates,
`esprit-microservices_default`). Every service resolves infra by **Docker
service name**, not `localhost`:
- Consul → `consul:8500`
- App Postgres → `app-postgres:5432` (note: internal port is `5432`, NOT the
  host-published `5433`)
- Menu MySQL → `menu-mysql:3306`
- RabbitMQ → `rabbitmq:5672`
- Keycloak → `keycloak:8080` (internal container port; published externally
  as `9000`)

Only the browser-facing pieces publish ports to the host:
- `gateway` → `8080:8080`
- `frontend` → `4200:4200`
- Infra (`consul`, `keycloak`, `app-postgres`, `menu-mysql`, `rabbitmq`,
  `keycloak-postgres`, `mail-dev`) already published their usual ports from
  before this session — unchanged.

### Keycloak issuer / JWKS decoupling (important, don't undo)
Problem: services validate JWTs using
`spring.security.oauth2.resourceserver.jwt.issuer-uri`, which defaults to
`http://localhost:9000/realms/esprit` everywhere (frontend, gateway,
employee-management, delivery-management). On bridge networking, a backend
container's own `localhost` doesn't reach the Keycloak container — but the
**issuer claim in already-issued tokens must still say `localhost:9000`**
because that's the address the **browser** uses to log in, and changing it
would mean updating Keycloak client redirect URIs (which the user wants to
avoid).

Solution implemented (standard Spring Boot / Keycloak pattern — NOT a hack):
1. **Keycloak container**: added env var `KC_HOSTNAME: http://localhost:9000`
   in `compose.yaml`. This pins the `iss` claim of every issued token to
   exactly `http://localhost:9000/realms/esprit`, **regardless of the actual
   Host header/address** used to physically reach the Keycloak container
   (browser via `localhost:9000`, or a backend container via
   `keycloak:8080` — both get tokens with the same issuer).
2. **Backend services** (gateway, employee-management, delivery-management —
   the three with an `oauth2ResourceServer.jwt` config) get a **new**
   `jwk-set-uri` property added alongside the existing `issuer-uri` in their
   Consul KV config (`consul-kv.json`). When Spring Boot sees **both**
   `issuer-uri` and `jwk-set-uri` set, it does NOT perform OIDC discovery
   (which would require hitting `issuer-uri` over the network) — it fetches
   signing keys directly from `jwk-set-uri` and validates the `iss` claim
   as a pure string comparison against `issuer-uri`. This decouples "what
   address to physically fetch keys from" (internal:
   `http://keycloak:8080/realms/esprit/protocol/openid-connect/certs`) from
   "what issuer string to accept" (external: `http://localhost:9000/realms/esprit`,
   unchanged).
3. Same pattern manually implemented in the new Python `ai-service`
   (`app/security.py` / `app/config.py`): `KEYCLOAK_ISSUER_URI` is used only
   for a string comparison against the token's `iss` claim (no network call);
   `KEYCLOAK_JWKS_URI` (defaults to `http://keycloak:8080/realms/esprit/protocol/openid-connect/certs`
   via compose env var) is hit directly for the signing keys, bypassing
   OIDC discovery entirely.
4. **Verified working end-to-end**: decoded a real token's `iss` claim →
   exactly `http://localhost:9000/realms/esprit`. ai-service logs showed
   `GET http://keycloak:8080/realms/esprit/protocol/openid-connect/certs
   "HTTP/1.1 200 OK"`. Gateway correctly returned 401 for missing/invalid
   tokens and let valid tokens through to the ai-service.
5. **No Keycloak client secrets or redirect URIs were changed.**

### `consul-kv.json` — how to read/edit it
This file is **UTF-16LE with a BOM**, and each entry's `value` field is
**base64-encoded YAML** (not gzip — an earlier assumption in this session
that it was gzip was wrong; the leading `77u/...` in some values decodes to
a UTF-8 BOM, meaning it's just base64 of a UTF-8 string with a BOM prefix).
Standard tools like `cat`/`Read` will show mangled/garbled output because of
the UTF-16 encoding — use Node.js to decode/edit it, e.g.:

```js
const fs = require('fs');
const raw = fs.readFileSync('consul-kv.json', 'utf16le');
const data = JSON.parse(raw.charCodeAt(0) === 0xFEFF ? raw.slice(1) : raw);
// data is an array of {key, flags, value} — value is base64
const entry = data.find(e => e.key === 'config/gateway/data');
const yaml = Buffer.from(entry.value, 'base64').toString('utf8');
// ...edit yaml as a string...
entry.value = Buffer.from(yaml, 'utf8').toString('base64');
const out = JSON.stringify(data, null, '\t').replace(/^\[/, '[\n\t').replace(/\]$/, '\n]');
fs.writeFileSync('consul-kv.json', '﻿' + out, 'utf16le');
```

This file has 7 keys: `config/`, `config/application/data`,
`config/gateway/data`, `config/reservation/data`,
`config/employee-management/data`, `config/menu-management/data`,
`config/delivery-management/data`. It's imported into Consul KV on every
`docker compose up` by the one-shot `consul-kv-init` service (runs
`scripts/import-consul-kv.js`). **After editing consul-kv.json, you must
re-run `docker compose up -d consul-kv-init` and then restart whichever
service(s) consume the changed config** (Spring services read config from
Consul via `spring.config.import: consul:...` at boot — they don't hot
reload without a Consul watch mechanism being separately configured, so a
container restart is the safe way to pick up changes:
`docker restart gateway-service` etc.)

### Route table (gateway → services), all defined in `config/gateway/data`
Predicate `Path=/api/xxx/**` → `uri: lb://service-name` (load-balanced via
Consul service discovery, since all Spring services register with
`spring.cloud.consul.discovery` and `prefer-ip-address: true` — meaning they
register their own container IP with Consul, which works correctly on
bridge networking with zero extra config).

- `demo1` → `/api/users/**` (legacy/demo, still present, unused)
- `demo2` → `/api/products/**` (legacy/demo, still present, unused)
- `reservation` → `/api/reservations/**, /api/availability/**, /api/tables/**, /api/rooms/**, /api/manager/**`
- `employee-management` → `/api/employees/**`
- `menu-management` → `/api/menus/**`
- `delivery-management` → `/api/deliveries/**`
- `ai-service` (NEW) → `/api/ai/**`, filters: `RewritePath=/api/ai/(?<segment>.*), /api/ai/${segment}`
  (identity rewrite, added while debugging — see Known Issue below; the
  `CircuitBreaker` filter that other routes have was **removed** from this
  route during debugging and has NOT been re-added)

### Per-service environment variables needed on bridge networking
(all defined in `compose.yaml` under each service's `environment:` block)

- **gateway**: `CONSUL_HOST=consul`, `CONSUL_PORT=8500`,
  `KEYCLOAK_ISSUER_URI=http://localhost:9000/realms/esprit` (unchanged),
  `KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/esprit/protocol/openid-connect/certs`
- **reservation**: `CONSUL_HOST=consul`, `CONSUL_PORT=8500`,
  `DB_HOST=app-postgres`, `DB_PORT=5432`, `RABBITMQ_HOST=rabbitmq`,
  `KEYCLOAK_URL=http://keycloak:8080` (used for admin/service-account calls,
  property name in this service's YAML is `keycloak.server-url` bound to env
  var `KEYCLOAK_URL`, NOT `KEYCLOAK_SERVER_URL` — inconsistent naming
  between services, verified from decoded consul-kv.json)
- **employee-management**: `CONSUL_HOST=consul`, `CONSUL_PORT=8500`,
  `DB_HOST=app-postgres`, `DB_PORT=5432`, `RABBITMQ_HOST=rabbitmq`,
  `KEYCLOAK_ISSUER_URI=http://localhost:9000/realms/esprit`,
  `KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/esprit/protocol/openid-connect/certs`,
  `KEYCLOAK_SERVER_URL=http://keycloak:8080` (this service's YAML property
  name IS `KEYCLOAK_SERVER_URL`, different from reservation's `KEYCLOAK_URL`)
- **menu-management**: `CONSUL_HOST=consul`, `CONSUL_PORT=8500`,
  `DB_HOST=menu-mysql`, `DB_PORT=3306`, `RABBITMQ_HOST=rabbitmq`
- **delivery-management**: `CONSUL_HOST=consul`, `CONSUL_PORT=8500`,
  `DB_HOST=app-postgres`, `DB_PORT=5432`,
  `KEYCLOAK_ISSUER_URI=http://localhost:9000/realms/esprit`,
  `KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/esprit/protocol/openid-connect/certs`,
  `KEYCLOAK_SERVER_URL=http://keycloak:8080`
- **ai-service**: `CONSUL_HOST=consul`, `CONSUL_PORT=8500`,
  `SERVICE_HOST=ai-service` (so it registers with Consul under a
  Docker-DNS-resolvable name, not `127.0.0.1`),
  `KEYCLOAK_ISSUER_URI=http://localhost:9000/realms/esprit`,
  `KEYCLOAK_JWKS_URI=http://keycloak:8080/realms/esprit/protocol/openid-connect/certs`
- **frontend**: no special env vars — it's a browser-facing static/dev
  server; the browser calls `localhost:8080` (gateway) and `localhost:9000`
  (Keycloak) directly, both already correct and unaffected by internal
  Docker networking.

Note: `CONSUL_HOST`/`CONSUL_PORT` must be set as **container env vars**
(not just Consul KV) because they're needed at JVM/app **bootstrap**, before
Consul config is even loaded (`spring.config.import:
consul:${CONSUL_HOST:localhost}:${CONSUL_PORT:8500}` in each service's local
`application.yaml`).

## New Python AI microservice — `esprit-microservices/ai-service/`

Directory layout:
```
ai-service/
  Dockerfile
  requirements.txt
  app/
    __init__.py
    main.py               # FastAPI app, lifespan hook for consul register/deregister, POST /api/ai/sentiment, GET /api/ai/health
    config.py             # env-driven config: SERVICE_NAME/PORT/HOST, CONSUL_HOST/PORT, KEYCLOAK_ISSUER_URI, KEYCLOAK_JWKS_URI
    consul_registration.py # PUT /v1/agent/service/register on startup, deregister on shutdown, HTTP health check
    security.py            # FastAPI dependency: validates Keycloak JWT via python-jose, fetches JWKS from KEYCLOAK_JWKS_URI (cached 5 min), issuer checked as plain string compare
    sentiment.py           # Real ML: HuggingFace transformers pipeline, model "distilbert-base-uncased-finetuned-sst-2-english", CPU/PyTorch
```

Key implementation details:
- **Real ML, not a lexicon**: uses
  `transformers.pipeline("sentiment-analysis", model="distilbert-base-uncased-finetuned-sst-2-english", framework="pt")`.
  The model is baked into the Docker image at build time (a `RUN python -c
  "from transformers import pipeline; pipeline(...)"` line in the
  Dockerfile) so there's no cold-start download and no internet dependency
  at runtime. Response includes `label` (POSITIVE/NEUTRAL/NEGATIVE — model
  is binary POSITIVE/NEGATIVE, so confidence < 0.6 is remapped to NEUTRAL)
  and `scores: {model_label, confidence}`.
- **Consul registration**: on FastAPI startup (`lifespan` context manager),
  PUTs to `http://{CONSUL_HOST}:{CONSUL_PORT}/v1/agent/service/register`
  with `Address={SERVICE_HOST}` (the Docker service name `ai-service` in
  compose), `Port=8090`, and an HTTP health check hitting
  `http://{SERVICE_HOST}:8090/api/ai/health` every 10s. Deregisters on
  shutdown. This lets the gateway's `lb://ai-service` route discover it
  exactly like the Java services.
- **requirements.txt** uses `--extra-index-url
  https://download.pytorch.org/whl/cpu` as the first line so `torch` installs
  the CPU-only build (much smaller, no CUDA needed for this lightweight use
  case).
- **Dockerfile CMD**: `uvicorn app.main:app --host 0.0.0.0 --port 8090 --http httptools`
  — the `--http httptools` flag is important, see Known Issue below.
- Built and tagged as `esprit-ai-service:latest` (manually, matching the
  "no build: in compose" rule):
  `docker build -f ai-service/Dockerfile -t esprit-ai-service:latest ai-service`

## Frontend changes — `esprit-microservices-front/`

- **`Dockerfile`** (new file, repo root of the frontend project):
  ```dockerfile
  FROM node:22-alpine
  WORKDIR /workspace
  ENV CI=true
  RUN corepack enable && corepack prepare pnpm@11.0.0 --activate
  COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
  RUN pnpm install --frozen-lockfile
  COPY . .
  EXPOSE 4200
  ENTRYPOINT ["npx", "ng", "serve", "--host", "0.0.0.0", "--port", "4200"]
  ```
  Two bugs were hit and fixed along the way, both worth knowing about if
  touching this file again:
  1. Without `ENV CI=true`, pnpm's dependency-status-check tries to
     interactively confirm purging `node_modules` and fails with
     `ERR_PNPM_ABORTED_REMOVE_MODULES_DIR_NO_TTY` (no TTY in a container).
  2. The entrypoint was originally `["pnpm", "start", "--", "--host",
     "0.0.0.0", "--port", "4200"]` — this **double-inserts `--`** because
     pnpm's own arg-forwarding already adds one when running the `start`
     script (`ng serve`), producing `ng serve -- --host ... -- --port ...`
     effectively, which fails Angular CLI's schema validation
     ("Option '--' has been specified multiple times" /
     "Schema validation failed... must NOT have additional properties").
     Fixed by calling `ng serve` directly via `npx` instead of through the
     `pnpm start` wrapper.
  - **Do not use nginx or a production static build for this Dockerfile —
    the user explicitly and strongly rejected that approach.**
- **New feature**: `src/app/features/ai/`
  - `service/ai.service.ts` — `AiService`, `analyzeSentiment(text)` POSTs to
    `${environment.apiUrl}/api/ai/sentiment`, returns
    `SentimentResult { label, scores: { model_label, confidence } }`
  - `pages/ai-insights.ts` / `.html` — `AiInsights` component: textarea +
    "Analyze sentiment" button, shows a badge (color varies by label) and
    confidence score, error message on failure. Uses spartan-ng/hlm
    components (`HlmBadgeImports`, `HlmButtonImports`, `HlmCardImports`,
    `HlmTextareaImports`), Angular signals, `ChangeDetectionStrategy.OnPush`
    — matches this codebase's existing conventions (see
    `features/menu/pages/menu-management.ts` as the reference pattern that
    was used).
  - `routes.ts` — lazy-loaded route, `loadComponent: () => import('./pages/ai-insights')`
  - Registered in `src/app/app.routes.ts` under the `MainLayout` children as
    path `ai-insights`.
  - Registered in `src/app/layout/app/components/navigation/navigation.ts`
    nav items list: `{ title: 'AI Insights', key: 'ai-insights', url:
    '/ai-insights', icon: 'lucideBot' }` — reused the already-imported-but-
    unused `lucideBot` icon.
  - `npx tsc --noEmit -p tsconfig.app.json` was run and passed with no
    errors after these additions (this project's Angular setup does
    strict template type checking as part of `tsc`, so this also validates
    template bindings like the badge `variant` union type).
- Built and tagged as `esprit-microservices-frontend:latest`:
  `cd esprit-microservices-front && docker build -t esprit-microservices-frontend:latest .`

## `esprit-microservices/compose.yaml` — final shape

All 5 backend Dockerfiles **already existed** in the repo before this
session (`gateway/Dockerfile`, `reservation/Dockerfile`,
`employee-management/Dockerfile`, `menu-management/Dockerfile`,
`delivery-management/Dockerfile` — multi-stage Maven reactor builds, build
context must be the repo root because it's a multi-module Maven project;
each copies all sibling `pom.xml` files before copying only its own
module's `src/`). These were NOT modified.

The app-services section of `compose.yaml` now looks like (paraphrased,
see the actual file for exact current state):

```yaml
services:
  # ... infra services (consul, rabbitmq, keycloak-postgres, keycloak,
  #     mail-dev, app-postgres, menu-mysql, consul-kv-init) unchanged
  #     except keycloak got KC_HOSTNAME: http://localhost:9000 added ...

  gateway:
    image: esprit-microservices-gateway:latest
    container_name: gateway-service
    ports: ["8080:8080"]
    environment:
      CONSUL_HOST: consul
      CONSUL_PORT: "8500"
      KEYCLOAK_ISSUER_URI: http://localhost:9000/realms/esprit
      KEYCLOAK_JWK_SET_URI: http://keycloak:8080/realms/esprit/protocol/openid-connect/certs
    depends_on: [consul (healthy), keycloak (healthy), consul-kv-init (completed)]
    restart: unless-stopped

  reservation:
    image: esprit-microservices-reservation:latest
    # ... DB_HOST=app-postgres, DB_PORT=5432, RABBITMQ_HOST=rabbitmq, KEYCLOAK_URL=http://keycloak:8080

  employee-management:
    image: esprit-microservices-employee-management:latest
    # ... DB_HOST/PORT, RABBITMQ_HOST, KEYCLOAK_ISSUER_URI, KEYCLOAK_JWK_SET_URI, KEYCLOAK_SERVER_URL

  menu-management:
    image: esprit-microservices-menu-management:latest
    # ... DB_HOST=menu-mysql, DB_PORT=3306, RABBITMQ_HOST=rabbitmq

  delivery-management:
    image: esprit-microservices-delivery-management:latest
    # ... DB_HOST/PORT, KEYCLOAK_ISSUER_URI, KEYCLOAK_JWK_SET_URI, KEYCLOAK_SERVER_URL

  ai-service:
    image: esprit-ai-service:latest
    container_name: ai-service
    environment:
      CONSUL_HOST: consul
      CONSUL_PORT: "8500"
      SERVICE_HOST: ai-service
      KEYCLOAK_ISSUER_URI: http://localhost:9000/realms/esprit
      KEYCLOAK_JWKS_URI: http://keycloak:8080/realms/esprit/protocol/openid-connect/certs
    depends_on: [consul (healthy), keycloak (healthy), consul-kv-init (completed)]
    restart: unless-stopped

  frontend:
    image: esprit-microservices-frontend:latest
    container_name: esprit-frontend
    ports: ["4200:4200"]
    depends_on: [gateway]
    restart: unless-stopped

volumes:
  consul_data:
  keycloak_postgres_data:
  app_postgres_data:
  menu_mysql_data:
```

**No `build:` key anywhere** in the app services — this is intentional per
the user's explicit instruction. A comment block above the app-services
section in the actual file documents the rebuild workflow for contributors.

## How to build all images (manual, not via compose)

```bash
cd esprit-microservices
docker build -f gateway/Dockerfile -t esprit-microservices-gateway:latest .
docker build -f reservation/Dockerfile -t esprit-microservices-reservation:latest .
docker build -f employee-management/Dockerfile -t esprit-microservices-employee-management:latest .
docker build -f menu-management/Dockerfile -t esprit-microservices-menu-management:latest .
docker build -f delivery-management/Dockerfile -t esprit-microservices-delivery-management:latest .
docker build -f ai-service/Dockerfile -t esprit-ai-service:latest ai-service
cd ../esprit-microservices-front
docker build -t esprit-microservices-frontend:latest .
```

Then from `esprit-microservices/`: `docker compose up -d`

## Known/verified-working checks

- `docker ps` — all 13 containers reach `Up` (infra ones `healthy`).
- Consul catalog: `curl http://localhost:8500/v1/catalog/service/ai-service`
  returns a registered instance with `ServiceAddress: "ai-service"`,
  `ServicePort: 8090`.
- Decoding a real Keycloak token's `iss` claim (client_credentials grant,
  client `esprit-backend`, secret is in
  `keycloak/kc-realm/esprit-realm.json` → `clients[].secret` for
  `clientId: "esprit-backend"`, or hardcoded as
  `1f8w9PSmXaS1JF99qFSyGWcC501iVYJR` in `config/gateway/data`'s springdoc
  block **but that hardcoded value did NOT match the actual realm-configured
  secret** — always read the real one from `esprit-realm.json` if testing)
  gives exactly `http://localhost:9000/realms/esprit`.
- ai-service logs show a successful JWKS fetch:
  `INFO:httpx:HTTP Request: GET http://keycloak:8080/realms/esprit/protocol/openid-connect/certs "HTTP/1.1 200 OK"`
- Calling `POST http://localhost:8080/api/ai/sentiment` **without** a
  token → `401` (gateway blocks it, as expected).
- Calling `GET http://localhost:8090/api/ai/health` directly on the Docker
  network (e.g. via `docker run --rm --network esprit-microservices_default
  curlimages/curl ...`) → `200 OK`.
- Frontend container serves cleanly on `http://localhost:4200/` (`ng serve`,
  watch mode, "Application bundle generation complete").

## ⚠️ KNOWN UNRESOLVED ISSUE — needs a fresh pair of eyes

**Symptom**: `POST http://localhost:8080/api/ai/sentiment` (through the
gateway, WITH a valid bearer token) intermittently fails:
- Sometimes: `HTTP 422` with body
  `{"detail":[{"type":"missing","loc":["body"],"msg":"Field required","input":null}]}`
  (FastAPI/Pydantic says the JSON body arrived empty/null).
- Sometimes: `HTTP 400` with body `Invalid HTTP request received.` (this is
  a raw uvicorn/h11-level protocol rejection, seen as a `WARNING: Invalid
  HTTP request received.` line in `docker logs ai-service`, happening
  *before* the request even reaches FastAPI application code).

**What's confirmed NOT the cause** (all tried, none fully fixed it):
1. The `CircuitBreaker` gateway filter — removed from the `ai-service`
   route entirely, issue persisted.
2. Missing `RewritePath` filter — added an identity-mapping `RewritePath`
   (`/api/ai/(?<segment>.*)` → `/api/ai/${segment}`) to match the shape of
   other working routes, issue persisted.
3. uvicorn's default HTTP parser (h11) being too strict — switched to
   `--http httptools` (more lenient parser), issue persisted (though the
   *type* of failure may have shifted between the 400 and 422 forms across
   different attempts, suggesting a request-to-request inconsistency, not a
   deterministic single cause).

**What's confirmed working fine**:
- `GET` requests through the gateway to ai-service (e.g. health checks via
  Consul, from *within* the Docker network) work fine.
- Direct POST to `ai-service:8090/api/ai/sentiment` (bypassing the gateway
  entirely, via `docker run --rm --network esprit-microservices_default
  curlimages/curl ... -X POST http://ai-service:8090/api/ai/sentiment -d
  '{"text":"..."}'`) correctly reaches FastAPI's auth check (401 without a
  valid token) — meaning direct POST-with-body works, so the bug is
  specific to **gateway-proxied** POST-with-body to this specific Python
  backend.
- Auth/JWKS validation itself is proven correct (see verified-working list
  above) — this is purely a body/framing forwarding issue, not a security
  issue.

**Leading hypothesis** (unconfirmed): Spring Cloud Gateway Server WebMvc
(the newer synchronous, non-reactive Gateway flavor used by this project's
`gateway` module — see
`gateway/pom.xml` dependency `spring-cloud-starter-gateway-server-webmvc`)
may have a request-body-forwarding quirk when proxying to a non-Spring/
non-Tomcat backend, possibly related to connection reuse/keep-alive pooling
in its underlying HTTP client (likely the JDK `HttpClient` via
`RestClient`), producing malformed framing (chunked encoding issues,
`Expect: 100-continue` handling, or a race in connection pooling) that
Tomcat (used by all the other, Java, backend services) tolerates
leniently but that a stricter/different HTTP server implementation does
not always tolerate the same way.

**Suggested next debugging steps for a fresh session**:
1. Capture the literal bytes the gateway sends to `ai-service` — e.g. run
   a raw `nc -l -p 8090` or a minimal Python `socketserver` listener in
   place of the real ai-service temporarily, and log the raw request bytes
   received when hit via the gateway vs. via direct curl, to see exactly
   what differs (headers, `Transfer-Encoding` vs `Content-Length`,
   `Expect: 100-continue`, connection reuse behavior).
2. Try configuring Spring Cloud Gateway's HTTP client explicitly (e.g.
   `spring.cloud.gateway.server.webmvc.http-client.*` properties, or
   switching its underlying client from the default to Apache HttpClient 5
   if that's configurable) to see if a different client implementation
   produces cleaner framing.
3. Try disabling HTTP keep-alive / connection pooling on the gateway's
   outbound calls to `ai-service` specifically, to test the "stale/reused
   connection" theory.
4. As a workaround (not a real fix, but unblocks the user if nothing else
   works quickly): have `ai-service` accept the request body via query
   string or a differently-encoded content type as a temporary measure, or
   put a tiny reverse proxy (e.g. a minimal Caddy/nginx sidecar — though the
   user dislikes nginx for the *frontend*, a transparent internal sidecar
   for just this one backend route might be acceptable if framed as "fixing
   a compatibility bug", not "the frontend serving approach") in front of
   `ai-service` to normalize the HTTP framing before it reaches uvicorn.
5. Re-test whether an *existing* Java route (e.g. `POST /api/employees`
   with a full valid JSON body matching the DTO, actually hitting a 2xx or
   a real validation 400 from the Java service) truly forwards bodies
   correctly through this gateway in the current bridge-networked setup —
   this was never fully proven in this session (both attempts returned
   `405 Method Not Allowed` before the body would have mattered, which is
   inconclusive). If it turns out Java routes are affected too, this is a
   **general gateway bug**, not something specific to Python/ai-service,
   and the fix priority should shift accordingly.

## Task list state at end of session (all marked done except the open issue above)

1. ✅ Write frontend Dockerfile + nginx.conf → later revised to `ng serve`,
   nginx.conf deleted, not used.
2. ✅ Add app services to compose.yaml
3. ✅ Build all Docker images
4. ✅ Run full stack via docker compose up -d
5. ✅ Verify app is reachable end-to-end (frontend, gateway, infra all
   confirmed up; the ai-service POST issue above is the one gap)
6. ✅ Create Python AI microservice
7. ✅ Wire ai-service into gateway route + consul-kv.json
8. ✅ Build ai-service Docker image
9. ✅ Add frontend AI feature page
10. ✅ Run full stack and test ai-service end-to-end (mostly — POST-via-
    gateway issue open)

## Memory files saved (for the Claude Code memory system, if continuing in
the same tool/session — not relevant if you're a different LLM/tool, but
listed for completeness)

- `project_dockerize_microservices.md` — the original host-networking plan
  (now superseded by bridge networking + Keycloak decoupling — read this
  file's content with that in mind, it documents the *reasoning* that led
  here even though the final networking choice changed)
- `feedback_no_nginx_dev_server.md` — the nginx rejection preference,
  still valid and permanent.

## Tone/working-style notes about this user (for whoever continues this)

- Dictates by voice-to-text; messages often have typos, filler words,
  run-on phrasing, and repeated words. Read for intent, not literal text.
- Wants things actually done, running, and tested — not just planned or
  described. Gets frustrated by anything that looks like a shortcut,
  placeholder, or "fake" implementation (see the vaderSentiment → DistilBERT
  incident).
- Appreciates directness about problems — do not hide or downplay the open
  gateway issue; they'd rather know than be told everything is perfect when
  it isn't.
- Prefers heavy use of task tracking/memory so nothing gets lost across a
  long multi-hour session.
- Wants Docker image builds kept manual/explicit, decoupled from
  `docker compose up`, so iteration doesn't force unwanted rebuilds.
