# University Concern Management System (UCMS) — Backend

REST API backend for the University Concern Management System, handling ticket submission, routing, and resolution for students, staff, and admins.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.3 |
| Language | Java 21 |
| Database | Supabase (PostgreSQL) |
| Auth / Storage | Supabase Auth + Supabase Storage |
| Migrations | Flyway |
| Boilerplate reduction | Lombok |
| Security | Spring Security + JWT (Supabase tokens) |

---

## 📚 Documentation

All detailed docs live in [`docs/`](docs/).

| File | Description |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | System architecture and layered design pattern |
| [`docs/api-contract.md`](docs/api-contract.md) | All API endpoints, request/response shapes |
| [`docs/data-models.md`](docs/data-models.md) | ERD and data model decisions |
| [`docs/roles-permissions.md`](docs/roles-permissions.md) | Roles, permissions matrix, and enforcement rules |
| [`docs/ticket-status-flow.md`](docs/ticket-status-flow.md) | Ticket state diagram and valid transitions |
| [`docs/migration-runbook.md`](docs/migration-runbook.md) | DB migrations and full team setup guide |
| [`docs/postman/UCMS-Full.postman_collection.json`](docs/postman/UCMS-Full.postman_collection.json) | Postman collection for all endpoints |

Also see:
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — branch naming, commit messages, PR template, code style

---

## 🚀 Quick Start

### Prerequisites

- **Java 21**
- **Maven 3.6+**

### Steps

```bash
# 1. Clone the repo and navigate to the backend
git clone <repo-url>
cd ucms/ucms-backend

# 2. Make the Maven wrapper executable
chmod +x mvnw

# 3. Copy the config template
cp src/main/resources/application.yaml.example src/main/resources/application.yaml

# 4. Fill in your credentials
#    Open application.yaml and set your Supabase URL, credentials, and JWT secret.
#    See docs/migration-runbook.md for the full setup guide.

# 5. Run the application
./mvnw spring-boot:run
```

**Expected output:** Look for `Tomcat started on port 8080` in the console.

> **Note:** Never commit `application.yaml` with real credentials. It is gitignored. Use `application.yaml.example` as the source of truth for config keys.

---

## 🚢 Deployment

**Live base URL:** `https://ucms-backend-latest.onrender.com`

### Deployment Steps (Render via Docker Hub)

The CI/CD pipeline automatically builds and pushes the Docker image to [`kenz2025/ucms-backend`](https://hub.docker.com/r/kenz2025/ucms-backend) on every push to `main` or `development`.

1. In Render, create a new **Web Service** and choose **Deploy an existing image from a registry**
2. Set the image URL to `docker.io/kenz2025/ucms-backend:latest`
3. Set **Health Check Path** to `/api/health`
4. Add all required environment variables (see table below)
5. Deploy — Flyway migrations run automatically on startup
6. Confirm deployment: `GET https://ucms-backend-latest.onrender.com/api/health` should return `200 { "status": "UP" }`

**Required GitHub Secrets** (for the Docker publish workflow):

| Secret | Description |
|---|---|
| `DOCKERHUB_USERNAME` | Your Docker Hub username (`kenz2025`) |
| `DOCKERHUB_TOKEN` | Docker Hub access token (generate at hub.docker.com → Account Settings → Security) |

Set these environment variables in your deployment platform (Railway/Render):

| Variable | Required | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Yes | JDBC URL for the production PostgreSQL connection |
| `SPRING_DATASOURCE_USERNAME` | Yes | Database username for the production connection |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database password for the production connection |
| `SUPABASE_URL` | Yes | Supabase project URL (for example `https://<project-ref>.supabase.co`) |
| `SUPABASE_ANON_KEY` | Yes | Supabase anon/public API key |
| `SUPABASE_SERVICE_ROLE_KEY` | Yes | Supabase service role key (secret, backend only) |
| `SUPABASE_JWKS_URI` | Yes | Supabase JWKS endpoint (`<supabase-url>/auth/v1/.well-known/jwks.json`) |
| `SUPABASE_JWT_ISSUER` | Yes | Expected JWT issuer (`<supabase-url>/auth/v1`) |
| `SUPABASE_STORAGE_SIGNED_URL_EXPIRY_SECONDS` | Yes | Signed URL lifetime in seconds for private storage access |

Also set the Spring profile in the platform environment:

- `SPRING_PROFILES_ACTIVE=prod`

### Verify Flyway on first production startup

After deploying to Railway/Render, open the service logs and confirm Flyway runs during boot.

- Look for Flyway startup lines (for example `Flyway Community Edition` and `Migrating schema`)
- Confirm migration success lines (for example `Successfully applied`)
- Confirm the app starts after migrations (for example `Tomcat started on port`)
- If the database is already populated, `baseline-on-migrate: true` in `application-prod.yaml` allows startup without rerunning old scripts

---

## 📦 Package Structure

```
com.ucms_backend/
├── controller/       → HTTP layer: routes, request/response mapping
├── service/          → Business logic, ownership checks, role enforcement
├── repository/       → JPA repositories (DB access only)
├── model/entity/     → JPA entities mapped to DB tables
├── dto/              → Request and response DTOs
├── config/           → SecurityConfig, JwtConfig
├── security/         → SupabaseAuthFilter
└── exception/        → GlobalExceptionHandler, AppException
```

> Schema changes are managed exclusively by Flyway migrations. `ddl-auto` is set to `none` — Hibernate does not modify the schema.

---

## 🧪 Testing the API (Postman)

### Import the Collection

1. Open **Postman**
2. Click **Import** (top left)
3. Select **File** and navigate to:
   ```
   docs/postman/UCMS-Full.postman_collection.json
   ```
4. Click **Import**

### Set Up the Environment

1. Click **Environments** in the left sidebar
2. Click **+** to create a new environment and name it `UCMS Local`
3. Add the following variables:

| Variable | Initial Value | Notes |
|---|---|---|
| `base_url` | `http://localhost:8080` | Change for staging/prod |
| `access_token` | *(leave blank)* | Auto-filled after Login |
| `admin_token` | *(leave blank)* | Set this to an admin JWT for admin-only routes |

4. Click **Save**, then select `UCMS Local` from the environment dropdown (top right)

### Usage

- Run **Register** to create an account
- Run **Login** — the access token is automatically saved to `{{access_token}}`
- Student and shared protected routes use `{{access_token}}` automatically
- Admin-only routes use `{{admin_token}}` (set this manually from an admin login)

---

## 🤝 Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for branch naming conventions, commit message format, PR template, and code style guidelines.

**Branch workflow:**
- Branch off `development` for all new work
- PRs must target `development` (never `main` directly)

---

## ⚙️ CI

GitHub Actions runs `./mvnw test` automatically on every pull request targeting `development`. Workflows are defined in [`.github/workflows/`](.github/workflows/).
