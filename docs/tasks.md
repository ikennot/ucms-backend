## Overview
The existing Postman collection (UCMS-Auth.postman_collection.json) only covers auth endpoints. Expand it to cover all endpoints added in Phase 3 (Profile, Attachments, Responses, Notifications, Analytics) and Phase 4 (confirm-resolved, Categories CRUD). Collection must use environment variables and include example request/response bodies.

## Tasks
- [x] Add Profile folder — GET /api/users/me, PUT /api/users/me, PUT /api/users/me/email (with example bodies)
- [x] Add Tickets folder — POST /api/tickets, GET /api/tickets, GET /api/tickets/{id}, PATCH /api/tickets/{id}/status, PATCH /api/tickets/{id}/confirm-resolved (with example bodies and status values)
- [x] Add Attachments folder — POST /api/tickets/{id}/attachments (multipart/form-data), GET /api/tickets/{id}/attachments
- [x] Add Responses folder — POST /api/tickets/{id}/responses, GET /api/tickets/{id}/responses
- [x] Add Notifications folder — GET /api/notifications, PATCH /api/notifications/{id}/read, PATCH /api/notifications/read-all
- [x] Add Analytics folder — GET /api/analytics/summary, GET /api/analytics/by-category, GET /api/analytics/unresolved
- [x] Add Categories folder — GET /api/categories, POST /api/categories, PUT /api/categories/{id}, DELETE /api/categories/{id}
- [x] All requests use {{base_url}} and {{access_token}} environment variables
- [x] Add admin_token environment variable for admin-only endpoints
- [x] Save updated collection to docs/postman/UCMS-Full.postman_collection.json

## Acceptance Criteria
- Collection covers all endpoints in docs/api-contract.md plus PATCH /api/tickets/{id}/confirm-resolved
- All requests use environment variables (no hardcoded URLs or tokens)
- Each request has an example response body saved
- Collection imports cleanly into Postman with no errors
- README.md updated to reference UCMS-Full.postman_collection.json

## References
- docs/api-contract.md — full endpoint list
- docs/postman/UCMS-Auth.postman_collection.json — existing collection
- README.md — Postman usage section
