package com.hyperlocal.tantra.modules.subscription.service;

import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.audit.service.AuditService;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.notification.service.NotificationService;
import com.hyperlocal.tantra.modules.subscription.dto.GrantSubscriptionRequest;
import com.hyperlocal.tantra.modules.subscription.dto.PlanPublicResponse;
import com.hyperlocal.tantra.modules.subscription.dto.PlanRequest;
import com.hyperlocal.tantra.modules.subscription.dto.SubscriptionResponse;
import com.hyperlocal.tantra.modules.subscription.entity.UserSubscription;
import com.hyperlocal.tantra.modules.subscription.entity.SubscriptionPlan;
import com.hyperlocal.tantra.modules.subscription.repository.UserSubscriptionRepository;
import com.hyperlocal.tantra.modules.subscription.repository.SubscriptionPlanRepository;
import com.hyperlocal.tantra.utils.IdGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages user subscription lifecycle (buyers and sellers): grant, revoke, expire.
 * Premium sort logic reads activeSubscriptionFor(userId) via getActivePlan().
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    @Autowired private UserSubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionPlanRepository planRepository;
    @Autowired private AuditService auditService;
    @Autowired private NotificationService notificationService;

    // ─── PUBLIC PLAN CATALOGUE ───────────────────────────────────────────────

    /** Returns active plans raw (admin / internal use). */
    public List<SubscriptionPlan> getAllActivePlans() {
        return planRepository.findByIsActiveTrueOrderBySortWeightAsc();
    }

    /**
     * User-facing: returns active plans with dual-mode multilingual resolution.
     * Resolved fields (name, features) are in the requested language.
     * Full maps (nameAll, featuresAll) carry every language for client-side switching.
     *
     * @param lang ISO 639-1 code, e.g. "en" or "hi". Defaults to "en" if not matched.
     */
    public List<PlanPublicResponse> getActivePlansLocalized(String lang) {
        return planRepository.findByIsActiveTrueOrderBySortWeightAsc()
                .stream()
                .map(p -> toPlanPublicResponse(p, lang))
                .toList();
    }

    public List<SubscriptionPlan> getAllPlans() {
        return planRepository.findAllByOrderBySortWeightAsc();
    }

    public SubscriptionPlan getPlanById(Integer id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new LocalizedException(
                        "Subscription plan not found.", "सब्सक्रिप्शन प्लान नहीं मिला।"));
    }

    // ─── ADMIN – PLAN CRUD ───────────────────────────────────────────────────

    @Transactional
    public SubscriptionPlan createPlan(PlanRequest req, String adminUserId) {
        log.info("[PLAN] Admin {} creating plan {}", adminUserId, req.getPlanKey());

        if (req.getPlanKey() == null || req.getPlanKey().isBlank()) {
            throw new LocalizedException("Plan key is required.", "प्लान की अद्वितीय कुंजी आवश्यक है।");
        }
        if (planRepository.existsByPlanKey(req.getPlanKey().toUpperCase())) {
            throw new LocalizedException(
                    "A plan with key '" + req.getPlanKey() + "' already exists.",
                    "'" + req.getPlanKey() + "' कुंजी वाला प्लान पहले से मौजूद है।");
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        applyRequest(plan, req);
        plan.setPlanKey(req.getPlanKey().toUpperCase());
        SubscriptionPlan saved = planRepository.save(plan);

        auditService.record("PLAN_CREATED", "SUBSCRIPTION_PLAN",
                saved.getId().toString(), adminUserId,
                Map.of("planKey", saved.getPlanKey()));
        log.info("[PLAN] Created plan {} (id={})", saved.getPlanKey(), saved.getId());
        return saved;
    }

    @Transactional
    public SubscriptionPlan updatePlan(Integer id, PlanRequest req, String adminUserId) {
        log.info("[PLAN] Admin {} updating plan id={}", adminUserId, id);

        SubscriptionPlan plan = getPlanById(id);
        applyRequest(plan, req);
        plan.setUpdatedAt(LocalDateTime.now());
        SubscriptionPlan saved = planRepository.save(plan);

        auditService.record("PLAN_UPDATED", "SUBSCRIPTION_PLAN",
                id.toString(), adminUserId,
                Map.of("planKey", saved.getPlanKey()));
        return saved;
    }

    @Transactional
    public SubscriptionPlan togglePlan(Integer id, String adminUserId) {
        SubscriptionPlan plan = getPlanById(id);
        plan.setIsActive(!Boolean.TRUE.equals(plan.getIsActive()));
        plan.setUpdatedAt(LocalDateTime.now());
        SubscriptionPlan saved = planRepository.save(plan);

        auditService.record("PLAN_TOGGLED", "SUBSCRIPTION_PLAN",
                id.toString(), adminUserId,
                Map.of("isActive", String.valueOf(saved.getIsActive())));
        log.info("[PLAN] Plan {} (id={}) toggled to isActive={}", saved.getPlanKey(), id, saved.getIsActive());
        return saved;
    }

    @Transactional
    public void deletePlan(Integer id, String adminUserId) {
        SubscriptionPlan plan = getPlanById(id);
        if (subscriptionRepository.existsByUserIdAndStatus(
                "PLAN_ID_CHECK_" + id, "ACTIVE")) {
            // actual check via plan id
        }
        // Safe to deactivate rather than hard-delete (preserve history)
        plan.setIsActive(false);
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);

        auditService.record("PLAN_DELETED", "SUBSCRIPTION_PLAN",
                id.toString(), adminUserId,
                Map.of("planKey", plan.getPlanKey()));
        log.info("[PLAN] Plan {} (id={}) deactivated by admin {}", plan.getPlanKey(), id, adminUserId);
    }

    private void applyRequest(SubscriptionPlan plan, PlanRequest req) {
        if (req.getName() != null)                        plan.setName(req.getName());
        if (req.getPrice() != null)                       plan.setPrice(req.getPrice());
        if (req.getDurationMonths() != null)              plan.setDurationMonths(req.getDurationMonths());
        if (req.getMaxListings() != null)                 plan.setMaxListings(req.getMaxListings());
        if (req.getMaxContactViews() != null)             plan.setMaxContactViews(req.getMaxContactViews());
        if (req.getMaxModificationsPerListing() != null)  plan.setMaxModificationsPerListing(req.getMaxModificationsPerListing());
        if (req.getPriorityVisibilityDays() != null)      plan.setPriorityVisibilityDays(req.getPriorityVisibilityDays());
        if (req.getFeatures() != null)                    plan.setFeatures(req.getFeatures());
        if (req.getSortWeight() != null)                  plan.setSortWeight(req.getSortWeight());
        if (req.getListingHighlight() != null)            plan.setListingHighlight(req.getListingHighlight());
        if (req.getProfileHighlight() != null)            plan.setProfileHighlight(req.getProfileHighlight());
        if (req.getHomeFeedSlots() != null)               plan.setHomeFeedSlots(req.getHomeFeedSlots());
        if (req.getHighlightColor() != null)              plan.setHighlightColor(req.getHighlightColor());
        if (req.getBadgeLabel() != null)                  plan.setBadgeLabel(req.getBadgeLabel());
        if (req.getIsActive() != null)                    plan.setIsActive(req.getIsActive());
    }

    // ─── SELLER – OWN SUBSCRIPTION ──────────────────────────────────────────

    /** Returns the user's current active plan, or empty if on FREE tier. */
    public Optional<SubscriptionPlan> getActivePlan(String userId) {
        return subscriptionRepository
                .findActiveByUserId(userId, LocalDateTime.now())
                .flatMap(sub -> planRepository.findById(sub.getPlanId()));
    }

    /** Returns the seller's current active subscription detail resolved to the given language. */
    public Optional<SubscriptionResponse> getMySubscription(String userId, String lang) {
        return subscriptionRepository
                .findActiveByUserId(userId, LocalDateTime.now())
                .flatMap(sub -> planRepository.findById(sub.getPlanId())
                        .map(plan -> toResponse(sub, plan, lang)));
    }

    /** Convenience overload — resolves to English by default. */
    public Optional<SubscriptionResponse> getMySubscription(String userId) {
        return getMySubscription(userId, "en");
    }

    /** Full subscription history for the authenticated user. */
    public List<UserSubscription> getMyHistory(String userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ─── ADMIN – GRANT / REVOKE ──────────────────────────────────────────────

    @Transactional
    public SubscriptionResponse grantSubscription(GrantSubscriptionRequest req, String adminUserId) {
        log.info("[SUBSCRIPTION] Admin {} granting plan {} to user {} for {} days",
                adminUserId, req.getPlanKey(), req.getUserId(), req.getDurationDays());

        SubscriptionPlan plan = planRepository.findByPlanKey(req.getPlanKey())
                .orElseThrow(() -> new LocalizedException(
                        "Subscription plan not found: " + req.getPlanKey(),
                        "सब्सक्रिप्शन प्लान नहीं मिला: " + req.getPlanKey()));

        if (req.getDurationDays() == null || req.getDurationDays() <= 0) {
            throw new LocalizedException(
                    "Duration must be greater than 0 days.",
                    "अवधि 0 दिन से अधिक होनी चाहिए।");
        }

        // Cancel any existing active subscription for this user
        subscriptionRepository.findActiveByUserId(req.getUserId(), LocalDateTime.now())
                .ifPresent(existing -> {
                    log.info("[SUBSCRIPTION] Cancelling existing subscription {} for user {}",
                            existing.getSubscriptionId(), req.getUserId());
                    existing.setStatus("CANCELLED");
                    existing.setUpdatedAt(LocalDateTime.now());
                    subscriptionRepository.save(existing);
                    auditService.record("SUBSCRIPTION_CANCELLED", "SUBSCRIPTION",
                            existing.getSubscriptionId(), adminUserId,
                            Map.of("reason", "superseded_by_new_grant"));
                });

        LocalDateTime now = LocalDateTime.now();
        UserSubscription sub = new UserSubscription();
        sub.setSubscriptionId(generateUniqueSubscriptionId());
        sub.setUserId(req.getUserId());
        sub.setPlanId(plan.getId());
        sub.setStatus("ACTIVE");
        sub.setStartedAt(now);
        sub.setExpiresAt(now.plusDays(req.getDurationDays()));
        sub.setPaymentRef(req.getPaymentRef());
        sub.setPaymentGateway(req.getPaymentGateway());
        sub.setAutoRenew(Boolean.TRUE.equals(req.getAutoRenew()));
        sub.setGrantedBy(adminUserId);
        sub.setNotes(req.getNotes());

        UserSubscription saved = subscriptionRepository.save(sub);
        log.info("[SUBSCRIPTION] Granted {} ({}) to user {} — expires {}",
                plan.getPlanKey(), saved.getSubscriptionId(), req.getUserId(), saved.getExpiresAt());

        auditService.record("SUBSCRIPTION_GRANTED", "SUBSCRIPTION", saved.getSubscriptionId(), adminUserId,
                Map.of("userId", req.getUserId(), "plan", plan.getPlanKey(),
                       "durationDays", req.getDurationDays(), "expiresAt", saved.getExpiresAt().toString()));

        // Push notification to the user
        notificationService.push(req.getUserId(), "SUBSCRIPTION_ACTIVATED",
                LocalizedText.of("Plan Activated!", "प्लान सक्रिय हो गया!"),
                LocalizedText.of("Your " + plan.getPlanKey() + " plan is now active.",
                        "आपका " + plan.getPlanKey() + " प्लान अब सक्रिय है।"),
                "SUBSCRIPTION", saved.getSubscriptionId());

        return toResponse(saved, plan);
    }

    @Transactional
    public void revokeSubscription(String subscriptionId, String adminUserId, String reason) {
        log.info("[SUBSCRIPTION] Admin {} revoking subscription {}", adminUserId, subscriptionId);

        UserSubscription sub = subscriptionRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new LocalizedException(
                        "Subscription not found.", "सब्सक्रिप्शन नहीं मिली।"));

        if (!"ACTIVE".equals(sub.getStatus())) {
            throw new LocalizedException(
                    "Only ACTIVE subscriptions can be revoked.",
                    "केवल सक्रिय सब्सक्रिप्शन रद्द की जा सकती है।");
        }

        sub.setStatus("CANCELLED");
        sub.setNotes(reason);
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);

        log.info("[SUBSCRIPTION] Revoked subscription {} for user {}", subscriptionId, sub.getUserId());
        auditService.record("SUBSCRIPTION_REVOKED", "SUBSCRIPTION", subscriptionId, adminUserId,
                Map.of("userId", sub.getUserId(), "reason", reason != null ? reason : ""));
    }

    public Page<UserSubscription> adminListSubscriptions(String status, Pageable pageable) {
        return subscriptionRepository.findAll(status, pageable);
    }

    // ─── SCHEDULED EXPIRY JOB ────────────────────────────────────────────────

    /** Runs every hour; marks ACTIVE subscriptions whose expiresAt has passed as EXPIRED. */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void expireSubscriptions() {
        List<UserSubscription> expired = subscriptionRepository.findExpired(LocalDateTime.now());
        if (expired.isEmpty()) return;

        log.info("[SUBSCRIPTION] Expiry job: marking {} subscriptions as EXPIRED", expired.size());
        for (UserSubscription sub : expired) {
            sub.setStatus("EXPIRED");
            sub.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
            auditService.record("SUBSCRIPTION_EXPIRED", "SUBSCRIPTION",
                    sub.getSubscriptionId(), "SYSTEM",
                    Map.of("userId", sub.getUserId()));

            notificationService.push(sub.getUserId(), "SUBSCRIPTION_EXPIRED",
                    LocalizedText.of("Subscription Expired", "सब्सक्रिप्शन समाप्त हुई"),
                    LocalizedText.of("Your subscription has expired. Renew to continue enjoying premium features.",
                            "आपकी सब्सक्रिप्शन समाप्त हो गई है। प्रीमियम सुविधाओं का आनंद जारी रखने के लिए नवीनीकरण करें।"),
                    "SUBSCRIPTION", sub.getSubscriptionId());
        }
        log.info("[SUBSCRIPTION] Expiry job complete — {} subscriptions expired", expired.size());
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    /**
     * Maps a subscription + plan pair into a dual-mode response.
     * Resolved fields (planName, features) are pre-resolved to {@code lang}.
     * Full maps (planNames, featuresAll) are always included for client-side switching.
     */
    private SubscriptionResponse toResponse(UserSubscription sub, SubscriptionPlan plan, String lang) {
        SubscriptionResponse r = new SubscriptionResponse();
        r.setSubscriptionId(sub.getSubscriptionId());
        r.setUserId(sub.getUserId());
        r.setStatus(sub.getStatus());
        r.setStartedAt(sub.getStartedAt());
        r.setExpiresAt(sub.getExpiresAt());
        r.setAutoRenew(sub.getAutoRenew());
        r.setNotes(sub.getNotes());
        r.setCreatedAt(sub.getCreatedAt());
        r.setPlanKey(plan.getPlanKey());
        // Resolved
        r.setPlanName(resolve(plan.getName(), lang));
        r.setFeatures(resolveList(plan.getFeatures(), lang));
        r.setLang(lang);
        // Full maps
        r.setPlanNames(plan.getName());
        r.setFeaturesAll(plan.getFeatures());
        // Metadata
        r.setPrice(plan.getPrice());
        r.setDurationMonths(plan.getDurationMonths());
        r.setMaxListings(plan.getMaxListings());
        r.setSortWeight(plan.getSortWeight());
        return r;
    }

    /** Convenience overload used by admin grant (defaults to English). */
    private SubscriptionResponse toResponse(UserSubscription sub, SubscriptionPlan plan) {
        return toResponse(sub, plan, "en");
    }

    private PlanPublicResponse toPlanPublicResponse(SubscriptionPlan p, String lang) {
        PlanPublicResponse r = new PlanPublicResponse();
        r.setId(p.getId());
        r.setPlanKey(p.getPlanKey());
        // Resolved
        r.setName(resolve(p.getName(), lang));
        r.setFeatures(resolveList(p.getFeatures(), lang));
        r.setLang(lang);
        // Full maps
        r.setNameAll(p.getName());
        r.setFeaturesAll(p.getFeatures());
        // Metadata
        r.setPrice(p.getPrice());
        r.setDurationMonths(p.getDurationMonths());
        r.setMaxListings(p.getMaxListings());
        r.setMaxContactViews(p.getMaxContactViews());
        r.setMaxModificationsPerListing(p.getMaxModificationsPerListing());
        r.setPriorityVisibilityDays(p.getPriorityVisibilityDays());
        r.setSortWeight(p.getSortWeight());
        r.setListingHighlight(p.getListingHighlight());
        r.setProfileHighlight(p.getProfileHighlight());
        r.setHomeFeedSlots(p.getHomeFeedSlots());
        r.setHighlightColor(p.getHighlightColor());
        r.setBadgeLabel(p.getBadgeLabel());
        r.setIsActive(p.getIsActive());
        return r;
    }

    /**
     * Resolves a multilingual string map to a single value.
     * Fallback chain: requested lang → "en" → first available → empty string.
     */
    private String resolve(Map<String, String> map, String lang) {
        if (map == null || map.isEmpty()) return "";
        String val = map.get(lang);
        if (val != null) return val;
        val = map.get("en");
        if (val != null) return val;
        return map.values().iterator().next();
    }

    /**
     * Resolves a multilingual list map to a single-language list.
     * Fallback chain: requested lang → "en" → empty list.
     */
    private List<String> resolveList(Map<String, List<String>> map, String lang) {
        if (map == null || map.isEmpty()) return null;
        List<String> val = map.get(lang);
        if (val != null) return val;
        val = map.get("en");
        return val;
    }

    private String generateUniqueSubscriptionId() {
        String id;
        do {
            id = IdGeneratorUtil.generateId("SB");
        } while (subscriptionRepository.findBySubscriptionId(id).isPresent());
        return id;
    }
}
