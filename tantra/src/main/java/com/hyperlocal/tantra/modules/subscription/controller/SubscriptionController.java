package com.hyperlocal.tantra.modules.subscription.controller;

import com.hyperlocal.tantra.modules.subscription.dto.PlanPublicResponse;
import com.hyperlocal.tantra.modules.subscription.entity.UserSubscription;
import com.hyperlocal.tantra.modules.subscription.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Seller-facing subscription endpoints (authenticated).
 * All localizable fields are returned in dual mode:
 *   - Resolved (name, features) — pre-resolved to the requested ?lang, display directly.
 *   - Full maps (nameAll, featuresAll / planNames, featuresAll) — every language for client-side switching.
 *
 * Query param: ?lang=en|hi  (default: en)
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    @Autowired private SubscriptionService subscriptionService;

    /**
     * Returns all active plans ordered by sort weight.
     * Use ?lang=hi to receive Hindi names and feature bullets pre-resolved.
     * The response also includes nameAll and featuresAll for language switching.
     */
    @GetMapping("/plans")
    public ResponseEntity<List<PlanPublicResponse>> getPlans(
            @RequestParam(defaultValue = "en") String lang) {
        return ResponseEntity.ok(subscriptionService.getActivePlansLocalized(lang));
    }

    /**
     * Returns the authenticated seller's current active subscription + plan details.
     * Use ?lang=hi for Hindi. planName and features are pre-resolved; planNames and featuresAll
     * carry the full multilingual maps.
     */
    @GetMapping("/mine")
    public ResponseEntity<?> mySubscription(
            Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return subscriptionService.getMySubscription(auth.getName(), lang)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(null));
    }

    /** Full subscription history for the authenticated seller (all statuses). */
    @GetMapping("/mine/history")
    public ResponseEntity<List<UserSubscription>> myHistory(Authentication auth) {
        return ResponseEntity.ok(subscriptionService.getMyHistory(auth.getName()));
    }
}
