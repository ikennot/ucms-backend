package com.ucms_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Provides the JwtDecoder bean separately to avoid circular dependency with SecurityConfig.
 * Explicitly supports ES256 (Supabase default JWT algorithm).
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${supabase.jwks-uri:https://icosjzuwekgilmmxgqwr.supabase.co/auth/v1/keys}") String jwksUri
    ) {
        return NimbusJwtDecoder.withJwkSetUri(jwksUri)
                .jwsAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.ES256)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
