package com.ucms_backend.service;

import com.ucms_backend.dto.AuthResponse;
import com.ucms_backend.dto.ChangePasswordRequest;
import com.ucms_backend.dto.ForgotPasswordRequest;
import com.ucms_backend.dto.LoginRequest;
import com.ucms_backend.dto.RegisterRequest;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.repository.ProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService business logic.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SupabaseAuthService supabaseAuthService;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_success() {
        RegisterRequest request = buildRegisterRequest("20242025-A");
        UUID authUserId = UUID.randomUUID();

        when(profileRepository.findByStudentId(request.getStudentId()))
                .thenReturn(Optional.empty());
        when(supabaseAuthService.createUser(request.getStudentId(), request.getPassword()))
                .thenReturn(authUserId);

        authService.register(request);

        verify(profileRepository).findByStudentId(request.getStudentId());
        verify(supabaseAuthService).createUser(request.getStudentId(), request.getPassword());

        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(profileCaptor.capture());

        Profile savedProfile = profileCaptor.getValue();
        assertNotNull(savedProfile);
        assertEquals(authUserId, savedProfile.getAuthUserId());
        assertEquals(request.getStudentId(), savedProfile.getStudentId());
        assertEquals(request.getName(), savedProfile.getName());
        assertEquals(request.getCourse(), savedProfile.getCourse());
        assertEquals(request.getYearLevel(), savedProfile.getYearLevel());
        assertEquals("STUDENT", savedProfile.getRole());
        assertFalse(savedProfile.isEmailVerified());
    }

    @Test
    void register_invalidStudentIdFormat() {
        RegisterRequest request = buildRegisterRequest("INVALID");

        AppException exception = assertThrows(AppException.class, () -> authService.register(request));

        assertEquals(400, exception.getStatus());
        assertEquals("INVALID_STUDENT_ID", exception.getErrorCode());
        verify(profileRepository, never()).findByStudentId(anyString());
        verify(supabaseAuthService, never()).createUser(anyString(), anyString());
    }

    @Test
    void register_duplicateStudentId() {
        RegisterRequest request = buildRegisterRequest("20242025-A");
        Profile existingProfile = Profile.builder()
                .studentId(request.getStudentId())
                .name("Jane Student")
                .role("STUDENT")
                .emailVerified(false)
                .build();

        when(profileRepository.findByStudentId(request.getStudentId()))
                .thenReturn(Optional.of(existingProfile));

        AppException exception = assertThrows(AppException.class, () -> authService.register(request));

        assertEquals(409, exception.getStatus());
        assertEquals("STUDENT_ID_TAKEN", exception.getErrorCode());
        verify(supabaseAuthService, never()).createUser(anyString(), anyString());
        verify(profileRepository, never()).save(org.mockito.ArgumentMatchers.any(Profile.class));
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest("20242025-A", "password123");
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        when(supabaseAuthService.login(request.getStudentId(), request.getPassword()))
                .thenReturn(response);

        AuthResponse result = authService.login(request);

        assertSame(response, result);
        verify(supabaseAuthService).login(request.getStudentId(), request.getPassword());
    }

    @Test
    void forgotPassword_studentNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("20242025-A");

        when(profileRepository.findByStudentId(request.getStudentId()))
                .thenReturn(Optional.empty());

        authService.forgotPassword(request);

        verify(supabaseAuthService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgotPassword_emailNull() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("20242025-A");
        Profile profile = Profile.builder()
                .studentId(request.getStudentId())
                .name("Jane Student")
                .role("STUDENT")
                .email(null)
                .emailVerified(false)
                .build();

        when(profileRepository.findByStudentId(request.getStudentId()))
                .thenReturn(Optional.of(profile));

        authService.forgotPassword(request);

        verify(supabaseAuthService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgotPassword_emailNotVerified() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("20242025-A");
        Profile profile = Profile.builder()
                .studentId(request.getStudentId())
                .name("Jane Student")
                .role("STUDENT")
                .email("jane.student@ucms.local")
                .emailVerified(false)
                .build();

        when(profileRepository.findByStudentId(request.getStudentId()))
                .thenReturn(Optional.of(profile));

        authService.forgotPassword(request);

        verify(supabaseAuthService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgotPassword_success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("20242025-A");
        Profile profile = Profile.builder()
                .studentId(request.getStudentId())
                .name("Jane Student")
                .role("STUDENT")
                .email("jane.student@ucms.local")
                .emailVerified(true)
                .build();

        when(profileRepository.findByStudentId(request.getStudentId()))
                .thenReturn(Optional.of(profile));

        authService.forgotPassword(request);

        verify(supabaseAuthService).sendPasswordResetEmail(eq("20242025-A"), eq("jane.student@ucms.local"));
    }

    @Test
    void changePassword_success() {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword123", "newPassword123");
        Profile profile = Profile.builder()
                .authUserId(userId)
                .studentId("20242025-A")
                .name("Jane Student")
                .role("STUDENT")
                .emailVerified(true)
                .build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(supabaseAuthService.isPasswordValid("20242025-A", "oldPassword123")).thenReturn(true);

        authService.changePassword(userId, request);

        verify(supabaseAuthService).updateUserPassword(userId, "newPassword123");
    }

    @Test
    void changePassword_profileNotFound() {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword123", "newPassword123");

        when(profileRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.changePassword(userId, request));

        assertEquals(404, exception.getStatus());
        assertEquals("PROFILE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void changePassword_samePassword() {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("samePassword123", "samePassword123");

        AppException exception = assertThrows(AppException.class, () -> authService.changePassword(userId, request));

        assertEquals(400, exception.getStatus());
        assertEquals("SAME_PASSWORD", exception.getErrorCode());
    }

    @Test
    void changePassword_weakPassword() {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword123", "short");

        AppException exception = assertThrows(AppException.class, () -> authService.changePassword(userId, request));

        assertEquals(400, exception.getStatus());
        assertEquals("WEAK_PASSWORD", exception.getErrorCode());
    }

    @Test
    void changePassword_incorrectCurrentPassword() {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", "newPassword123");
        Profile profile = Profile.builder()
                .authUserId(userId)
                .studentId("20242025-A")
                .name("Jane Student")
                .role("STUDENT")
                .emailVerified(true)
                .build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(supabaseAuthService.isPasswordValid("20242025-A", "wrongPassword")).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.changePassword(userId, request));

        assertEquals(403, exception.getStatus());
        assertEquals("CURRENT_PASSWORD_INCORRECT", exception.getErrorCode());
        verify(supabaseAuthService, never()).updateUserPassword(org.mockito.ArgumentMatchers.any(), anyString());
    }

    private RegisterRequest buildRegisterRequest(String studentId) {
        return new RegisterRequest(studentId, "Jane Student", "password123", "BSCS", 2);
    }
}
