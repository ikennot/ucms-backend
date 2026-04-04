package com.ucms_backend.config;

import com.ucms_backend.security.RateLimitFilter;
import com.ucms_backend.security.SupabaseAuthFilter;
import com.ucms_backend.security.UnauthorizedEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configures API security and JWT decoding for Supabase auth tokens.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final SupabaseAuthFilter supabaseAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final UnauthorizedEntryPoint unauthorizedEntryPoint;

    public SecurityConfig(
            SupabaseAuthFilter supabaseAuthFilter,
            RateLimitFilter rateLimitFilter,
            UnauthorizedEntryPoint unauthorizedEntryPoint
    ) {
        this.supabaseAuthFilter = supabaseAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.unauthorizedEntryPoint = unauthorizedEntryPoint;
    }

    /**
     * Main security configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(unauthorizedEntryPoint))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/forgot-password")
                        .permitAll()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(supabaseAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
