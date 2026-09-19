package com.hyperlocal.tantra.common.web;

import com.hyperlocal.tantra.config.CacheConfig;
import com.hyperlocal.tantra.modules.auth.service.LastActiveService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Automatically tracks when a user was last active — no frontend "/ping" endpoint needed.
 *
 * Strategy (optimized):
 *  1. Runs on every authenticated request (after JwtAuthenticationFilter sets SecurityContext).
 *  2. Cache guard: skips DB write if this userId was updated within the last 15 minutes.
 *  3. Fire-and-forget: DB write happens on a background thread — zero latency added to the request.
 *
 * Order is positive (runs AFTER Spring Security's -100 order), so SecurityContext is already set.
 */
@Component
@Order(10)
public class LastActiveFilter extends OncePerRequestFilter {

    /** Cooldown window — one DB write per user per this many minutes. */
    private static final int COOLDOWN_MINUTES = 15;

    /** Single background thread — last_active writes are low-priority, serial is fine. */
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "last-active-writer");
        t.setDaemon(true);
        return t;
    });

    private final CacheManager cacheManager;
    private final LastActiveService lastActiveService;

    public LastActiveFilter(CacheManager cacheManager, LastActiveService lastActiveService) {
        this.cacheManager = cacheManager;
        this.lastActiveService = lastActiveService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Let the request proceed first — tracking must never block it
        chain.doFilter(request, response);

        // After response: check if authenticated user needs a last_active update
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return;

            String principal = auth.getName(); // mobile number (CustomUserDetailsService)
            if (principal == null || principal.isBlank()) return;

            Cache cache = cacheManager.getCache(CacheConfig.LAST_ACTIVE);
            if (cache == null) return;

            // Cache stores the timestamp of the last DB write — check cooldown
            LocalDateTime lastPing = cache.get(principal, LocalDateTime.class);
            if (lastPing != null && lastPing.isAfter(LocalDateTime.now().minusMinutes(COOLDOWN_MINUTES))) {
                return; // Still within cooldown — skip
            }

            // Mark in cache immediately (before async write) to prevent concurrent duplicate writes
            LocalDateTime now = LocalDateTime.now();
            cache.put(principal, now);

            // Fire-and-forget DB write — doesn't affect response latency
            final String mobileFinal = principal;
            final LocalDateTime nowFinal = now;
            WRITER.submit(() -> {
                try {
                    lastActiveService.touch(mobileFinal, nowFinal);
                } catch (Exception ignored) {
                    // Silently ignore — stale last_active is acceptable
                }
            });

        } catch (Exception ignored) {
            // Never let tracking errors affect the main request
        }
    }
}
