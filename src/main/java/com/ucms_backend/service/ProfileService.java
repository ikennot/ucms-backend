package com.ucms_backend.service;

import com.ucms_backend.dto.ProfileResponse;
import com.ucms_backend.dto.UpdateEmailRequest;
import com.ucms_backend.dto.UpdateProfileRequest;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.repository.ProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final SupabaseAuthService supabaseAuthService;

    public ProfileService(ProfileRepository profileRepository, SupabaseAuthService supabaseAuthService) {
        this.profileRepository = profileRepository;
        this.supabaseAuthService = supabaseAuthService;
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
        return ProfileResponse.from(saved);
    }

    private void ensureOwner(Profile profile, UUID authUserId) {
        if (!profile.getAuthUserId().equals(authUserId)) {
            throw new AppException(403, "FORBIDDEN", "Access denied");
        }
    }
}
