## Overview
Deploy the UCMS backend to Railway or Render (free tier). Configure all required environment variables, add a health check endpoint, and document the deployment process. The backend must connect to the shared Supabase project in production.

## Tasks
- [ ] Add GET /api/health endpoint — returns 200 { status: UP, timestamp } — no auth required; used by platform health checks
- [ ] Create application-prod.yaml profile — production-safe config (no debug logging, ddl-auto=none, connection pool tuned)
- [ ] Document all required environment variables in README.md under a Deployment section: SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD, SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY, SUPABASE_JWKS_URI, SUPABASE_JWT_ISSUER, SUPABASE_STORAGE_SIGNED_URL_EXPIRY_SECONDS
- [ ] Add Dockerfile (or verify Railway/Render native Java 21 buildpack works)
- [ ] Configure platform health check path to /api/health
- [ ] Set SPRING_PROFILES_ACTIVE=prod in platform environment
- [ ] Verify Flyway migrations run automatically on startup in production
- [ ] Smoke test all critical endpoints after deployment (auth, ticket create, ticket list)
- [ ] Update README.md with deployment steps and live base URL

## Acceptance Criteria
- GET /api/health returns 200 with no auth required
- Backend starts successfully on Railway/Render with all env vars set
- Flyway migrations run cleanly on first deploy
- All GitHub Secrets are set in repo settings for CI
- Smoke test passes: register, login, create ticket, list tickets
- README.md documents deployment steps and environment variables

## References
- docs/migration-runbook.md — required env vars
- src/main/resources/application.yaml.example
- .github/workflows/ — CI config
- README.md