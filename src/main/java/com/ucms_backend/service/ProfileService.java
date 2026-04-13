package com.ucms_backend.service;

import com.ucms_backend.dto.ProfileResponse;
import com.ucms_backend.dto.RealtimeEventResponse;
import com.ucms_backend.dto.UpdateEmailRequest;
import com.ucms_backend.dto.UpdateProfileRequest;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.repository.ProfileRepository;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final SupabaseAuthService supabaseAuthService;
    private final RealtimeSseService realtimeSseService;

    public ProfileService(
            ProfileRepository profileRepository,
            SupabaseAuthService supabaseAuthService,
            RealtimeSseService realtimeSseService
    ) {
        this.profileRepository = profileRepository;
        this.supabaseAuthService = supabaseAuthService;
        this.realtimeSseService = realtimeSseService;
    }

    public List<ProfileResponse> getAllAdmins() {
        return profileRepository.findByRole("ADMIN").stream()
                .map(ProfileResponse::from)
                .collect(Collectors.toList());
    }

    public ProfileResponse getMyProfile(UUID authUserId) {
        Profile profile = profileRepository.findById(authUserId)
                .orElseThrow(() -> new AppException(404, "PROFILE_NOT_FOUND", "Profile not found"));
        return ProfileResponse.from(profile);
    }

    public ProfileResponse updateProfile(UUID authUserId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findById(authUserId)
                .orElseThrow(() -> new AppException(404, "PROFILE_NOT_FOUND", "Profile not found"));

        ensureOwner(profile, authUserId);

        profile.setName(request.getName());
        profile.setCourse(request.getCourse());
        profile.setYearLevel(request.getYearLevel());

        Profile saved = profileRepository.save(profile);
        publishProfileEvent(saved.getAuthUserId(), "PROFILE_UPDATED");
        return ProfileResponse.from(saved);
    }

    public ProfileResponse updateEmail(UUID authUserId, UpdateEmailRequest request) {
        Profile profile = profileRepository.findById(authUserId)
                .orElseThrow(() -> new AppException(404, "PROFILE_NOT_FOUND", "Profile not found"));

        ensureOwner(profile, authUserId);

        profile.setEmail(request.getEmail());
        profile.setEmailVerified(false);

        Profile saved = profileRepository.save(profile);
        supabaseAuthService.sendVerificationEmail(authUserId, request.getEmail());
        publishProfileEvent(saved.getAuthUserId(), "PROFILE_EMAIL_UPDATED");
        return ProfileResponse.from(saved);
    }

    public ProfileResponse confirmEmailVerified(UUID authUserId) {
        Profile profile = profileRepository.findById(authUserId)
                .orElseThrow(() -> new AppException(404, "PROFILE_NOT_FOUND", "Profile not found"));

        ensureOwner(profile, authUserId);

        if (profile.getEmail() == null || profile.getEmail().isBlank()) {
            throw new AppException(400, "EMAIL_REQUIRED", "Email is required before verification");
        }

        profile.setEmailVerified(true);
        Profile saved = profileRepository.save(profile);
        publishProfileEvent(saved.getAuthUserId(), "PROFILE_EMAIL_VERIFIED");
        return ProfileResponse.from(saved);
    }

    private void publishProfileEvent(UUID userId, String eventType) {
        realtimeSseService.publishToUser(userId, RealtimeEventResponse.builder()
                .domain("profile")
                .eventType(eventType)
                .entityId(userId.toString())
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .actorRole("SYSTEM")
                .build());
    }

    private void ensureOwner(Profile profile, UUID authUserId) {
        if (!profile.getAuthUserId().equals(authUserId)) {
            throw new AppException(403, "FORBIDDEN", "Access denied");
        }
    }
}
