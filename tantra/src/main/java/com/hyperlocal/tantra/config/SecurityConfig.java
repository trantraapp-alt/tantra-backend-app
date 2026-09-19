package com.hyperlocal.tantra.config;

import com.hyperlocal.tantra.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Admin-only ────────────────────────────────────────────────────────
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ── Public read-only endpoints (must come before authenticated rules) ────
                        .requestMatchers("/api/v1/listings/flash-deals").permitAll()
                        .requestMatchers("/api/v1/business-profiles/top-sellers").permitAll()
                        .requestMatchers("/api/v1/business-profiles/directory").permitAll()
                        .requestMatchers("/api/v1/stats/**").permitAll()
                        .requestMatchers("/api/v1/msp/**").permitAll()

                        // ── Public listing browse (guests can browse; writes + nearby stay auth) ──
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/category/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/browse/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/by-seller/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/*/similar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/*").permitAll()

                        // ── Authenticated: contact reveal ─────────────────────────────────────
                        .requestMatchers("/api/v1/contacts/**").authenticated()

                        // ── Authenticated: writes + personal data ────────────────────────────
                        .requestMatchers("/api/v1/uploads/**").authenticated()
                        .requestMatchers("/api/v1/addresses/**").authenticated()
                        .requestMatchers("/api/v1/wishlist/**").authenticated()
                        .requestMatchers("/api/v1/business-profiles/**").authenticated()
                        .requestMatchers("/api/v1/notifications/**").authenticated()
                        .requestMatchers("/api/v1/listings/**").authenticated()
                        .requestMatchers("/api/v1/subscriptions/mine").authenticated()
                        .requestMatchers("/api/v1/subscriptions/mine/**").authenticated()
                        .requestMatchers("/api/v1/auth/verify-session").authenticated()

                        // ── Authenticated: payment + own data ────────────────────────────────
                        .requestMatchers("/api/v1/payments/initiate").authenticated()
                        .requestMatchers("/api/v1/payments/verify").authenticated()
                        .requestMatchers("/api/v1/payments/mine").authenticated()

                        // ── Authenticated: device token management ────────────────────────────
                        .requestMatchers("/api/v1/notifications/device-token").authenticated()

                        // ── Public: home feed, search, browse, plans, directory, promo ────────
                        .requestMatchers("/files/**").permitAll()
                        .requestMatchers("/api/v1/home/**").permitAll()
                        .requestMatchers("/api/v1/search/**").permitAll()
                        .requestMatchers("/api/v1/subscriptions/plans").permitAll()
                        .requestMatchers("/api/v1/business-profiles/directory").permitAll()
                        .requestMatchers("/api/v1/promo-cards/**").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        .requestMatchers("/api/v1/filters/**").permitAll()
                        .requestMatchers("/api/v1/**").permitAll()

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"authenticated\":false,\"message\":\"User is not logged in\"}");
                        })
                );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Trace-Id"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
