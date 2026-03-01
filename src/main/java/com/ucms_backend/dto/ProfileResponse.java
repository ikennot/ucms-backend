package com.ucms_backend.dto;

import com.ucms_backend.model.entity.Profile;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private UUID authUserId;
    private String studentId;
    private String name;
    private String email;
    private boolean emailVerified;
    private String course;
    private Integer yearLevel;
    private String role;
    private LocalDateTime createdAt;

    public static ProfileResponse from(Profile profile) {
        return ProfileResponse.builder()
                .authUserId(profile.getAuthUserId())
                .studentId(profile.getStudentId())
                .name(profile.getName())
                .email(profile.getEmail())
                .emailVerified(profile.isEmailVerified())
                .course(profile.getCourse())
                .yearLevel(profile.getYearLevel())
                .role(profile.getRole())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
