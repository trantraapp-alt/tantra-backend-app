package com.hyperlocal.tantra.modules.subscription.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * User-facing plan response with dual-mode multilingual support.
 * <p>
 * Dual mode means the response carries both:
 *   - Resolved fields (name, features) — already in the requested language, ready to display.
 *   - Full maps (nameAll, featuresAll) — every language keyed by locale code ("en", "hi", …),
 *     so the client can switch language client-side without a second request.
 * <p>
 * Request language is controlled via ?lang=en|hi (default: en).
 * Fallback chain: requested lang → "en" → first available.
 */
@Data
public class PlanPublicResponse {

    private Integer id;
    private String planKey;

    // ── Resolved (ready to display) ────────────────────────────────────────
    /** Plan display name in the requested language. */
    private String name;
    /** Feature bullet points in the requested language. Null if admin hasn't set features yet. */
    private List<String> features;
    /** Which language was resolved (echoed back for the client). */
    private String lang;

    // ── Full multilingual maps (dual mode — client-side language switching) ─
    /** All language variants of the plan name: {"en":"Quarterly","hi":"त्रैमासिक"} */
    private Map<String, String> nameAll;
    /** All language variants of features: {"en":[...],"hi":[...]} */
    private Map<String, List<String>> featuresAll;

    // ── Plan metadata ───────────────────────────────────────────────────────
    private BigDecimal price;
    private Integer durationMonths;
    private Integer maxListings;
    private Integer maxContactViews;
    private Integer maxModificationsPerListing;
    private Integer priorityVisibilityDays;
    private Integer sortWeight;
    private Boolean listingHighlight;
    private Boolean profileHighlight;
    private Integer homeFeedSlots;
    private String highlightColor;
    private Map<String, String> badgeLabel;
    private Boolean isActive;
}
