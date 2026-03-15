package com.ucms_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucms_backend.exception.AppException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);
    private static final String STORAGE_ERROR_CODE = "STORAGE_ERROR";
    private static final String STORAGE_ERROR_MESSAGE = "Storage service error";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String supabaseUrl;
    // Security note: this secret is used only for outbound headers and is never logged.
    private final String serviceRoleKey;
    private final String bucket;
    private final int signedUrlExpirySeconds;

    public SupabaseStorageService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.storage.bucket}") String bucket,
            @Value("${supabase.storage.signed-url-expiry:3600}") int signedUrlExpirySeconds
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(supabaseUrl)
                .build();
        this.supabaseUrl = supabaseUrl;
        this.serviceRoleKey = serviceRoleKey;
        this.bucket = bucket;
        this.signedUrlExpirySeconds = signedUrlExpirySeconds;
    }

    public void uploadFile(String storagePath, byte[] content, String mimeType) {
        try {
            restClient.put()
                    .uri("/storage/v1/object/" + bucket + "/" + storagePath)
                    .header("apikey", serviceRoleKey)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .body(content)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        log.error("Supabase storage error {}: {}", response.getStatusCode(), errorBody);
                        throw new AppException(502, STORAGE_ERROR_CODE, STORAGE_ERROR_MESSAGE);
                    })
                    .toBodilessEntity();
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Supabase storage upload failed: {}", ex.getMessage(), ex);
            throw new AppException(502, STORAGE_ERROR_CODE, STORAGE_ERROR_MESSAGE);
        }
    }

    public String generateSignedUrl(String storagePath) {
        try {
            log.info("generateSignedUrl called for path: {}", storagePath);
            Map<String, Object> requestBody = Map.of("expiresIn", signedUrlExpirySeconds);
            String responseBody = restClient.post()
                    .uri("/storage/v1/object/sign/" + bucket + "/" + storagePath)
                    .header("apikey", serviceRoleKey)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        log.error("Supabase storage error {}: {}", response.getStatusCode(), errorBody);
                        throw new AppException(502, STORAGE_ERROR_CODE, STORAGE_ERROR_MESSAGE);
                    })
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new AppException(502, STORAGE_ERROR_CODE, STORAGE_ERROR_MESSAGE);
            }

            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<>() {
            });
            Object signedUrlValue = response.get("signedURL");
            if (signedUrlValue == null) {
                signedUrlValue = response.get("signedUrl");
            }

            if (!(signedUrlValue instanceof String signedUrl)) {
                throw new AppException(502, STORAGE_ERROR_CODE, STORAGE_ERROR_MESSAGE);
            }

            if (signedUrl.startsWith("http")) {
                // Full URL returned — ensure /storage/v1 is present
                if (!signedUrl.contains("/storage/v1/")) {
                    signedUrl = signedUrl.replace("/object/sign/", "/storage/v1/object/sign/");
                }
                return signedUrl;
            }

            // Relative URL returned — strip any leading /storage/v1 before appending
            if (signedUrl.startsWith("/storage/v1")) {
                signedUrl = signedUrl.substring("/storage/v1".length());
            }
            return supabaseUrl + "/storage/v1" + signedUrl;
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Supabase signed URL generation failed for path {}: {}", storagePath, ex.getMessage(), ex);
            throw new AppException(502, STORAGE_ERROR_CODE, STORAGE_ERROR_MESSAGE);
        }
    }
}
