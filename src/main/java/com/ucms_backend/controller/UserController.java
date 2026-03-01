package com.ucms_backend.controller;

import com.ucms_backend.dto.ApiResponse;
import com.ucms_backend.dto.ProfileResponse;
import com.ucms_backend.dto.UpdateEmailRequest;
import com.ucms_backend.dto.UpdateProfileRequest;
import com.ucms_backend.service.ProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ProfileService profileService;

    public UserController(ProfileService profileService) {
        this.profileService = profileService;
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

    @PutMapping("/me/email")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateEmail(@Valid @RequestBody UpdateEmailRequest request) {
        UUID userId = getUserId();
        ProfileResponse response = profileService.updateEmail(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Email update initiated", response));
    }

    private UUID getUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
