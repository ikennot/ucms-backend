# UCMS Backend Tasks

## Backlog

### [x] Fix V7 and V8 seed files — use real Supabase UUIDs, remove profile inserts
- **Ref**: `docs/issues/tickets-seeders.md`
- **Assignee**: codex
- **Labels**: `database`, `seeders`, `tickets`
- **Status**: RETURNED — wrong UUIDs, profile inserts must be removed

#### Background

Profiles already exist in Supabase via auth trigger. Do NOT insert into `profile` — it will conflict with existing rows. Use only real UUIDs from Supabase `auth.users`.

#### Real UUIDs (from uuid.md)

| UUID | role |
|---|---|
| `d71ccb57-6bc1-4660-a33c-28ab86998d12` | ADMIN |
| `d9201021-562a-43e4-a5a2-fed70ce541a0` | STUDENT |
| `c115ab19-9a6c-46c8-be66-2f27b99b9623` | STUDENT |
| `83a13dfa-afe4-48f7-b5d0-02ff2a475c37` | STUDENT |
| `82b7de73-be09-45e4-8bed-edd16007da75` | STUDENT |
| `cda210f3-1176-406b-9d2e-5b05ca921981` | STUDENT |
| `eeeb1ae5-4c82-4323-8c25-7ebad9bc91db` | STUDENT |

#### Fix Required

**`V7__seed_tickets.sql`**
- Remove all `INSERT INTO profile` — do not seed profiles
- Keep all ticket inserts
- Spread `user_id` across the 6 student UUIDs (use all of them at least once)

**`V8__seed_ticket_responses_attachments.sql`**
- Change all `admin_id` values to `d71ccb57-6bc1-4660-a33c-28ab86998d12`

#### Acceptance Criteria
- [ ] No `INSERT INTO profile` in V7
- [ ] All 6 student UUIDs used at least once across ticket `user_id`
- [ ] All `admin_id` in V8 = `d71ccb57-6bc1-4660-a33c-28ab86998d12`
- [ ] No FK violations
- [ ] All 4 ticket statuses still present

**`V8__seed_ticket_responses_attachments.sql`**
- Insert at least 1 `ticket_response` per `RESOLVED` and `CLOSED` ticket
- `admin_id` must reference the admin profile from V7
- Insert at least 2 `ticket_attachment` records linked to different tickets
- Use realistic `original_filename`, `mime_type` (e.g. `application/pdf`, `image/jpeg`), and `size_bytes`
- `storage_path` must be unique per row

#### Acceptance Criteria
- [ ] V7 runs cleanly after V6
- [ ] V8 runs cleanly after V7
- [ ] No FK violations
- [ ] All 4 ticket statuses present in seed data
- [ ] `./mvnw flyway:migrate` completes without errors on fresh DB

---

## Completed

- [x] Create Flyway seed files for Tickets domain (V7, V8) — `2026-02-28`
