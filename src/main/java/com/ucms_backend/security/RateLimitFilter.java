package com.ucms_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucms_backend.config.RateLimitConfig;
import com.ucms_backend.dto.ApiResponse;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies IP-based (and student-ID-based for forgot-password) rate limiting
 * on auth endpoints. Returns 429 with ApiResponse structure when limit exceeded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String FORGOT_PASSWORD_PATH = "/api/auth/forgot-password";

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitConfig rateLimitConfig, ObjectMapper objectMapper) {
        this.rateLimitConfig = rateLimitConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = resolveClientIp(request);

        if (LOGIN_PATH.equals(path)) {
            if (!rateLimitConfig.loginBucket(ip).tryConsume(1)) {
                rejectWithTooManyRequests(response);
                return;
            }
        } else if (REGISTER_PATH.equals(path)) {
            if (!rateLimitConfig.registerBucket(ip).tryConsume(1)) {
                rejectWithTooManyRequests(response);
                return;
            }
        } else if (FORGOT_PASSWORD_PATH.equals(path)) {
            if (!rateLimitConfig.forgotPasswordIpBucket(ip).tryConsume(1)) {
                rejectWithTooManyRequests(response);
                return;
            }
            String studentId = extractStudentId(request);
            if (studentId != null && !rateLimitConfig.forgotPasswordIdBucket(studentId).tryConsume(1)) {
                rejectWithTooManyRequests(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractStudentId(HttpServletRequest request) {
        // Read studentId from query param or cached body — for POST JSON bodies
        // we rely on the request param fallback; full body parsing is handled by
        // the controller. For rate limiting purposes, IP-only is the primary guard;
        // student ID keying is best-effort here.
        return request.getParameter("studentId");
    }

    private void rejectWithTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(
                "RATE_LIMIT_EXCEEDED",
                "Too many requests. Please try again later.");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
