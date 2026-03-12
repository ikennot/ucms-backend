package com.ucms_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucms_backend.config.RateLimitConfig;
import com.ucms_backend.dto.ApiResponse;
import io.github.bucket4j.Bucket;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        HttpServletRequest requestForChain = request;

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
            CachedBodyRequestWrapper wrappedRequest = new CachedBodyRequestWrapper(request);
            requestForChain = wrappedRequest;

            String studentId = extractStudentId(wrappedRequest);
            if (studentId != null && !rateLimitConfig.forgotPasswordIdBucket(studentId).tryConsume(1)) {
                rejectWithTooManyRequests(response);
                return;
            }
        }

        // Only /api/auth/* endpoints above are rate-limited; all other endpoints
        // pass through once and are not double-limited.
        filterChain.doFilter(requestForChain, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractStudentId(CachedBodyRequestWrapper request) {
        String studentIdFromBody = extractStudentIdFromBody(request.getCachedBody());
        if (studentIdFromBody != null) {
            return studentIdFromBody;
        }

        String studentIdFromParam = request.getParameter("studentId");
        return studentIdFromParam == null || studentIdFromParam.isBlank() ? null : studentIdFromParam;
    }

    private String extractStudentIdFromBody(byte[] body) {
        if (body.length == 0) {
            return null;
        }
        try {
            String studentId = objectMapper.readTree(body).path("studentId").asText(null);
            return studentId == null || studentId.isBlank() ? null : studentId;
        } catch (IOException ex) {
            return null;
        }
    }

    private void rejectWithTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(
                "RATE_LIMIT_EXCEEDED",
                "Too many requests. Please try again later.");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        private CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        private byte[] getCachedBody() {
            return cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bodyStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return bodyStream.read();
                }

                @Override
                public boolean isFinished() {
                    return bodyStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
