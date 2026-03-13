## Overview
Audit every endpoint to ensure all error responses use a consistent errorCode string in the ApiResponse wrapper. Error codes must be SCREAMING_SNAKE_CASE, descriptive, and documented. This ensures the Android client can handle errors programmatically without parsing messages.

## Tasks
- [ ] Define master error code registry — document all errorCode values in docs/error-codes.md (e.g. ACCOUNT_LIMITED, TICKET_NOT_FOUND, CATEGORY_IN_USE, INVALID_STATUS_TRANSITION, CONFIRMATION_REQUIRED, VALIDATION_ERROR, FORBIDDEN, UNAUTHORIZED, FILE_TOO_LARGE, INVALID_FILE_TYPE, NOTIFICATION_NOT_FOUND, ALREADY_CONFIRMED, CATEGORY_ALREADY_EXISTS)
- [ ] Audit GlobalExceptionHandler — ensure every exception type maps to a documented errorCode
- [ ] Audit all service layer throws — replace any generic RuntimeException with typed AppException with errorCode
- [ ] Verify ApiResponse<T> error shape is consistent: { status, message, errorCode, data: null } on all error responses
- [ ] Update unit tests to assert errorCode field in error responses

## Acceptance Criteria
- docs/error-codes.md exists and lists all errorCode values with HTTP status and description
- Every error response from the API includes a non-null errorCode field
- No endpoint returns a generic 500 for a known business rule violation
- GlobalExceptionHandler covers all AppException subtypes
- Unit tests assert errorCode on all error scenarios
- ./mvnw test passes

## References
- src/main/java/com/ucms_backend/exception/GlobalExceptionHandler.java
- src/main/java/com/ucms_backend/exception/AppException.java
- src/main/java/com/ucms_backend/dto/ApiResponse.java
- src/main/java/com/ucms_backend/service/