package com.ucms_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucms_backend.dto.AuthResponse;
import com.ucms_backend.exception.AppException;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class SupabaseAuthService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SupabaseAuthService.class);

    private static final String SUPABASE_ERROR_MESSAGE = "Authentication service error";
    private static final String SUPABASE_ERROR_CODE = "SUPABASE_ERROR";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String anonKey;
    // Security note: this secret is used only for outbound headers and is never logged.
    private final String serviceRoleKey;

    public SupabaseAuthService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.anon-key}") String anonKey,
            @Value("${supabase.service-role-key}") String serviceRoleKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(supabaseUrl)
                .build();
        this.anonKey = anonKey;
        this.serviceRoleKey = serviceRoleKey;
    }

    public UUID createUser(String studentId, String password) {
        String email = toUcsmLocalEmail(studentId);
        Map<String, Object> requestBody = Map.of(
                "email", email,
                "password", password,
                "email_confirm", true,
                "user_metadata", Map.of(
                        "role", "STUDENT",
                        "student_id", studentId
                )
        );

        Map<String, Object> response = executeForMap(
                restClient.post()
                        .uri("/auth/v1/admin/users")
                        .header("apikey", serviceRoleKey)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                        .body(requestBody)
        );

        Object idValue = response.get("id");
        if (!(idValue instanceof String idString)) {
            throw supabaseError();
        }

        try {
            return UUID.fromString(idString);
        } catch (IllegalArgumentException ex) {
            throw supabaseError();
        }
    }

    public AuthResponse login(String studentId, String password) {
        Map<String, Object> requestBody = Map.of(
                "email", toUcsmLocalEmail(studentId),
                "password", password
        );

        Map<String, Object> response = executeForMap(
                restClient.post()
                        .uri(uriBuilder -> uriBuilder.path("/auth/v1/token").queryParam("grant_type", "password").build())
                        .header("apikey", anonKey)
                        .body(requestBody)
        );

        Object accessToken = response.get("access_token");
        Object tokenType = response.get("token_type");
        Object expiresIn = response.get("expires_in");
        Object refreshToken = response.get("refresh_token");

        if (!(accessToken instanceof String accessTokenString)
                || !(tokenType instanceof String tokenTypeString)
                || !(expiresIn instanceof Number expiresInNumber)
                || !(refreshToken instanceof String refreshTokenString)) {
            throw supabaseError();
        }

        return AuthResponse.builder()
                .accessToken(accessTokenString)
                .tokenType(tokenTypeString)
                .expiresIn(expiresInNumber.longValue())
                .refreshToken(refreshTokenString)
                .build();
    }

    public void sendPasswordResetEmail(String email) {
        Map<String, Object> requestBody = Map.of("email", email);

        executeNoBody(
                restClient.post()
                        .uri("/auth/v1/recover")
                        .header("apikey", anonKey)
                        .body(requestBody)
        );
    }

    public void sendVerificationEmail(UUID authUserId, String email) {
        Map<String, Object> requestBody = Map.of(
                "type", "invite",
                "email", email,
                "data", Map.of("auth_user_id", authUserId.toString())
        );

        executeForMap(
                restClient.post()
                        .uri("/auth/v1/admin/generate_link")
                        .header("apikey", serviceRoleKey)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                        .body(requestBody)
        );
    }

    /**
     * Deletes a Supabase Auth user by UUID via Admin API.
     * Used as a compensating transaction if local profile save fails after user creation.
     * DELETE {supabaseUrl}/auth/v1/admin/users/{authUserId}
     * Header: Authorization: Bearer <service-role-key>
     */
    public void deleteUser(UUID authUserId) {
        try {
            restClient.delete()
                    .uri("/auth/v1/admin/users/" + authUserId)
                    .header("apikey", serviceRoleKey)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        log.error("Failed to delete Supabase user {}: {}", authUserId, errorBody);
                    })
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error("Failed to delete Supabase user {} during rollback: {}", authUserId, ex.getMessage());
        }
    }

    private Map<String, Object> executeForMap(RestClient.RequestBodySpec requestSpec) {
        try {
            String responseBody = requestSpec.retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        log.error("Supabase error {}: {}", response.getStatusCode(), errorBody);
                        throw new AppException(502, SUPABASE_ERROR_CODE, SUPABASE_ERROR_MESSAGE);
                    })
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                log.error("Supabase returned empty response body");
                throw new AppException(502, SUPABASE_ERROR_CODE, SUPABASE_ERROR_MESSAGE);
            }

            return objectMapper.readValue(responseBody, new TypeReference<>() {
            });
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Supabase call failed: {}", ex.getMessage(), ex);
            throw new AppException(502, SUPABASE_ERROR_CODE, SUPABASE_ERROR_MESSAGE);
        }
    }

    private void executeNoBody(RestClient.RequestBodySpec requestSpec) {
        try {
            requestSpec.retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        log.error("Supabase error {}: {}", response.getStatusCode(), errorBody);
                        throw new AppException(502, SUPABASE_ERROR_CODE, SUPABASE_ERROR_MESSAGE);
                    })
                    .toBodilessEntity();
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Supabase call failed: {}", ex.getMessage(), ex);
            throw new AppException(502, SUPABASE_ERROR_CODE, SUPABASE_ERROR_MESSAGE);
        }
    }

    private AppException supabaseError() {
        return new AppException(502, SUPABASE_ERROR_CODE, SUPABASE_ERROR_MESSAGE);
    }

    private String toUcsmLocalEmail(String studentId) {
        return studentId + "@ucms.local";
    }
}
