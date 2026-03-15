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
