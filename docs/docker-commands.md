# Docker Commands — UCMS Backend

## Build

```bash
# Standard build (uses layer cache)
docker build -t ucms-backend .

# Force full rebuild (use after changing YAML configs or pom.xml)
docker build --no-cache -t ucms-backend .
```

---

## Run

```bash
# Foreground (logs printed to terminal, Ctrl+C to stop)
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres?prepareThreshold=0" \
  -e SPRING_DATASOURCE_USERNAME="postgres.icosjzuwekgilmmxgqwr" \
  -e SPRING_DATASOURCE_PASSWORD='G9G!VqZWT&%t5bD' \
  -e SUPABASE_URL="https://icosjzuwekgilmmxgqwr.supabase.co" \
  -e SUPABASE_ANON_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imljb3NqenV3ZWtnaWxtbXhncXdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMDkyNTgsImV4cCI6MjA4NzU4NTI1OH0.wXKERkACJyoudEGMpumz83zAiIg9dMoJuC5xbbhriJA" \
  -e SUPABASE_SERVICE_ROLE_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imljb3NqenV3ZWtnaWxtbXhncXdyIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MjAwOTI1OCwiZXhwIjoyMDg3NTg1MjU4fQ.QfoeunzYo-xaGlJl-Z3_puo7QkbwBsEJOGtA4XMLOpE" \
  -e SUPABASE_JWKS_URI="https://icosjzuwekgilmmxgqwr.supabase.co/auth/v1/.well-known/jwks.json" \
  -e SUPABASE_JWT_ISSUER="https://icosjzuwekgilmmxgqwr.supabase.co/auth/v1" \
  -e MAIL_USERNAME="ucms.ucc@gmail.com" \
  -e MAIL_PASSWORD="qrioqaimxbggzlbl" \
  ucms-backend

# Detached / background (terminal stays free)
docker run --rm -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:6543/postgres?prepareThreshold=0" \
  -e SPRING_DATASOURCE_USERNAME="postgres.icosjzuwekgilmmxgqwr" \
  -e SPRING_DATASOURCE_PASSWORD='G9G!VqZWT&%t5bD' \
  -e SUPABASE_URL="https://icosjzuwekgilmmxgqwr.supabase.co" \
  -e SUPABASE_ANON_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imljb3NqenV3ZWtnaWxtbXhncXdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMDkyNTgsImV4cCI6MjA4NzU4NTI1OH0.wXKERkACJyoudEGMpumz83zAiIg9dMoJuC5xbbhriJA" \
  -e SUPABASE_SERVICE_ROLE_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imljb3NqenV3ZWtnaWxtbXhncXdyIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MjAwOTI1OCwiZXhwIjoyMDg3NTg1MjU4fQ.QfoeunzYo-xaGlJl-Z3_puo7QkbwBsEJOGtA4XMLOpE" \
  -e SUPABASE_JWKS_URI="https://icosjzuwekgilmmxgqwr.supabase.co/auth/v1/.well-known/jwks.json" \
  -e SUPABASE_JWT_ISSUER="https://icosjzuwekgilmmxgqwr.supabase.co/auth/v1" \
  -e MAIL_USERNAME="ucms.ucc@gmail.com" \
  -e MAIL_PASSWORD="qrioqaimxbggzlbl" \
  ucms-backend
```

---

## Check

```bash
# List all running containers
docker ps

# List only ucms-backend containers (running)
docker ps --filter ancestor=ucms-backend

# List all containers including stopped ones
docker ps -a

# Show container ID only (useful for scripting)
docker ps --filter ancestor=ucms-backend --format "{{.ID}}"

# Show ID, status, and port
docker ps --filter ancestor=ucms-backend --format "table {{.ID}}\t{{.Status}}\t{{.Ports}}"
```

---

## Logs

```bash
# Print logs of a running container (replace <ID> with actual container ID from docker ps)
docker logs <ID>

# Follow logs in real time
docker logs -f <ID>

# Last 50 lines only
docker logs --tail 50 <ID>

# Follow logs for the ucms-backend container (one-liner)
docker logs -f $(docker ps --filter ancestor=ucms-backend --format "{{.ID}}")
```

---

## Stop

```bash
# Stop by container ID
docker stop <ID>

# Stop all running ucms-backend containers
docker ps --filter ancestor=ucms-backend --format "{{.ID}}" | xargs -r docker stop

# Stop all running containers (any image)
docker stop $(docker ps -q)
```

---

## Smoke Test

```bash
# Health check
curl http://localhost:8080/api/health

# Expected response:
# {"status":"UP","timestamp":"..."}
```

---

## Docker Hub — Initial Push

> Gawin ito **isang beses** para ma-upload ang image sa Docker Hub.

```bash
# 1. Login sa Docker Hub (ilagay ang username at password kapag hiniling)
docker login

# 2. I-tag ang existing local image para sa Docker Hub
docker tag ucms-backend:latest kenz2025/ucms-backend:latest

# 3. I-push sa Docker Hub
docker push kenz2025/ucms-backend:latest
```

I-verify sa browser:
```
https://hub.docker.com/r/kenz2025/ucms-backend
```

---

## Docker Hub — Pag Mag-update ng Image

### A. Nag-bago ang source code (kailangan i-rebuild)

```bash
# 1. I-rebuild ang image mula sa source
docker build -t ucms-backend:latest .

# 2. I-tag muli para sa Docker Hub
docker tag ucms-backend:latest kenz2025/ucms-backend:latest

# 3. I-push ang updated image
docker push kenz2025/ucms-backend:latest
```

---

### B. Gusto ng versioning (e.g. v1.0.0, v1.1.0)

```bash
# I-tag ng specific version
docker tag ucms-backend:latest kenz2025/ucms-backend:v1.0.0

# I-push ang specific version
docker push kenz2025/ucms-backend:v1.0.0

# I-update din ang latest tag
docker tag ucms-backend:latest kenz2025/ucms-backend:latest
docker push kenz2025/ucms-backend:latest
```

> **Tip:** Palaging i-push ang `latest` kasabay ng specific version para ang mga pull na walang version tag ay makuha ang pinakabago.

---

## Cleanup

```bash
# Remove the ucms-backend image
docker rmi ucms-backend

# Remove all stopped containers
docker container prune

# Remove unused images
docker image prune

# Nuclear option — remove everything (containers, images, volumes, networks)
docker system prune -a
```
