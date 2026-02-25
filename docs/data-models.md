# UCMS Data Models / ERD

```
┌──────────────┐     ┌──────────────────┐
│   Profile    │     │    Category      │
│──────────────│     │──────────────────│
│ auth_user_id │     │ id (PK)          │
│ (PK, UUID)   │     │ name             │
│ student_id   │     └────────┬─────────┘
│ name         │              │ 1
│ email?       │              │
│ email_verified│             │ many
│ course       │     ┌────────▼─────────────────┐
│ year_level   │     │         Ticket            │
│ role         │◄────│──────────────────────────│
│ created_at   │ 1   │ id (PK)                  │
└──────┬───────┘     │ user_id (FK)             │
       │             │ category_id (FK)          │
       │             │ ticket_number             │
       │             │ title                     │
       │             │ description               │
       │             │ status                    │
       │             │ confirmed_resolved        │
       │             │ created_at                │
       │             │ updated_at                │
       │             └──┬──────┬────────────────┘
       │                │      │
       │          1     │      │ 1
       │        many    │      │ many
┌──────▼───────┐  ┌─────▼──┐ ┌▼──────────────────┐
│ Notification │  │ Ticket │ │ TicketAttachment   │
│──────────────│  │Response│ │────────────────────│
│ id (PK)      │  │────────│ │ id (PK)            │
│ user_id (FK) │  │ id(PK) │ │ ticket_id (FK)     │
│ ticket_id(FK)│  │ticket_id│ │ storage_path      │
│ message      │  │ (FK)   │ │ original_filename  │
│ is_read      │  │admin_id│ │ mime_type          │
│ created_at   │  │ (FK)   │ │ size_bytes         │
└──────────────┘  │message │ │ uploaded_at        │
                  │created_│ └────────────────────┘
                  │  at    │
                  └────────┘
```

## Key Decisions

- Supabase Auth stores credentials; no password is stored in app DB
- `role` on Profile — `STUDENT` or `ADMIN`
- `email_verified` gates ticket creation (LIMITED accounts are read-only)
- `status` on Ticket — `PENDING`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`
- All tickets are handled by MIS (Management Information System) — no department routing
- `category_id` on Ticket — FK to `Category` table. Categories are admin-configurable.
- Default seed categories: Academic-related, Grades, AIMS Problem, ID Concern, Facility Issue, Others
- Supabase Storage object key stored in `TicketAttachment.storage_path` (attachments kept private)
- `original_filename`, `mime_type`, and `size_bytes` are derived by the backend from the uploaded file — not sent by the client
- Department table removed — MIS coordinates internally with other offices as needed

## Storage Notes (Supabase)

- Attachments are stored in a private bucket (`ticket-attachments`)
- DB stores `storage_path` (object key), not a permanent URL
- API returns signed URLs (short expiry) for viewing/downloading

## Suggested Attachment Fields

- `storage_path` (string, unique per object)
- `original_filename` (string)
- `mime_type` (string)
- `size_bytes` (long)

## Category Notes

- `Category` is a standalone entity managed by admins
- Seeded on first run: Academic-related, Grades, AIMS Problem, ID Concern, Facility Issue, Others
- Cannot be deleted if referenced by any ticket (`409 Conflict`)
