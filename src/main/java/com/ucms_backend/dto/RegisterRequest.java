package com.ucms_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    private String studentId;

    @NotBlank
    private String name;

    @NotBlank
    @Size(min = 8)
    private String password;

    private String course;
    private Integer yearLevel;
}
