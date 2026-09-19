package com.hyperlocal.tantra.modules.payment.controller;

import com.hyperlocal.tantra.modules.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Razorpay webhook receiver.
 *
 * LOCAL MODE: endpoint exists but is a no-op (PaymentService.handleWebhook returns immediately).
 *
 * PRODUCTION SETUP:
 *   1. Set app.payment.test-mode=false in application.properties.
 *   2. Set app.payment.razorpay.webhook-secret=<your_webhook_secret>.
 *   3. Register this URL in Razorpay Dashboard → Webhooks:
 *         https://<your-domain>/api/v1/webhooks/razorpay
 *      Events to subscribe: payment.captured
 *   4. Uncomment the webhook body in PaymentService.handleWebhook().
 *
 * FOR LOCAL TESTING with ngrok:
 *   - Run: ngrok http 8080
 *   - Register the ngrok URL as webhook in Razorpay test dashboard.
 *   - Webhook will call your local server with real test events.
 *
 * IMPORTANT: Always return HTTP 200. Razorpay retries on non-2xx responses,
 *            which can cause duplicate subscription grants. PaymentService is idempotent.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Autowired private PaymentService paymentService;

    /**
     * Razorpay server-to-server callback.
     * Header X-Razorpay-Signature contains the HMAC-SHA256 signature.
     */
    @PostMapping("/razorpay")
    public ResponseEntity<String> razorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        log.info("[WEBHOOK] Razorpay event received, signature present={}", signature != null);
        try {
            paymentService.handleWebhook(rawBody, signature);
        } catch (Exception e) {
            // Log but always return 200 to prevent Razorpay retries
            log.error("[WEBHOOK] Error processing Razorpay webhook: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok("OK");
    }
}
