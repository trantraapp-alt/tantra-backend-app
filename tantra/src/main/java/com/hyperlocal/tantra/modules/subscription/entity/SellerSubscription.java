package com.hyperlocal.tantra.modules.subscription.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @deprecated Renamed to {@link UserSubscription}. Table renamed seller_subscriptions → user_subscriptions
 * via V3 migration. This class is kept only to avoid breaking any stale import; remove after confirming
 * no references remain. @Entity removed so Hibernate does not try to recreate the old table.
 */
@Deprecated
@Table(name = "seller_subscriptions", indexes = {
        @Index(name = "idx_sub_user",        columnList = "user_id"),
        @Index(name = "idx_sub_user_active",  columnList = "user_id, status, expires_at"),
        @Index(name = "idx_sub_plan",         columnList = "plan_id"),
        @Index(name = "idx_sub_expires",      columnList = "expires_at, status")
})
@Data
public class SellerSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public opaque id: SB + 8 chars. */
    @Column(name = "subscription_id", unique = true, nullable = false, length = 20)
    private String subscriptionId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "plan_id", nullable = false)
    private Integer planId;

    /** PENDING / ACTIVE / EXPIRED / CANCELLED */
    @Column(name = "status", nullable = false, length = 15)
    private String status = "PENDING";

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** External payment gateway order / transaction ref (Razorpay, Stripe, etc.). */
    @Column(name = "payment_ref", length = 100)
    private String paymentRef;

    @Column(name = "payment_gateway", length = 40)
    private String paymentGateway;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = false;

    /** Admin who granted or last modified this subscription. */
    @Column(name = "granted_by", length = 20)
    private String grantedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
