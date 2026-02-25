# Contributing to UCMS Backend

## Branch Naming

Branch off `development`. Use the following prefixes with kebab-case:

```
feat/ticket-submission
fix/auth-token-expiry
chore/update-dependencies
docs/api-endpoints
```

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/). Keep the subject under 72 characters, imperative mood.

```
feat: add ticket submission endpoint
fix: resolve JWT expiry handling
chore: upgrade Spring Boot to 3.2.1
refactor: extract validation logic to service layer
test: add unit tests for TicketService
docs: document auth flow in README
```

## Pull Requests

1. Branch off `development`
2. Open PR targeting `development`
3. Include a short description of what changed and why
4. Ensure all checks pass before requesting review

## Code Style

- Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Lombok is allowed (`@Data`, `@Builder`, `@RequiredArgsConstructor`, etc.)
- No wildcard imports (e.g. `import java.util.*` is not allowed)

## Package Structure

```
src/main/java/com/ucms_backend/
├── controller/
├── service/
├── repository/
├── model/entity/
├── dto/
├── config/
└── exception/
```

## Testing

Run tests before pushing:

```bash
./mvnw test
```

- Unit tests are required for the service layer
- Use mocks for repository and external dependencies

## Secrets

- Never commit `application.yml` with real credentials
- Use environment variables or a `.env` file (gitignored)
- Reference values via `${ENV_VAR_NAME}` in `application.yml`

## CI

All PRs to `development` must pass the **Backend CI** workflow before merging.

- CI runs `./mvnw test` automatically on every PR
- Required GitHub Secrets (set in repo settings):
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `SUPABASE_JWKS_URI`
  - `SUPABASE_ISSUER`
- Do not merge if CI is red
