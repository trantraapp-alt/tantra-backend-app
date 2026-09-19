package com.hyperlocal.tantra.modules.subscription.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Flat response combining UserSubscription + SubscriptionPlan fields.
 * <p>
 * Dual-mode multilingual: every localizable field is returned twice —
 *   - Resolved (planName, features): pre-resolved to the requested language, display directly.
 *   - Full map (planNames, featuresAll): all language variants, for client-side language switching.
 * <p>
 * Language is controlled by ?lang=en|hi on the calling endpoint (default: en).
 */
@Data
public class SubscriptionResponse {

    // ── Subscription fields ─────────────────────────────────────────────────
    private String subscriptionId;
    private String userId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private Boolean autoRenew;
    private String notes;
    private LocalDateTime createdAt;

    // ── Plan details — resolved (ready to display) ──────────────────────────
    private String planKey;
    /** Plan display name in the requested language. */
    private String planName;
    /** Feature bullet points in the requested language. Null if admin hasn't configured yet. */
    private List<String> features;
    /** Which language was resolved. */
    private String lang;

    // ── Plan details — full multilingual maps (dual mode) ───────────────────
    /** All language variants: {"en":"Quarterly","hi":"त्रैमासिक"} */
    private Map<String, String> planNames;
    /** All language variants: {"en":[...],"hi":[...]} */
    private Map<String, List<String>> featuresAll;

    // ── Plan metadata ───────────────────────────────────────────────────────
    private BigDecimal price;
    private Integer durationMonths;
    private Integer maxListings;
    private Integer sortWeight;
}
