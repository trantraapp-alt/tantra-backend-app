package com.hyperlocal.tantra.modules.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false, length = 20)
    private String userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "mobile_number", unique = true, nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "app_usage_role", nullable = false, length = 20)
    private String appUsageRole;

    @Column(name = "reset_otp", length = 6)
    private String resetOtp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "preferred_language", nullable = false, length = 2)
    private String preferredLanguage = "EN";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Admin-managed fields ──────────────────────────────────────────────────

    /** true = account suspended by admin; JWT filter returns 403 on every request. */
    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;

    /** Timestamp when admin blocked this account. */
    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    /** Reason selected by admin: SPAM_LISTINGS / FAKE_PROFILE / ABUSIVE_BEHAVIOR / OTHER */
    @Column(name = "blocked_reason", length = 200)
    private String blockedReason;

    /** Updated on every successful login — used for admin activity view. */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Timestamp of the user's most recent authenticated API call.
     * Updated automatically via LastActiveFilter with a 15-min cooldown (cache guard).
     * Used to show "Active X ago" on seller/buyer profiles.
     * Null for users who registered but never made an API call after V21 migration.
     */
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;
}