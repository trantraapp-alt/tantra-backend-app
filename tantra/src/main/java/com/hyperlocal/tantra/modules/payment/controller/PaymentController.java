package com.hyperlocal.tantra.modules.payment.controller;

import com.hyperlocal.tantra.modules.payment.dto.InitiatePaymentRequest;
import com.hyperlocal.tantra.modules.payment.dto.InitiatePaymentResponse;
import com.hyperlocal.tantra.modules.payment.dto.VerifyPaymentRequest;
import com.hyperlocal.tantra.modules.payment.entity.PaymentRecord;
import com.hyperlocal.tantra.modules.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Self-serve subscription purchase flow.
 *
 * LOCAL / TEST (app.payment.test-mode=true):
 *   1. POST /payments/initiate → returns { paymentId, orderId: "MOCK_ORDER_xxx", isTestMode: true }
 *   2. POST /payments/verify   → pass back paymentId + any razorpayPaymentId string → subscription granted
 *
 * PRODUCTION (app.payment.test-mode=false):
 *   1. POST /payments/initiate → returns { paymentId, orderId, razorpayKeyId, isTestMode: false }
 *   2. Frontend opens Razorpay checkout SDK with orderId + razorpayKeyId
 *   3. POST /payments/verify  → pass back all three Razorpay fields → HMAC verified → subscription granted
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired private PaymentService paymentService;

    /**
     * Initiate a plan purchase.
     * Body: { "planKey": "PREMIUM", "billingCycle": "MONTHLY" }
     */
    @PostMapping("/initiate")
    public ResponseEntity<InitiatePaymentResponse> initiate(
            @RequestBody InitiatePaymentRequest request,
            Authentication auth) {
        log.info("[PAYMENT] Initiate request from userId={}", auth.getName());
        return ResponseEntity.ok(paymentService.initiate(auth.getName(), request));
    }

    /**
     * Verify payment after checkout completes.
     * Test body: { "paymentId": "PAYxxxxxxxx", "razorpayOrderId": "MOCK_ORDER_xxx", "razorpayPaymentId": "TEST_PAY_001", "razorpaySignature": "any" }
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @RequestBody VerifyPaymentRequest request,
            Authentication auth) {
        log.info("[PAYMENT] Verify request from userId={}", auth.getName());
        return ResponseEntity.ok(paymentService.verify(auth.getName(), request));
    }

    /** Seller's own payment history. */
    @GetMapping("/mine")
    public ResponseEntity<List<PaymentRecord>> myPayments(Authentication auth) {
        return ResponseEntity.ok(paymentService.myPayments(auth.getName()));
    }

    /** Admin: all payments with optional status filter. */
    @GetMapping("/admin")
    public ResponseEntity<Page<PaymentRecord>> adminList(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.adminList(status, pageable));
    }
}
