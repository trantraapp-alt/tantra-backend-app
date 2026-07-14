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
}