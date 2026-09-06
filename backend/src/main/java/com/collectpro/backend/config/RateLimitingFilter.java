package com.collectpro.backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // Un seau de jetons par IP et par endpoint sensible
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        RateLimit limit = resolveLimit(method, path);
        if (limit != null) {
            String clientIp = extractClientIp(request);
            String bucketKey = limit.name() + ":" + clientIp;

            Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(limit));

            if (!bucket.tryConsume(1)) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Trop de tentatives. Veuillez réessayer dans une minute.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private RateLimit resolveLimit(String method, String path) {
        if (!"POST".equalsIgnoreCase(method)) return null;
        if (path.equals("/login")) return RateLimit.LOGIN;
        if (path.equals("/forgot-password")) return RateLimit.FORGOT_PASSWORD;
        if (path.equals("/refresh")) return RateLimit.REFRESH;
        return null;
    }

    private Bucket createBucket(RateLimit limit) {
        Bandwidth bandwidth = Bandwidth.classic(limit.capacity,
                io.github.bucket4j.Refill.intervally(limit.capacity, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private String extractClientIp(HttpServletRequest request) {
        // Derrière un reverse proxy (nginx), l'IP réelle du client est dans X-Forwarded-For
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private enum RateLimit {
        LOGIN(5),
        FORGOT_PASSWORD(3),
        REFRESH(10);

        final int capacity;

        RateLimit(int capacity) {
            this.capacity = capacity;
        }
    }
}