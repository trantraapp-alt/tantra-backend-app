package com.hyperlocal.tantra.modules.listing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyperlocal.tantra.modules.listing.model.Address;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Listing card used in home feed, category browse, nearby, and search results.
 * Includes subscription badge info so the frontend can render highlighted cards.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListingCardDTO {

    private String listingId;
    private String userId;
    private Integer moduleId;
    private Integer categoryId;
    private String listingTitle;
    private String listingType;
    private BigDecimal actualPrice;
    private BigDecimal offeredPrice;
    private BigDecimal discountPct;
    private BigDecimal quantity;
    private String unit;
    private Boolean isNegotiable;
    private Boolean showContact;
    /**
     * true = seller offers delivery. Present only on SELL listings.
     * Show as "🚚 Delivery Available" badge on the listing card / detail screen.
     */
    private Boolean deliveryAvailable;
    private List<String> images;
    private Address address;
    private Map<String, Object> attributes;
    private String status;
    private Long viewCount;
    private Long contactRevealCount;
    private LocalDateTime createdAt;

    /** Distance from the user's location in km — present only when lat/lng filter was applied. */
    private Double distanceKm;

    // ─── Subscription / Premium badge fields ─────────────────────────────────

    /** true if the seller has an active paid subscription. */
    private Boolean isHighlighted;

    /** Card border highlight color (e.g. "#FFD700") — present only when isHighlighted = true. */
    private String highlightColor;

    /** Localized badge: {"en":"Premium Seller","hi":"प्रीमियम विक्रेता"} */
    private Map<String, String> sellerBadge;

    /** Plan key of the seller's active subscription: BASIC/STANDARD/PREMIUM/ENTERPRISE */
    private String sellerPlanKey;

    // ─── Flash Deal fields ────────────────────────────────────────────────────

    /** true if this listing is currently in the Flash Deals section. */
    private Boolean flashDeal;

    /** ISO timestamp when the discounted price expires. Null = no expiry. */
    private LocalDateTime discountExpiresAt;

    // ─── Computed fields (server-side, not stored) ────────────────────────────

    /** true when the listing was posted within the last 24 hours. */
    private Boolean isNew;

    /**
     * true when the listing's seller has a BusinessProfile with verificationStatus = APPROVED.
     * Renders as the blue verified tick on the card.
     */
    private Boolean sellerVerified;

    // ─── Category Quality Description ─────────────────────────────────────────

    /**
     * Admin-set quality assurance description for this listing's category — English.
     * e.g. "All crop listings are verified for freshness and grade accuracy."
     * Show on listing detail page only, not on browse cards.
     */
    private String qualityDescEn;

    /**
     * Admin-set quality assurance description for this listing's category — Hindi.
     * e.g. "सभी फसल लिस्टिंग ताजगी और ग्रेड के लिए सत्यापित हैं।"
     * Show on listing detail page only, not on browse cards.
     */
    private String qualityDescHi;
}
