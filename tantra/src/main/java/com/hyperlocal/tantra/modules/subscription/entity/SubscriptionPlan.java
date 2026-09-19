package com.hyperlocal.tantra.modules.subscription.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Defines available subscription plans (duration-based).
 * Plans apply to both sellers and buyers.
 * Admin manages plans via /api/v1/admin/subscription-plans.
 */
@Entity
@Table(name = "subscription_plans")
@Data
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Unique key used in code: FREE_TRIAL / QUARTERLY / HALF_YEARLY etc. */
    @Column(name = "plan_key", unique = true, nullable = false, length = 30)
    private String planKey;

    /** Localized display name: {"en":"Free Trial","hi":"निःशुल्क परीक्षण"} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "name", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> name;

    /** Plan price for the full duration. 0.00 = free. */
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    /** Duration in months: 1 = monthly, 3 = quarterly, 6 = half-yearly, 12 = yearly. */
    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths = 1;

    /** Maximum listings allowed under this plan. -1 = unlimited. */
    @Column(name = "max_listings", nullable = false)
    private Integer maxListings = 25;

    /** Max contact reveals ("Get Contact" taps) per billing period. -1 = unlimited. */
    @ColumnDefault("-1")
    @Column(name = "max_contact_views", nullable = false)
    private Integer maxContactViews = -1;

    /** Max times each listing may be edited under this plan. -1 = unlimited. */
    @ColumnDefault("-1")
    @Column(name = "max_modifications_per_listing", nullable = false)
    private Integer maxModificationsPerListing = -1;

    /** Days the seller's listings appear above FREE listings in search/feed. 0 = no boost. */
    @ColumnDefault("0")
    @Column(name = "priority_visibility_days", nullable = false)
    private Integer priorityVisibilityDays = 0;

    /**
     * Feature bullet points shown on the plan card. Multilingual.
     * {"en":["Pashushala Pro","Listing Cattle (25)","Ration Calculator Pro"],
     *  "hi":["पशुशाला प्रो","मवेशी लिस्टिंग (25)","राशन कैलकुलेटर प्रो"]}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    private Map<String, List<String>> features;

    /** Sort weight; lower = displayed first in plan list. */
    @Column(name = "sort_weight", nullable = false)
    private Integer sortWeight = 99;

    /** Whether listings from this plan get a highlight border/badge on cards. */
    @Column(name = "listing_highlight", nullable = false)
    private Boolean listingHighlight = false;

    /** Whether business profiles from this plan appear in featured/directory highlights. */
    @Column(name = "profile_highlight", nullable = false)
    private Boolean profileHighlight = false;

    /** How many listing slots appear in the home feed for this plan. 0 = none. */
    @Column(name = "home_feed_slots", nullable = false)
    private Integer homeFeedSlots = 0;

    /** Hex color for card highlight border, e.g. "#FFD700". Null = no highlight. */
    @Column(name = "highlight_color", length = 10)
    private String highlightColor;

    /** Localized badge label shown on highlighted cards: {"en":"Premium Seller","hi":"प्रीमियम विक्रेता"} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "badge_label", columnDefinition = "jsonb")
    private Map<String, String> badgeLabel;

    /**
     * Whether sellers on this plan can create Flash Deals with a countdown timer.
     * false on FREE_TRIAL / base plans; true on premium paid plans.
     * Admin sets this via PUT /api/v1/admin/subscription-plans/{id}.
     */
    @Column(name = "flash_deal_allowed", nullable = false)
    private Boolean flashDealAllowed = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
