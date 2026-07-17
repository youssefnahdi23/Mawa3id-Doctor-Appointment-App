package com.mawa3id.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mawa3id.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A lightweight, dependency-free fixed-window rate limiter for the public
 * authentication endpoints ({@code /api/auth/login} and {@code /api/auth/register/**}).
 * It blunts brute-force / credential-stuffing by capping the number of requests per
 * client IP within a rolling window and returning {@code 429 Too Many Requests}.
 *
 * <p>State is in-memory and therefore per-instance. That is sufficient for the current
 * single-instance deployment; a shared store (e.g. Redis) would be needed if the API
 * is scaled horizontally.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String AUTH_PREFIX = "/api/auth/";

    private final boolean enabled;
    private final int capacity;
    private final long windowMs;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${mawa3id.ratelimit.enabled:true}") boolean enabled,
            @Value("${mawa3id.ratelimit.auth.capacity:10}") int capacity,
            @Value("${mawa3id.ratelimit.auth.window-seconds:60}") long windowSeconds,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.windowMs = windowSeconds * 1000L;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (enabled && isRateLimited(request) && !allow(clientKey(request))) {
            writeTooManyRequests(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Auth mutations are throttled: login, register, refresh, logout, verification and
     * password-reset. Reads ({@code GET /api/auth/me}) and CORS preflight are exempt.
     */
    private boolean isRateLimited(HttpServletRequest request) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method) || "GET".equalsIgnoreCase(method)) {
            return false;
        }
        return request.getRequestURI().startsWith(AUTH_PREFIX);
    }

    private String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /**
     * Registers a hit for the key and reports whether it is within the limit.
     * Each key owns a window that resets once {@code windowMs} has elapsed.
     */
    private boolean allow(String key) {
        long now = System.currentTimeMillis();
        // Bound memory: drop windows that have fully expired before we add a new one.
        windows.entrySet().removeIf(e -> e.getValue().isExpired(now, windowMs));
        Window window = windows.computeIfAbsent(key, k -> new Window(now));
        return window.tryAcquire(now, windowMs, capacity);
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(windowMs / 1000L));
        ApiError body = ApiError.of(HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Too many requests. Please try again later.", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }

    /** A per-key fixed window tracking the start time and request count. */
    private static final class Window {
        private long windowStart;
        private int count;

        private Window(long now) {
            this.windowStart = now;
            this.count = 0;
        }

        synchronized boolean tryAcquire(long now, long windowMs, int capacity) {
            if (now - windowStart >= windowMs) {
                windowStart = now;
                count = 0;
            }
            if (count >= capacity) {
                return false;
            }
            count++;
            return true;
        }

        synchronized boolean isExpired(long now, long windowMs) {
            return now - windowStart >= windowMs;
        }
    }
}
