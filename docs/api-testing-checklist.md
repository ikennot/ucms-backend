# UCMS API Testing Checklist

Use this checklist to verify that `docs/postman/UCMS-Auth.postman_collection.json` works end-to-end.

## Setup

- [X] App is running (`./mvnw spring-boot:run`)
- [X] Postman environment selected (`UCMS Local`)
- [X] `base_url` is correct (default: `http://localhost:8080`)
- [x] You have both a **student** account token and an **admin** account token for role-based routes

## Auth

- [X] `POST /api/auth/register` returns `201` and success message
- [X] `POST /api/auth/login` returns `200` and contains `accessToken` + `refreshToken`
- [ ] `access_token` is auto-saved in Postman after login
- [ ] `POST /api/auth/forgot-password` returns `200` for a valid `studentId`

## Users

- [X] `GET /api/users/me` returns `200` with profile data
- [X] `PUT /api/users/me` returns `200` and reflects updated profile fields
- [X] `PUT /api/users/me/email` returns `200` and triggers email update flow

## Categories

- [x] `GET /api/categories` returns `200` (student/admin)
- [x] `POST /api/categories` returns `201` when using admin token
- [x] `PUT /api/categories/{id}` returns `200` when using admin token
- [x] `DELETE /api/categories/{id}` returns `204` when deletable and using admin token

## Tickets

- [x] `POST /api/tickets` returns `201` when using a valid student token
- [x] `GET /api/tickets` returns `200` (student sees own, admin sees all)
- [x] `GET /api/tickets/{id}` returns `200` for accessible ticket
- [x] `PATCH /api/tickets/{id}/status` returns `200` when using admin token
- [x] `PATCH /api/tickets/{id}/confirm-resolved` returns `200` when using student owner token

## Ticket Responses

- [X] `POST /api/tickets/{id}/responses` returns `201` with admin token
- [x] `GET /api/tickets/{id}/responses` returns `200` for authorized user

## Attachments

- [x] `POST /api/tickets/{id}/attachments` returns `201` with file upload (student token)
- [x] `GET /api/tickets/{id}/attachments` returns `200` and attachment list

## Notifications

- [x] `GET /api/notifications` returns `200` with student token
- [x] `PATCH /api/notifications/{id}/read` returns `200`
- [x] `PATCH /api/notifications/read-all` returns `200`

## Analytics

- [x] `GET /api/analytics/summary` returns `200` with admin token
- [x] `GET /api/analytics/by-category` returns `200` with admin token
- [x] `GET /api/analytics/unresolved` returns `200` with admin token

## Negative / Access Control Checks

- [x] Student token is rejected on admin-only routes (`403` expected)
- [x] Missing/invalid token on protected routes returns `401`
- [x] Invalid payloads (missing required fields) return validation error (`400`)
