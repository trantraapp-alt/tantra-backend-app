package com.hyperlocal.tantra.modules.payment.dto;

import lombok.Data;

@Data
public class VerifyPaymentRequest {

    /** Internal payment id returned by /initiate */
    private String paymentId;

    /**
     * Razorpay order id (from initiate response).
     * Also accepted for test mode (pass the MOCK_ORDER_xxx value).
     */
    private String razorpayOrderId;

    /**
     * Razorpay payment id returned by the SDK after user completes payment.
     * For test mode, pass any non-null string (e.g. "TEST_PAY_001").
     */
    private String razorpayPaymentId;

    /**
     * HMAC-SHA256 signature from Razorpay SDK.
     * For test mode, pass any non-null string — signature check is skipped.
     */
    private String razorpaySignature;
}
