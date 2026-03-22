package com.ucms_backend.service;

import com.ucms_backend.dto.AuthResponse;
import com.ucms_backend.dto.ChangePasswordRequest;
import com.ucms_backend.dto.ForgotPasswordRequest;
import com.ucms_backend.dto.LoginRequest;
import com.ucms_backend.dto.RegisterRequest;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.repository.ProfileRepository;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    // Student ID format: YYYYNNNN-C (e.g. 20230733-N) — 8 digits, hyphen, 1 uppercase letter
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{8}-[A-Z]$");

    private final SupabaseAuthService supabaseAuthService;
    private final ProfileRepository profileRepository;

    public AuthService(SupabaseAuthService supabaseAuthService, ProfileRepository profileRepository) {
        this.supabaseAuthService = supabaseAuthService;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public void register(RegisterRequest request) {
        UUID authUserId = null;

        try {
            if (!STUDENT_ID_PATTERN.matcher(request.getStudentId()).matches()) {
                throw new AppException(400, "INVALID_STUDENT_ID", "Invalid student ID format");
            }

            if (profileRepository.findByStudentId(request.getStudentId()).isPresent()) {
                throw new AppException(409, "STUDENT_ID_TAKEN", "Student ID already registered");
            }

            authUserId = supabaseAuthService.createUser(request.getStudentId(), request.getPassword());

            Profile profile = Profile.builder()
                    .authUserId(authUserId)
                    .studentId(request.getStudentId())
                    .name(request.getName())
                    .course(request.getCourse())
                    .yearLevel(request.getYearLevel())
                    .role("STUDENT")
                    .emailVerified(false)
                    .build();

            profileRepository.save(profile);
        } catch (AppException ex) {
            if (authUserId != null) {
                supabaseAuthService.deleteUser(authUserId);
            }
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            if (authUserId != null) {
                supabaseAuthService.deleteUser(authUserId);
            }
            throw new AppException(409, "STUDENT_ID_TAKEN", "Student ID already registered");
        } catch (Exception ex) {
            if (authUserId != null) {
                supabaseAuthService.deleteUser(authUserId);
            }
            throw new AppException(500, "REGISTRATION_FAILED", "Registration failed");
        }
    }

    public AuthResponse login(LoginRequest request) {
        return supabaseAuthService.login(request.getStudentId(), request.getPassword());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        profileRepository.findByStudentId(request.getStudentId()).ifPresent(profile -> {
            if (profile.getEmail() != null && profile.isEmailVerified()) {
                supabaseAuthService.sendPasswordResetEmail(profile.getStudentId(), profile.getEmail());
            }
        });
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        String currentPassword = request.getCurrentPassword() == null ? "" : request.getCurrentPassword().trim();
        String newPassword = request.getNewPassword() == null ? "" : request.getNewPassword().trim();

        if (newPassword.length() < 8) {
            throw new AppException(400, "WEAK_PASSWORD", "New password must be at least 8 characters");
        }

        if (currentPassword.equals(newPassword)) {
            throw new AppException(400, "SAME_PASSWORD", "New password must be different from current password");
        }

        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new AppException(404, "PROFILE_NOT_FOUND", "Profile not found"));

        if (!supabaseAuthService.isPasswordValid(profile.getStudentId(), currentPassword)) {
            throw new AppException(403, "CURRENT_PASSWORD_INCORRECT", "Current password is incorrect");
        }

        supabaseAuthService.updateUserPassword(profile.getAuthUserId(), newPassword);
    }

}
