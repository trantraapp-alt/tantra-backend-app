package com.hyperlocal.tantra.modules.payment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable record of every payment attempt.
 * Status lifecycle: PENDING → CAPTURED (success) | FAILED | REFUNDED
 *
 * For local/test mode the gatewayOrderId is prefixed "MOCK_".
 * For production Razorpay the real order_id and payment_id are stored here.
 */
@Entity
@Table(name = "payment_records", indexes = {
        @Index(name = "idx_pay_user",    columnList = "user_id"),
        @Index(name = "idx_pay_status",  columnList = "status"),
        @Index(name = "idx_pay_gateway", columnList = "gateway_order_id")
})
@Data
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public opaque id: PAY + 8 chars. */
    @Column(name = "payment_id", unique = true, nullable = false, length = 20)
    private String paymentId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "plan_key", nullable = false, length = 30)
    private String planKey;

    /** MONTHLY / YEARLY */
    @Column(name = "billing_cycle", nullable = false, length = 10)
    private String billingCycle;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 5, nullable = false)
    private String currency = "INR";

    /**
     * Gateway order reference.
     * Local/mock: "MOCK_ORDER_<paymentId>"
     * Razorpay (production): "order_XXXXXXXXXXXXXXXX"
     */
    @Column(name = "gateway_order_id", length = 100)
    private String gatewayOrderId;

    /**
     * Gateway payment reference (set on CAPTURED).
     * Local/mock: "MOCK_PAY_<paymentId>"
     * Razorpay (production): "pay_XXXXXXXXXXXXXXXX"
     */
    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    /** Razorpay HMAC-SHA256 signature for verification (production only). */
    @Column(name = "gateway_signature", length = 200)
    private String gatewaySignature;

    /** PENDING / CAPTURED / FAILED / REFUNDED */
    @Column(name = "status", nullable = false, length = 15)
    private String status = "PENDING";

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Set when status → CAPTURED. */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
