package com.hyperlocal.tantra.modules.payment.service;

import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.audit.service.AuditService;
import com.hyperlocal.tantra.modules.payment.dto.InitiatePaymentRequest;
import com.hyperlocal.tantra.modules.payment.dto.InitiatePaymentResponse;
import com.hyperlocal.tantra.modules.payment.dto.VerifyPaymentRequest;
import com.hyperlocal.tantra.modules.payment.entity.PaymentRecord;
import com.hyperlocal.tantra.modules.payment.repository.PaymentRepository;
import com.hyperlocal.tantra.modules.subscription.dto.GrantSubscriptionRequest;
import com.hyperlocal.tantra.modules.subscription.entity.SubscriptionPlan;
import com.hyperlocal.tantra.modules.subscription.repository.SubscriptionPlanRepository;
import com.hyperlocal.tantra.modules.subscription.service.SubscriptionService;
import com.hyperlocal.tantra.utils.IdGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/*
 * ──────────────────────────────────────────────────────────────────────────────
 * PRODUCTION RAZORPAY — uncomment when going live:
 *
 *   1. Add dependency in pom.xml (already commented there).
 *   2. Set properties in application.properties (already commented there).
 *   3. Uncomment the import and all // RAZORPAY sections below.
 *   4. Delete / comment the // LOCAL MOCK sections.
 * ──────────────────────────────────────────────────────────────────────────────
 */

// import com.razorpay.Order;
// import com.razorpay.RazorpayClient;
// import com.razorpay.RazorpayException;
// import com.razorpay.Utils;
// import org.json.JSONObject;

/**
 * Payment orchestration service.
 *
 * Plan duration drives everything — no separate billingCycle concept.
 * Each plan has its own price and durationMonths.
 *
 * FREE plan (price = 0): subscription is granted instantly, no payment record created.
 *
 * LOCAL MODE  (app.payment.test-mode=true):
 *   - initiate() creates a MOCK_ORDER, no real money involved.
 *   - verify()   skips HMAC check, immediately marks CAPTURED, grants subscription.
 *
 * PRODUCTION MODE (app.payment.test-mode=false):
 *   - initiate() creates a real Razorpay order via REST API.
 *   - verify()   validates the HMAC-SHA256 signature before granting subscription.
 *   - Webhook at POST /api/v1/webhooks/razorpay is the authoritative capture path.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SubscriptionPlanRepository planRepository;
    @Autowired private SubscriptionService subscriptionService;
    @Autowired private AuditService auditService;

    @Value("${app.payment.test-mode:true}")
    private boolean testMode;

    @Value("${app.payment.razorpay.key-id:}")
    private String razorpayKeyId;

    // @Value("${app.payment.razorpay.key-secret:}")   // RAZORPAY — uncomment for production
    // private String razorpayKeySecret;

    // ─────────────────────────────────────────────────────────────────────────
    // INITIATE PAYMENT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Step 1 — User taps "Select Plan".
     * FREE plan: grants subscription instantly, returns amount=0 with no orderId.
     * Paid plan: creates a PaymentRecord (PENDING) and returns order details to the frontend.
     */
    @Transactional
    public InitiatePaymentResponse initiate(String userId, InitiatePaymentRequest req) {
        log.info("[PAYMENT] Initiate — userId={} plan={}", userId, req.getPlanKey());

        SubscriptionPlan plan = planRepository.findByPlanKey(req.getPlanKey().toUpperCase())
                .orElseThrow(() -> new LocalizedException(
                        "Plan not found: " + req.getPlanKey(),
                        "प्लान नहीं मिला: " + req.getPlanKey()));

        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new LocalizedException("This plan is currently unavailable.",
                    "यह प्लान अभी उपलब्ध नहीं है।");
        }

        // Free plan — grant immediately, no payment needed
        if (plan.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            return grantFreePlan(userId, plan);
        }

        String paymentId = generateUniquePaymentId();

        if (testMode) {
            return initiateLocal(userId, plan, paymentId);
        } else {
            return initiateRazorpay(userId, plan, paymentId);
        }
    }

    // ── Free plan fast-path ──────────────────────────────────────────────────
    private InitiatePaymentResponse grantFreePlan(String userId, SubscriptionPlan plan) {
        log.info("[PAYMENT] Free plan — granting {} directly to user={}", plan.getPlanKey(), userId);

        GrantSubscriptionRequest grantReq = new GrantSubscriptionRequest();
        grantReq.setUserId(userId);
        grantReq.setPlanKey(plan.getPlanKey());
        grantReq.setDurationDays(plan.getDurationMonths() * 30);
        grantReq.setPaymentGateway("FREE");
        grantReq.setAutoRenew(false);
        grantReq.setNotes("Free plan activation");
        subscriptionService.grantSubscription(grantReq, "SYSTEM");

        auditService.record("FREE_PLAN_ACTIVATED", "PAYMENT", userId, userId,
                Map.of("planKey", plan.getPlanKey()));

        InitiatePaymentResponse response = new InitiatePaymentResponse();
        response.setPaymentId(null);
        response.setOrderId(null);
        response.setAmount(BigDecimal.ZERO);
        response.setCurrency("INR");
        response.setPlanKey(plan.getPlanKey());
        response.setDurationMonths(plan.getDurationMonths());
        response.setIsTestMode(false);
        response.setRazorpayKeyId(null);
        return response;
    }

    // ── LOCAL MOCK initiate ──────────────────────────────────────────────────
    private InitiatePaymentResponse initiateLocal(
            String userId, SubscriptionPlan plan, String paymentId) {

        String mockOrderId = "MOCK_ORDER_" + paymentId;

        PaymentRecord record = new PaymentRecord();
        record.setPaymentId(paymentId);
        record.setUserId(userId);
        record.setPlanKey(plan.getPlanKey());
        record.setBillingCycle(plan.getDurationMonths() + "M");
        record.setAmount(plan.getPrice());
        record.setCurrency("INR");
        record.setGatewayOrderId(mockOrderId);
        record.setStatus("PENDING");
        paymentRepository.save(record);

        log.info("[PAYMENT][TEST] Created mock order {} for user={} plan={}", mockOrderId, userId, plan.getPlanKey());

        InitiatePaymentResponse response = new InitiatePaymentResponse();
        response.setPaymentId(paymentId);
        response.setOrderId(mockOrderId);
        response.setAmount(plan.getPrice());
        response.setCurrency("INR");
        response.setPlanKey(plan.getPlanKey());
        response.setDurationMonths(plan.getDurationMonths());
        response.setIsTestMode(true);
        response.setRazorpayKeyId(null);
        return response;
    }

    // ── RAZORPAY production initiate (commented — uncomment for production) ──
    private InitiatePaymentResponse initiateRazorpay(
            String userId, SubscriptionPlan plan, String paymentId) {

        // RAZORPAY — uncomment this entire block for production:
        //
        // try {
        //     RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        //
        //     JSONObject orderReq = new JSONObject();
        //     orderReq.put("amount", plan.getPrice().multiply(BigDecimal.valueOf(100)).intValue()); // paise
        //     orderReq.put("currency", "INR");
        //     orderReq.put("receipt", paymentId);
        //     orderReq.put("notes", new JSONObject()
        //             .put("userId", userId)
        //             .put("planKey", plan.getPlanKey())
        //             .put("durationMonths", plan.getDurationMonths()));
        //
        //     Order rzpOrder = client.orders.create(orderReq);
        //     String rzpOrderId = rzpOrder.get("id");
        //
        //     PaymentRecord record = new PaymentRecord();
        //     record.setPaymentId(paymentId);
        //     record.setUserId(userId);
        //     record.setPlanKey(plan.getPlanKey());
        //     record.setBillingCycle(plan.getDurationMonths() + "M");
        //     record.setAmount(plan.getPrice());
        //     record.setCurrency("INR");
        //     record.setGatewayOrderId(rzpOrderId);
        //     record.setStatus("PENDING");
        //     paymentRepository.save(record);
        //
        //     log.info("[PAYMENT][PROD] Created Razorpay order {} for user={}", rzpOrderId, userId);
        //
        //     InitiatePaymentResponse response = new InitiatePaymentResponse();
        //     response.setPaymentId(paymentId);
        //     response.setOrderId(rzpOrderId);
        //     response.setAmount(plan.getPrice());
        //     response.setCurrency("INR");
        //     response.setPlanKey(plan.getPlanKey());
        //     response.setDurationMonths(plan.getDurationMonths());
        //     response.setIsTestMode(false);
        //     response.setRazorpayKeyId(razorpayKeyId);
        //     return response;
        //
        // } catch (RazorpayException e) {
        //     log.error("[PAYMENT][PROD] Razorpay order creation failed: {}", e.getMessage(), e);
        //     throw new LocalizedException("Payment gateway error. Please try again.",
        //             "पेमेंट गेटवे में समस्या। कृपया पुनः प्रयास करें।");
        // }

        throw new LocalizedException(
                "Production payment not configured. Set app.payment.test-mode=true for local testing.",
                "प्रोडक्शन पेमेंट कॉन्फ़िगर नहीं है।");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY PAYMENT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Step 2 — Frontend calls this after user completes payment in Razorpay checkout.
     * Verifies signature (production) or skips check (test mode).
     * Marks payment CAPTURED and grants the subscription.
     */
    @Transactional
    public Map<String, Object> verify(String userId, VerifyPaymentRequest req) {
        log.info("[PAYMENT] Verify — userId={} paymentId={}", userId, req.getPaymentId());

        PaymentRecord record = paymentRepository.findByPaymentId(req.getPaymentId())
                .orElseThrow(() -> new LocalizedException(
                        "Payment record not found.", "पेमेंट रिकॉर्ड नहीं मिला।"));

        if (!record.getUserId().equals(userId)) {
            throw new LocalizedException("Unauthorized payment verification.",
                    "अनधिकृत पेमेंट सत्यापन।");
        }

        if ("CAPTURED".equals(record.getStatus())) {
            log.warn("[PAYMENT] Already captured — paymentId={}", req.getPaymentId());
            return Map.of("message", "Already activated.", "planKey", record.getPlanKey());
        }

        if (!"PENDING".equals(record.getStatus())) {
            throw new LocalizedException(
                    "Payment is in status " + record.getStatus() + " and cannot be verified.",
                    "पेमेंट स्थिति " + record.getStatus() + " है, सत्यापित नहीं किया जा सकता।");
        }

        if (testMode) {
            verifyLocal(record, req);
        } else {
            verifyRazorpay(record, req);
        }

        // Look up plan to get durationMonths
        SubscriptionPlan plan = planRepository.findByPlanKey(record.getPlanKey())
                .orElseThrow(() -> new LocalizedException(
                        "Plan not found: " + record.getPlanKey(), "प्लान नहीं मिला।"));

        GrantSubscriptionRequest grantReq = new GrantSubscriptionRequest();
        grantReq.setUserId(record.getUserId());
        grantReq.setPlanKey(record.getPlanKey());
        grantReq.setDurationDays(plan.getDurationMonths() * 30);
        grantReq.setPaymentRef(record.getGatewayPaymentId());
        grantReq.setPaymentGateway(testMode ? "MOCK" : "RAZORPAY");
        grantReq.setAutoRenew(false);
        grantReq.setNotes("Self-serve purchase via " + (testMode ? "test" : "Razorpay"));
        subscriptionService.grantSubscription(grantReq, "SYSTEM");

        auditService.record("PAYMENT_CAPTURED", "PAYMENT", record.getPaymentId(), userId,
                Map.of("planKey", record.getPlanKey(), "amount", record.getAmount().toString()));

        log.info("[PAYMENT] Payment {} captured — plan {} granted to user {}",
                record.getPaymentId(), record.getPlanKey(), userId);

        return Map.of(
                "message", Map.of("en", "Plan activated!", "hi", "प्लान सक्रिय हो गया!"),
                "planKey", record.getPlanKey(),
                "durationMonths", plan.getDurationMonths());
    }

    // ── LOCAL MOCK verify ────────────────────────────────────────────────────
    private void verifyLocal(PaymentRecord record, VerifyPaymentRequest req) {
        record.setGatewayPaymentId("MOCK_PAY_" + record.getPaymentId());
        record.setGatewaySignature("MOCK_SIG");
        record.setStatus("CAPTURED");
        record.setPaidAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(record);
        log.info("[PAYMENT][TEST] Mock verify success for paymentId={}", record.getPaymentId());
    }

    // ── RAZORPAY production verify (commented — uncomment for production) ───
    private void verifyRazorpay(PaymentRecord record, VerifyPaymentRequest req) {
        // RAZORPAY — uncomment this block for production:
        //
        // try {
        //     String payload = req.getRazorpayOrderId() + "|" + req.getRazorpayPaymentId();
        //     boolean valid = Utils.verifyPaymentSignature(
        //             new JSONObject()
        //                 .put("razorpay_order_id",   req.getRazorpayOrderId())
        //                 .put("razorpay_payment_id", req.getRazorpayPaymentId())
        //                 .put("razorpay_signature",  req.getRazorpaySignature()),
        //             razorpayKeySecret);
        //
        //     if (!valid) {
        //         record.setStatus("FAILED");
        //         record.setFailureReason("Invalid signature");
        //         record.setUpdatedAt(LocalDateTime.now());
        //         paymentRepository.save(record);
        //         throw new LocalizedException("Payment verification failed — invalid signature.",
        //                 "पेमेंट सत्यापन विफल — अमान्य हस्ताक्षर।");
        //     }
        //
        //     record.setGatewayPaymentId(req.getRazorpayPaymentId());
        //     record.setGatewaySignature(req.getRazorpaySignature());
        //     record.setStatus("CAPTURED");
        //     record.setPaidAt(LocalDateTime.now());
        //     record.setUpdatedAt(LocalDateTime.now());
        //     paymentRepository.save(record);
        //
        // } catch (RazorpayException e) {
        //     log.error("[PAYMENT][PROD] Signature verification error: {}", e.getMessage(), e);
        //     throw new LocalizedException("Payment verification error.", "पेमेंट सत्यापन में त्रुटि।");
        // }

        throw new LocalizedException(
                "Production payment not configured.",
                "प्रोडक्शन पेमेंट कॉन्फ़िगर नहीं है।");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WEBHOOK (Razorpay server-to-server — authoritative capture path)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called by Razorpay server when payment.captured event fires.
     *
     * LOCAL: no-op stub.
     * PRODUCTION: uncomment, validate HMAC, look up plan by planKey on PaymentRecord,
     *             call grantSubscription with plan.getDurationMonths() * 30.
     *
     * Setup for local webhook testing (optional):
     *   1. Install ngrok: https://ngrok.com/download
     *   2. Run: ngrok http 8080
     *   3. Register the ngrok URL + /api/v1/webhooks/razorpay in Razorpay test dashboard.
     */
    @Transactional
    public void handleWebhook(String rawBody, String razorpaySignatureHeader) {
        if (testMode) {
            log.debug("[PAYMENT][TEST] Webhook received but test-mode=true — ignoring");
            return;
        }

        // RAZORPAY WEBHOOK — uncomment this entire block for production:
        //
        // try {
        //     boolean valid = Utils.verifyWebhookSignature(rawBody, razorpaySignatureHeader, webhookSecret);
        //     if (!valid) {
        //         log.warn("[PAYMENT][PROD] Webhook signature invalid — rejected");
        //         return;
        //     }
        //
        //     JSONObject payload   = new JSONObject(rawBody);
        //     String event         = payload.getString("event");
        //     if (!"payment.captured".equals(event)) return;
        //
        //     JSONObject paymentObj = payload
        //             .getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        //
        //     String rzpOrderId   = paymentObj.getString("order_id");
        //     String rzpPaymentId = paymentObj.getString("id");
        //
        //     PaymentRecord record = paymentRepository.findByGatewayOrderId(rzpOrderId).orElse(null);
        //     if (record == null) {
        //         log.warn("[PAYMENT][PROD] Webhook: no PaymentRecord for order {}", rzpOrderId);
        //         return;
        //     }
        //     if ("CAPTURED".equals(record.getStatus())) return; // idempotent
        //
        //     record.setGatewayPaymentId(rzpPaymentId);
        //     record.setStatus("CAPTURED");
        //     record.setPaidAt(LocalDateTime.now());
        //     record.setUpdatedAt(LocalDateTime.now());
        //     paymentRepository.save(record);
        //
        //     SubscriptionPlan plan = planRepository.findByPlanKey(record.getPlanKey()).orElseThrow();
        //     GrantSubscriptionRequest grantReq = new GrantSubscriptionRequest();
        //     grantReq.setUserId(record.getUserId());
        //     grantReq.setPlanKey(record.getPlanKey());
        //     grantReq.setDurationDays(plan.getDurationMonths() * 30);
        //     grantReq.setPaymentRef(rzpPaymentId);
        //     grantReq.setPaymentGateway("RAZORPAY");
        //     grantReq.setAutoRenew(false);
        //     grantReq.setNotes("Auto-captured via Razorpay webhook");
        //     subscriptionService.grantSubscription(grantReq, "WEBHOOK");
        //
        //     log.info("[PAYMENT][PROD] Webhook captured order={} pay={} user={}",
        //             rzpOrderId, rzpPaymentId, record.getUserId());
        //
        // } catch (Exception e) {
        //     log.error("[PAYMENT][PROD] Webhook processing error: {}", e.getMessage(), e);
        //     // Do NOT re-throw — Razorpay retries on non-2xx; always return 200 after logging
        // }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN / HISTORY
    // ─────────────────────────────────────────────────────────────────────────

    public List<PaymentRecord> myPayments(String userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<PaymentRecord> adminList(String status, Pageable pageable) {
        return paymentRepository.findAll(status, pageable);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String generateUniquePaymentId() {
        String id;
        do {
            id = IdGeneratorUtil.generateId("PAY");
        } while (paymentRepository.findByPaymentId(id).isPresent());
        return id;
    }
}
