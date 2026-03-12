## Overview
Audit and harden all security-sensitive surfaces before deployment. Covers input validation on all DTOs, file upload size and type constraints for attachments, signed URL expiry configuration, and rate limiting coverage review.

## Tasks
- [x] Audit all request DTOs — ensure @NotBlank, @Size, @Email, @Pattern annotations are present and correct on every field (CreateTicketRequest, UpdateProfileRequest, UpdateEmailRequest, CreateResponseRequest, CreateCategoryRequest, UpdateCategoryRequest)
- [x] Add global @Validated enforcement — verify @Valid is applied on all @RequestBody parameters in every controller
- [x] File upload constraints — enforce max file size (e.g. 10MB) and allowed MIME types (image/jpeg, image/png, application/pdf) in AttachmentService; reject with 400 INVALID_FILE_TYPE or 413 FILE_TOO_LARGE
- [x] Signed URL expiry — make expiry duration configurable via application.yaml (e.g. supabase.storage.signed-url-expiry-seconds); default 3600
- [x] Rate limiting coverage — verify RateLimitFilter covers all public endpoints (/api/auth/*); confirm protected endpoints are not double-limited
- [x] Verify role is always derived from JWT claims — never from request body; audit all service methods
- [x] Confirm service role key is never logged or exposed in any response or stack trace
- [x] Unit tests — invalid file type rejected, oversized file rejected, missing required fields return 400

## Acceptance Criteria
- All DTOs have complete validation annotations; missing field returns 400 VALIDATION_ERROR with field details
- File upload rejects files > 10MB with 413 FILE_TOO_LARGE
- File upload rejects disallowed MIME types with 400 INVALID_FILE_TYPE
- Signed URL expiry is configurable and defaults to 3600 seconds
- Rate limiting applies to all /api/auth/* endpoints
- Role is never accepted from request body in any endpoint
- Service role key does not appear in any log output or API response
- All unit tests pass: ./mvnw test

## References
- docs/roles-permissions.md — Enforcement section
- docs/api-contract.md — Attachments notes
- src/main/java/com/ucms_backend/security/RateLimitFilter.java
- src/main/java/com/ucms_backend/config/SecurityConfig.java
- src/main/java/com/ucms_backend/dto/
- src/main/java/com/ucms_backend/service/
