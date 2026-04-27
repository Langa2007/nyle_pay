package com.nyle.nylepay.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiting filter using Bucket4j's token-bucket algorithm.
 *
 * Configuration:
 *   rate.limit.requests-per-minute — max requests per minute per IP (default 100)
 *   rate.limit.enabled — enables/disables rate limiting (default true)
 *
 * Stricter limits are applied to auth and payment endpoints:
 *   /api/auth/**        — 20 requests/minute (login brute-force protection)
 *   /api/payments/**     — 30 requests/minute (payment abuse prevention)
 *   /api/payments/local/**  — 15 requests/minute (local payment abuse)
 *   Everything else      — configurable via rate.limit.requests-per-minute
 *
 * Webhook endpoints are excluded from rate limiting since they come
 * from trusted payment providers (Safaricom, Flutterwave, Stripe, Paystack).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    @Value("${rate.limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${rate.limit.requests-per-minute:100}")
    private int requestsPerMinute;

    // In-memory buckets keyed by IP + path category
    // In production with multiple instances, replace with Redis-backed Bucket4j
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Skip rate limiting for webhooks (trusted provider callbacks)
        if (path.contains("/webhook/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String bucketKey = clientIp + ":" + getPathCategory(path);
        int limit = getLimitForPath(path);

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(limit));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded: ip={} path={} limit={}/min", clientIp, path, limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Too many requests. Please slow down and try again in a minute.\"}"
            );
        }
    }

    private Bucket createBucket(int tokensPerMinute) {
        return Bucket.builder()
            .addLimit(Bandwidth.simple(tokensPerMinute, Duration.ofMinutes(1)))
            .build();
    }

    private int getLimitForPath(String path) {
        if (path.startsWith("/api/auth/")) {
            return 20; // Strict: login, register, password reset
        }
        if (path.startsWith("/api/payments/local/")) {
            return 15; // Very strict: local payments
        }
        if (path.startsWith("/api/payments/")) {
            return 30; // Strict: deposits, withdrawals
        }
        if (path.startsWith("/api/kyc/")) {
            return 10; // KYC submissions
        }
        return requestsPerMinute; // Default
    }

    private String getPathCategory(String path) {
        if (path.startsWith("/api/auth/")) return "auth";
        if (path.startsWith("/api/payments/local/")) return "local";
        if (path.startsWith("/api/payments/")) return "payments";
        if (path.startsWith("/api/kyc/")) return "kyc";
        return "general";
    }

    private String getClientIp(HttpServletRequest request) {
        // Support for reverse proxies (Nginx, Cloudflare, etc.)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
