# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

UCMS Backend is a REST API for a University Concern Management System — a ticket submission, routing, and resolution platform for students, staff, and admins. Built with Spring Boot 4.0.3 / Java 21, backed by Supabase (PostgreSQL + Auth + Storage).

## Commands

```bash
# Run locally (port 8080)
./mvnw spring-boot:run

# Build JAR
./mvnw clean package

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=TicketServiceTest

# Run a specific test method
./mvnw test -Dtest=TicketServiceTest#testCreateTicket
```

No dedicated linting tool. Code style follows the Google Java Style Guide (no wildcard imports).

## Architecture

Strict 3-tier layered architecture: **Controller → Service → Repository**

- **No business logic in controllers** — controllers only parse requests and delegate to services.
- **Ownership and role checks belong exclusively in the service layer.**
- **All responses use `ApiResponse<T>` wrapper.**

### Key Service Responsibilities

| Service | Responsibility |
|---|---|
| `TicketService` | Ticket CRUD, status transitions, ownership enforcement |
| `SupabaseAuthService` | JWT validation and token refresh via Supabase |
| `SupabaseStorageService` | File upload/download, signed URL generation |
| `NotificationService` | Email notifications triggered by ticket events |
| `TicketUrgencyScoringService` | AI urgency scoring via Gemini API |
| `RealtimeSseService` | Server-sent events for live ticket updates |
| `AnalyticsService` | Reports and statistics |

### Security

Authentication uses Supabase JWTs validated as OAuth2 resource server tokens. The `SupabaseAuthFilter` extracts roles and sets the Spring Security context. RBAC is enforced in the service layer — see `docs/roles-permissions.md` for the full matrix.

### Database

PostgreSQL (Supabase-managed). Schema is managed entirely by **Flyway** — `spring.jpa.hibernate.ddl-auto` is set to `none`. Migration files live in `src/main/resources/db/migration/` using the `V{n}__{description}.sql` naming convention. Never use Hibernate to auto-generate or modify schema.

### Rate Limiting

Bucket4j is used for API rate limiting. Configuration lives in `SecurityConfig`.

## Configuration

Copy `src/main/resources/application.yaml.example` to `application.yaml` for local dev. Required keys:

- `SPRING_DATASOURCE_URL` / `USERNAME` / `PASSWORD` — PostgreSQL connection
- `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY` — Supabase project
- `SUPABASE_JWKS_URI`, `SUPABASE_JWT_ISSUER` — JWT validation
- `GEMINI_API_KEY`, `GEMINI_MODEL` — AI urgency scoring (optional)
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` — SMTP

Spring profiles: `dev` (default, verbose SQL logging) and `prod` (env vars, connection pooling).

## Testing

Tests live in `src/test/java/com/ucms_backend/`. Service tests use `@ExtendWith(MockitoExtension.class)` with `@Mock` / `@InjectMocks`. Tests that involve authorization manually set up `SecurityContextHolder`.

CI runs `./mvnw test` on every PR to `development` via GitHub Actions.

## Branching

Always branch off `development`. Never work directly on `main`.

## Key Reference Docs

- `docs/ticket-status-flow.md` — Valid ticket status transitions (state machine)
- `docs/roles-permissions.md` — RBAC matrix
- `docs/api-contract.md` — Full endpoint contracts
- `docs/data-models.md` — ERD and schema decisions
- `docs/error-codes.md` — Error codes and messages
