# Error Code Registry

This document is the master registry of all API `errorCode` values returned in `ApiResponse` error payloads.

Response shape target:

```json
{
  "success": false,
  "message": "...",
  "errorCode": "SCREAMING_SNAKE_CASE",
  "data": null
}
```

## 400 Bad Request

| errorCode | HTTP Status | Description | Notes |
| --- | --- | --- | --- |
| `VALIDATION_ERROR` | 400 Bad Request | Request validation failed (`@Valid`, missing required params/parts). | Thrown by `GlobalExceptionHandler`. |
| `INVALID_REQUEST_BODY` | 400 Bad Request | Request body is malformed or unreadable. | Thrown by `GlobalExceptionHandler`. |
| `INVALID_STUDENT_ID` | 400 Bad Request | Student ID format is invalid. | Thrown by `AuthService`. |
| `INVALID_FILE_TYPE` | 400 Bad Request | Uploaded file type is not allowed. | Thrown by `AttachmentService`. |

## 403 Forbidden

| errorCode | HTTP Status | Description | Notes |
| --- | --- | --- | --- |
| `ACCESS_DENIED` | 403 Forbidden | Access denied by Spring Security authorization checks. | Thrown by `GlobalExceptionHandler` for `AccessDeniedException`. |
| `FORBIDDEN` | 403 Forbidden | Caller is not allowed to access or modify the target resource. | Thrown by service-layer ownership/authorization checks. |
| `ACCOUNT_LIMITED` | 403 Forbidden | Account does not meet requirements for this action (for example, unverified email). | Thrown by `TicketService` and `AttachmentService`. |
| `TICKET_CLOSED` | 403 Forbidden | Action cannot be performed because the ticket is closed. | Thrown by `TicketService` and `AttachmentService`. |

## 401 Unauthorized

| errorCode | HTTP Status | Description | Notes |
| --- | --- | --- | --- |
| `UNAUTHORIZED` | 401 Unauthorized | Missing, invalid, or expired authentication credentials. | Returned by Spring Security authentication entry point and token auth filter. |

## 404 Not Found

| errorCode | HTTP Status | Description | Notes |
| --- | --- | --- | --- |
| `CATEGORY_NOT_FOUND` | 404 Not Found | Requested category does not exist. | Thrown by `CategoryService` and `TicketService`. |
| `PROFILE_NOT_FOUND` | 404 Not Found | Requested profile does not exist. | Thrown by `ProfileService`, `TicketService`, and `AttachmentService`. |
| `TICKET_NOT_FOUND` | 404 Not Found | Requested ticket does not exist. | Thrown by `TicketService`, `TicketResponseService`, and `AttachmentService`. |
| `NOTIFICATION_NOT_FOUND` | 404 Not Found | Requested notification does not exist. | Thrown by `NotificationService`. |

## 409 Conflict

| errorCode | HTTP Status | Description | Notes |
| --- | --- | --- | --- |
| `STUDENT_ID_TAKEN` | 409 Conflict | Student ID is already registered. | Thrown by `AuthService`. |
| `CATEGORY_ALREADY_EXISTS` | 409 Conflict | Category name already exists. | Thrown by `CategoryService`. |
| `CATEGORY_IN_USE` | 409 Conflict | Category cannot be deleted because it is still referenced. | Thrown by `CategoryService`. |
| `ALREADY_CONFIRMED` | 409 Conflict | Ticket resolution has already been confirmed. | Thrown by `TicketService`. |
| `CONFIRMATION_REQUIRED` | 409 Conflict | Ticket cannot be closed until student confirmation is completed. | Thrown by `TicketService`. |
| `INVALID_STATUS_TRANSITION` | 409 Conflict | Requested ticket status transition is not allowed. | Thrown by `TicketService`. |

## 413 Payload Too Large

| errorCode | HTTP Status | Description | Notes |
| --- | --- | --- | --- |
| `FILE_TOO_LARGE` | 413 Payload Too Large | Uploaded file exceeds the maximum allowed size. | Thrown by `GlobalExceptionHandler` and `AttachmentService`. |

## 500 Internal Server Error

| errorCode | HTTP Status | Description | Notes |
| --- | --- | --- | --- |
| `REGISTRATION_FAILED` | 500 Internal Server Error | Registration failed due to an internal server-side issue. | Thrown by `AuthService`. |
| `FILE_READ_ERROR` | 500 Internal Server Error | Server failed to read uploaded file bytes. | Thrown by `AttachmentService`. |
| `INTERNAL_SERVER_ERROR` | 500 Internal Server Error | Unhandled server error fallback. | Thrown by `GlobalExceptionHandler` generic fallback. |

## 502 Bad Gateway

| errorCode | HTTP Status | Description | Notes |
| --- | --- | --- | --- |
| `SUPABASE_ERROR` | 502 Bad Gateway | Supabase authentication service returned an upstream error. | Thrown by `SupabaseAuthService`. |
| `STORAGE_ERROR` | 502 Bad Gateway | Supabase storage service returned an upstream error. | Thrown by `SupabaseStorageService`. |
