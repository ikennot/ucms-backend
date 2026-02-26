package com.ucms_backend.service;

import com.ucms_backend.dto.AuthResponse;
import com.ucms_backend.dto.ForgotPasswordRequest;
import com.ucms_backend.dto.LoginRequest;
import com.ucms_backend.dto.RegisterRequest;
import com.ucms_backend.dto.ResetPasswordRequest;
import com.ucms_backend.exception.AppException;
import com.ucms_backend.model.entity.Profile;
import com.ucms_backend.repository.ProfileRepository;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{4}\\d{4}-[A-Z]$");

    private final SupabaseAuthService supabaseAuthService;
    private final ProfileRepository profileRepository;

    public AuthService(SupabaseAuthService supabaseAuthService, ProfileRepository profileRepository) {
        this.supabaseAuthService = supabaseAuthService;
        this.profileRepository = profileRepository;
    }

    public void register(RegisterRequest request) {
        if (!STUDENT_ID_PATTERN.matcher(request.getStudentId()).matches()) {
            throw new AppException(400, "INVALID_STUDENT_ID", "Invalid student ID format");
        }

        if (profileRepository.findByStudentId(request.getStudentId()).isPresent()) {
            throw new AppException(409, "STUDENT_ID_TAKEN", "Student ID already registered");
        }

        UUID authUserId = supabaseAuthService.createUser(request.getStudentId(), request.getPassword());

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
    }

    public AuthResponse login(LoginRequest request) {
        return supabaseAuthService.login(request.getStudentId(), request.getPassword());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        Profile profile = profileRepository.findByStudentId(request.getStudentId())
                .orElseThrow(() -> new AppException(404, "USER_NOT_FOUND", "Student not found"));

        if (profile.getEmail() == null || !profile.isEmailVerified()) {
            throw new AppException(
                    400,
                    "EMAIL_NOT_VERIFIED",
                    "Please add and verify your email before resetting your password"
            );
        }

        supabaseAuthService.sendPasswordResetEmail(profile.getEmail());
    }

    public void resetPassword(ResetPasswordRequest request) {
        supabaseAuthService.resetPassword(request.getToken(), request.getNewPassword());
    }
}
