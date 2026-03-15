# Task #4 Plan: Add Dockerfile (Java 21, Spring Boot)

## Goal
Containerize the UCMS backend so it can be deployed to Railway using a
Dockerfile-based build. Also configure the Railway platform health check
(covers task #5).

## Assumptions
- Target platform: **Railway**
- Base image: **eclipse-temurin:21-alpine** (smaller image, ~200 MB final)
- Tests are already run in CI (`backend-ci.yml`); Docker build skips them
- `application-prod.yaml` is already configured; activated via
  `SPRING_PROFILES_ACTIVE=p rod` set in the Railway dashboard
- Railway overrides the port at runtime via the `PORT` env var, which
  `application-prod.yaml` already reads as `${PORT:8080}`

## Files to Create

| File | Description |
|---|---|
| `Dockerfile` | Multi-stage build: build with JDK → run with JRE |
| `.dockerignore` | Exclude secrets, target/, docs, IDE files from build context |
| `railway.toml` | Pin Railway to Dockerfile builder; configure health check path |

No existing files are modified.

---

## Task Breakdown

### 1. `Dockerfile`

Multi-stage build using Eclipse Temurin 21 Alpine.

**Stage 1 — Build**
- Base: `eclipse-temurin:21-jdk-alpine`
- Copy Maven wrapper + `pom.xml`, run `dependency:go-offline` (layer-cached)
- Copy `src/`, run `mvnw package -DskipTests`

**Stage 2 — Runtime**
- Base: `eclipse-temurin:21-jre-alpine`
- Copy only the fat JAR from the build stage
- `EXPOSE 8080`
- `ENTRYPOINT ["java", "-jar", "app.jar"]`

**Done criteria:** `docker build -t ucms-backend .` succeeds and the
container starts when all required env vars are provided.

---

### 2. `.dockerignore`

Exclude from build context:
- `.git/`, `.github/`
- `target/`
- `src/main/resources/application.yaml` (live secrets — must never enter image)
- `src/main/resources/application.yaml.example`
- `docs/`, `scripts/`, `*.md`
- IDE metadata: `.settings/`, `.classpath`, `.factorypath`, `.project`

**Done criteria:** Build context is small; no credentials or docs are
copied into the image layer.

---

### 3. `railway.toml`

```toml
[build]
  builder = "DOCKERFILE"

[deploy]
  healthcheckPath = "/api/health"
  healthcheckTimeout = 300
  restartPolicyType = "ON_FAILURE"
  restartPolicyMaxRetries = 3
```

**Done criteria:** Railway uses the Dockerfile explicitly and polls
`/api/health` for liveness (also satisfies task #5 — "Configure platform
health check path to /api/health").

---

## Local Verification Steps

```bash
# Build the image
docker build -t ucms-backend .

# Run with prod profile and required env vars
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=<your-value> \
  -e SPRING_DATASOURCE_USERNAME=<your-value> \
  -e SPRING_DATASOURCE_PASSWORD=<your-value> \
  -e SUPABASE_URL=<your-value> \
  -e SUPABASE_ANON_KEY=<your-value> \
  -e SUPABASE_SERVICE_ROLE_KEY=<your-value> \
  -e SUPABASE_JWKS_URI=<your-value> \
  -e SUPABASE_JWT_ISSUER=<your-value> \
  ucms-backend

# Smoke test
curl http://localhost:8080/api/health
# Expected: 200 { "status": "UP", "timestamp": "..." }
```

---

## Risks / Edge Cases

| Risk | Mitigation |
|---|---|
| `application.yaml` baked into image | Listed in `.dockerignore`; prod config is env-var driven via `application-prod.yaml` |
| Alpine musl libc incompatibility with Apache Tika (JNI) | Tika-core 2.9.2 is pure Java — no JNI; alpine is safe |
| Maven wrapper not executable in container | `COPY mvnw` + `RUN chmod +x mvnw` before `./mvnw` |
| Railway PORT env var not honoured | `application-prod.yaml` already maps `server.port: ${PORT:8080}` |
| Large build context slows CI | `.dockerignore` removes `target/`, docs, `.git` |
| Flyway migrations on first deploy | Already configured in `application-prod.yaml`; runs automatically on startup |
