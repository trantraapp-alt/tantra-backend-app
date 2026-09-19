package com.hyperlocal.tantra.modules.notification.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Stores the FCM / APNs device token for a user's device.
 * One user can have multiple tokens (multiple devices / reinstalls).
 * Tokens are upserted on every app launch so stale tokens self-correct.
 */
@Entity
@Table(name = "device_tokens", indexes = {
        @Index(name = "idx_dt_user",  columnList = "user_id"),
        @Index(name = "idx_dt_token", columnList = "fcm_token", unique = true)
})
@Data
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    /** Firebase Cloud Messaging registration token from the device. */
    @Column(name = "fcm_token", nullable = false, length = 500, unique = true)
    private String fcmToken;

    /** ANDROID / IOS / WEB */
    @Column(name = "platform", length = 10)
    private String platform = "ANDROID";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
