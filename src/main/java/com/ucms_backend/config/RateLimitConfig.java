package com.ucms_backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Provides per-key Bucket4j rate limit buckets for auth endpoints.
 *
 * <p>Limits:
 * <ul>
 *   <li>login: 10 requests / 1 minute per IP</li>
 *   <li>register: 5 requests / 1 minute per IP</li>
 *   <li>forgot-password (IP): 5 requests / 1 minute per IP</li>
 *   <li>forgot-password (student ID): 3 requests / 10 minutes per student ID</li>
 * </ul>
 */
@Component
public class RateLimitConfig {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> forgotPasswordIpBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> forgotPasswordIdBuckets = new ConcurrentHashMap<>();

    public Bucket loginBucket(String ip) {
        return loginBuckets.computeIfAbsent(ip, k -> newBucket(10, Duration.ofMinutes(1)));
    }

    public Bucket registerBucket(String ip) {
        return registerBuckets.computeIfAbsent(ip, k -> newBucket(5, Duration.ofMinutes(1)));
    }

    public Bucket forgotPasswordIpBucket(String ip) {
        return forgotPasswordIpBuckets.computeIfAbsent(ip, k -> newBucket(5, Duration.ofMinutes(1)));
    }

    public Bucket forgotPasswordIdBucket(String studentId) {
        return forgotPasswordIdBuckets.computeIfAbsent(studentId, k -> newBucket(3, Duration.ofMinutes(10)));
    }

    private Bucket newBucket(long capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, refillDuration)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
