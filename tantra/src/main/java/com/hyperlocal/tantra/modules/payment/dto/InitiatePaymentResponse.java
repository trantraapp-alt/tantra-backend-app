package com.hyperlocal.tantra.modules.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitiatePaymentResponse {

    /** Internal payment record id (PAY + 8 chars). Pass back in verify call. null for free plans. */
    private String paymentId;

    /** Order id from gateway (MOCK_ORDER_xxx in test mode, order_xxx in production). null for free plans. */
    private String orderId;

    private BigDecimal amount;
    private String currency;
    private String planKey;
    private Integer durationMonths;

    /**
     * true  = local/test mode — use the mock verify endpoint, no real money.
     * false = production Razorpay — open Razorpay checkout with this orderId.
     */
    private Boolean isTestMode;

    /**
     * Razorpay key_id to initialise the Razorpay JS/React Native SDK.
     * null in test mode and for free plans.
     */
    private String razorpayKeyId;
}
