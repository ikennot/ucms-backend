package com.ucms_backend.controller;

import com.ucms_backend.dto.ApiResponse;
import com.ucms_backend.dto.ChangePasswordRequest;
import com.ucms_backend.dto.ProfileResponse;
import com.ucms_backend.dto.UpdateEmailRequest;
import com.ucms_backend.dto.UpdateProfileRequest;
import com.ucms_backend.service.AuthService;
import com.ucms_backend.service.ProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ProfileService profileService;
    private final AuthService authService;

    public UserController(ProfileService profileService, AuthService authService) {
        this.profileService = profileService;
        this.authService = authService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile() {
        UUID userId = getUserId();
        ProfileResponse response = profileService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", response));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = getUserId();
        ProfileResponse response = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", response));
    }

    @PutMapping("/me/password")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = getUserId();
        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    @PutMapping("/me/email")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateEmail(@Valid @RequestBody UpdateEmailRequest request) {
        UUID userId = getUserId();
        ProfileResponse response = profileService.updateEmail(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Email update initiated", response));
    }

    @PatchMapping("/me/email/verify")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<ProfileResponse>> confirmEmailVerified() {
        UUID userId = getUserId();
        ProfileResponse response = profileService.confirmEmailVerified(userId);
        return ResponseEntity.ok(ApiResponse.ok("Email marked as verified", response));
    }

    private UUID getUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
