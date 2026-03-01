package com.ucms_backend.service;

import com.ucms_backend.dto.ProfileResponse;
import com.ucms_backend.dto.UpdateEmailRequest;
import com.ucms_backend.dto.UpdateProfileRequest;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.repository.ProfileRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private SupabaseAuthService supabaseAuthService;

    @InjectMocks
    private ProfileService profileService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void getMyProfile_profileNotFound_throws404() {
        UUID userId = UUID.randomUUID();

        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> profileService.getMyProfile(userId));

        assertEquals(404, exception.getStatus());
        assertEquals("PROFILE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getMyProfile_success_returnsProfileResponse() {
        UUID userId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .authUserId(userId)
                .studentId("20242025-A")
                .name("Jane Student")
                .email("jane.student@ucms.local")
                .emailVerified(true)
                .course("BSCS")
                .yearLevel(2)
                .role("STUDENT")
                .build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getMyProfile(userId);

        assertEquals(userId, response.getAuthUserId());
        assertEquals("20242025-A", response.getStudentId());
        assertEquals("Jane Student", response.getName());
        assertEquals("jane.student@ucms.local", response.getEmail());
        assertEquals("BSCS", response.getCourse());
        assertEquals(2, response.getYearLevel());
        assertEquals("STUDENT", response.getRole());
        assertEquals(true, response.isEmailVerified());
    }

    @Test
    void updateProfile_profileNotFound_throws404() {
        UUID userId = UUID.randomUUID();
        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "BSIT", 3);

        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> profileService.updateProfile(userId, request));

        assertEquals(404, exception.getStatus());
        assertEquals("PROFILE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void updateProfile_otherUser_throws403() {
        UUID userId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .authUserId(UUID.randomUUID())
                .name("Jane Student")
                .role("STUDENT")
                .emailVerified(false)
                .build();
        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "BSIT", 3);

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        AppException exception = assertThrows(AppException.class, () -> profileService.updateProfile(userId, request));

        assertEquals(403, exception.getStatus());
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void updateProfile_success_updatesNameCourseYearLevel() {
        UUID userId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .authUserId(userId)
                .name("Old Name")
                .course("BSCS")
                .yearLevel(1)
                .role("STUDENT")
                .emailVerified(false)
                .build();
        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "BSIT", 3);

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResponse response = profileService.updateProfile(userId, request);

        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(profileCaptor.capture());

        Profile saved = profileCaptor.getValue();
        assertEquals("New Name", saved.getName());
        assertEquals("BSIT", saved.getCourse());
        assertEquals(3, saved.getYearLevel());
        assertEquals("New Name", response.getName());
        assertEquals("BSIT", response.getCourse());
        assertEquals(3, response.getYearLevel());
    }

    @Test
    void updateEmail_profileNotFound_throws404() {
        UUID userId = UUID.randomUUID();
        UpdateEmailRequest request = new UpdateEmailRequest("new.email@ucms.local");

        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> profileService.updateEmail(userId, request));

        assertEquals(404, exception.getStatus());
        assertEquals("PROFILE_NOT_FOUND", exception.getErrorCode());
        verify(supabaseAuthService, never()).sendVerificationEmail(any(UUID.class), any(String.class));
    }

    @Test
    void updateEmail_success_setsEmailAndVerifiedFalse() {
        UUID userId = UUID.randomUUID();
        Profile profile = Profile.builder()
                .authUserId(userId)
                .name("Jane Student")
                .email("old.email@ucms.local")
                .emailVerified(true)
                .role("STUDENT")
                .build();
        UpdateEmailRequest request = new UpdateEmailRequest("new.email@ucms.local");

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResponse response = profileService.updateEmail(userId, request);

        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(profileCaptor.capture());

        Profile saved = profileCaptor.getValue();
        assertEquals("new.email@ucms.local", saved.getEmail());
        assertFalse(saved.isEmailVerified());
        assertEquals("new.email@ucms.local", response.getEmail());
        assertFalse(response.isEmailVerified());
        verify(supabaseAuthService).sendVerificationEmail(userId, "new.email@ucms.local");
    }

    @Test
    void updateEmail_invalidFormat_validationError() {
        UpdateEmailRequest request = new UpdateEmailRequest("invalid-email");

        Set<ConstraintViolation<UpdateEmailRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }
}
