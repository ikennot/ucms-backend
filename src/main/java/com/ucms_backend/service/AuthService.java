package com.ucms_backend.service;

import com.ucms_backend.dto.AuthResponse;
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
                supabaseAuthService.sendPasswordResetEmail(profile.getEmail());
            }
        });
    }

}
