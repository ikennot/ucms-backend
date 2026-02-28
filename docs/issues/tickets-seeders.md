# Issue: Create Seeders for Tickets Domain

**Label**: `database`, `seeders`, `tickets`
**Priority**: High
**Type**: Infrastructure

---

## Overview

Create seed data for the Tickets domain to support development and testing. Seeders will populate realistic sample data for `ticket`, `ticket_response`, and `ticket_attachment` tables, enabling consistent local and staging environments.

---

## Seeders to Create

| File | Description |
|---|---|
| `V7__seed_tickets.sql` | Sample `ticket` records |
| `V8__seed_ticket_responses_attachments.sql` | Sample `ticket_response` and `ticket_attachment` records |

---

## V7 — `ticket` seed data

**Requirements:**
- Cover all status values: `PENDING`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`
- Reference existing `profile(auth_user_id)` and `category(id)` records from prior seeds
- Use realistic ticket numbers following format `TKT-yyyyMMdd-XXXX`
- Include varied `title` and `description` values per category

**Sample categories to cover:**
- Academic-related
- Grades
- AIMS Problem
- ID Concern
- Facility Issue
- Others

---

## V8 — `ticket_response` and `ticket_attachment` seed data

**`ticket_response`:**
- At least one response per resolved/closed ticket
- `responder_id` must reference an admin profile

**`ticket_attachment`:**
- Sample file URLs for tickets with attachments
- At least 2–3 attachment records total

**Dependencies:** V7 must exist.

---

## Acceptance Criteria

- [ ] V7 seeds insert sample tickets covering all status values and categories
- [ ] V8 seeds insert responses and attachments linked to valid ticket IDs
- [ ] Seeds run cleanly on a fresh DB after migrations (`./mvnw flyway:migrate`)
- [ ] No FK violations on insert
- [ ] Seed data does not conflict with existing profile/category seeds

---

## Notes

- Seed files location: `src/main/resources/db/migration/`
- Ticket number format: `TKT-yyyyMMdd-XXXX`
- Status enum values: `PENDING`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`
- Dependencies: V1 (profiles), V2 (categories), V3 (tickets), V4 (responses/attachments), V5, V6 must exist
