# UCMS System Architecture

```
┌─────────────────────────────┐
│        Android App          │
│     (Java + XML)            │
│                             │
│  Retrofit → REST calls      │
│  JWT stored locally         │
└────────────┬────────────────┘
             │ HTTPS / REST API
             ▼
┌─────────────────────────────┐
│     Spring Boot Backend     │
│  Spring Boot 4.0.3 (Java 21)│
│                             │
│  - Auth proxy → Supabase    │
│  - JWT verification (JWKS)  │
│  - Ticket management        │
│  - Role: Student / Admin    │
│  - Report generation        │
└────┬──────────────┬─────────┘
     │              │
     ▼              ▼
┌─────────────────┐   ┌──────────────────┐
│ Supabase Postgres│   │ Supabase Storage │
│ (managed DB)     │   │ (attachments)    │
└─────────────────┘   └──────────────────┘

All backend hosted on Railway / Render (free tier)
```

## Flow Summary

- Android app sends requests via Retrofit over HTTPS
- Spring Boot proxies auth flows to Supabase Auth (Option B)
- Spring Boot validates Supabase JWT on every request (JWKS)
- Data stored in Supabase Postgres
- Attachments stored in Supabase Storage; backend returns signed URLs for viewing
- "Report generation" refers to the analytics endpoints only — no file export (CSV/PDF) is planned

---

## Backend Architecture — Layered Pattern

The backend follows a **Layered Architecture** (Controller → Service → Repository).

```
HTTP Request
     │
     ▼
┌─────────────┐
│  Controller │  → Handles HTTP, maps request/response, delegates to service
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Service   │  → Business logic, role enforcement, ownership checks
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Repository  │  → JPA data access (DB queries only)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Database   │  → Supabase Postgres
└─────────────┘
```

### Package Structure

```
com.ucms_backend/
├── controller/      → HTTP layer, routes, request/response mapping
├── service/         → Business logic, ownership checks, role enforcement
├── repository/      → JPA repositories (DB access only)
├── model/entity/    → JPA entities mapped to DB tables
├── dto/             → Request/response DTOs
├── config/          → SecurityConfig, beans, filters
└── exception/       → @ControllerAdvice, custom exceptions
```

### Rules
- No business logic in controllers — delegate to service layer
- Ownership + role checks in service layer only
- `@ControllerAdvice` for all exception handling — no try/catch in controllers
- All REST responses use `ApiResponse<T>`: `{ "status", "message", "data" }`
- DTOs used for all request/response — never expose entities directly
