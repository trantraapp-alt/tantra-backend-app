package com.hyperlocal.tantra.modules.subscription.controller;

import com.hyperlocal.tantra.modules.subscription.dto.PlanRequest;
import com.hyperlocal.tantra.modules.subscription.entity.SubscriptionPlan;
import com.hyperlocal.tantra.modules.subscription.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin CRUD for subscription plans.
 * All endpoints require ROLE_ADMIN (enforced by SecurityConfig /api/v1/admin/**).
 */
@RestController
@RequestMapping("/api/v1/admin/subscription-plans")
public class AdminPlanController {

    private static final Logger log = LoggerFactory.getLogger(AdminPlanController.class);

    @Autowired private SubscriptionService subscriptionService;

    /** All plans (active + inactive), sorted by sort_weight. */
    @GetMapping
    public ResponseEntity<List<SubscriptionPlan>> listAll() {
        return ResponseEntity.ok(subscriptionService.getAllPlans());
    }

    /** Get single plan by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlan> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(subscriptionService.getPlanById(id));
    }

    /**
     * Create a new plan tier.
     * Body example:
     * {
     *   "planKey": "GOLD",
     *   "name": {"en":"Gold Seller","hi":"गोल्ड विक्रेता"},
     *   "priceMonthly": 399,
     *   "priceYearly": 3999,
     *   "maxListings": 75,
     *   "homeFeedSlots": 3,
     *   "sortWeight": 30,
     *   "badgeLabel": {"en":"Gold Seller","hi":"गोल्ड विक्रेता"},
     *   "highlightColor": "#FFD700",
     *   "listingHighlight": true,
     *   "profileHighlight": true,
     *   "isActive": true
     * }
     */
    @PostMapping
    public ResponseEntity<SubscriptionPlan> create(
            @RequestBody PlanRequest request,
            Authentication auth) {
        log.info("[ADMIN] {} creating subscription plan {}", auth.getName(), request.getPlanKey());
        return ResponseEntity.ok(subscriptionService.createPlan(request, auth.getName()));
    }

    /** Update price, limits, badge, color, etc. planKey cannot be changed after creation. */
    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionPlan> update(
            @PathVariable Integer id,
            @RequestBody PlanRequest request,
            Authentication auth) {
        log.info("[ADMIN] {} updating subscription plan id={}", auth.getName(), id);
        return ResponseEntity.ok(subscriptionService.updatePlan(id, request, auth.getName()));
    }

    /** Toggle isActive on/off. Deactivated plans are hidden from the seller plan picker. */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<SubscriptionPlan> toggle(
            @PathVariable Integer id,
            Authentication auth) {
        log.info("[ADMIN] {} toggling plan id={}", auth.getName(), id);
        return ResponseEntity.ok(subscriptionService.togglePlan(id, auth.getName()));
    }

    /**
     * Soft-deletes (deactivates) a plan.
     * We never hard-delete to preserve history for existing subscriptions on that plan.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Integer id,
            Authentication auth) {
        log.info("[ADMIN] {} deleting plan id={}", auth.getName(), id);
        subscriptionService.deletePlan(id, auth.getName());
        return ResponseEntity.ok(Map.of(
                "message", Map.of("en", "Plan deactivated.", "hi", "प्लान निष्क्रिय कर दिया गया।")));
    }
}
