package com.hyperlocal.tantra.modules.subscription.controller;

import com.hyperlocal.tantra.modules.subscription.dto.GrantSubscriptionRequest;
import com.hyperlocal.tantra.modules.subscription.dto.SubscriptionResponse;
import com.hyperlocal.tantra.modules.subscription.entity.UserSubscription;
import com.hyperlocal.tantra.modules.subscription.service.SubscriptionService;
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

import java.util.Map;

/**
 * Admin-only subscription management. Grant, revoke, and view all subscriptions.
 * All endpoints require ROLE_ADMIN via SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/admin/subscriptions")
public class AdminSubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(AdminSubscriptionController.class);

    @Autowired private SubscriptionService subscriptionService;

    /** Grant a plan to a seller. Creates a new ACTIVE subscription, cancelling any existing one. */
    @PostMapping("/grant")
    public ResponseEntity<SubscriptionResponse> grant(
            @RequestBody GrantSubscriptionRequest request,
            Authentication auth) {
        log.info("[ADMIN] {} granting subscription: {}", auth.getName(), request);
        return ResponseEntity.ok(subscriptionService.grantSubscription(request, auth.getName()));
    }

    /** Revoke an active subscription by subscriptionId. */
    @PostMapping("/{subscriptionId}/revoke")
    public ResponseEntity<?> revoke(
            @PathVariable String subscriptionId,
            @RequestParam(required = false) String reason,
            Authentication auth) {
        log.info("[ADMIN] {} revoking subscription {}", auth.getName(), subscriptionId);
        subscriptionService.revokeSubscription(subscriptionId, auth.getName(), reason);
        return ResponseEntity.ok(Map.of(
                "message", Map.of("en", "Subscription revoked.", "hi", "सब्सक्रिप्शन रद्द कर दी गई।")));
    }

    /** List all subscriptions (paginated), optionally filtered by status. */
    @GetMapping
    public ResponseEntity<Page<UserSubscription>> list(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.adminListSubscriptions(status, pageable));
    }
}
