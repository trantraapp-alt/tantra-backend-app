package com.hyperlocal.tantra.modules.deals.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Admin-managed deal group card shown on the home screen "Today's Deals" strip.
 * A group maps to one or more category keys (e.g. POULTRY_FISHERY = ["poultry","fishery"]).
 * Live stats (listingCount, minPrice) are computed at request time.
 */
@Entity
@Table(name = "deal_groups", indexes = {
        @Index(name = "idx_deal_active_order", columnList = "is_active, display_order")
})
@Data
public class DealGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "group_key", unique = true, nullable = false, length = 50)
    private String groupKey;

    @Column(name = "icon", length = 10)
    private String icon;

    @Column(name = "label_en", nullable = false, length = 100)
    private String labelEn;

    @Column(name = "label_hi", nullable = false, length = 100)
    private String labelHi;

    @Column(name = "badge_en", length = 100)
    private String badgeEn;

    @Column(name = "badge_hi", length = 100)
    private String badgeHi;

    /** Hex accent: left border + badge text + price text. e.g. "#22C55E" */
    @Column(name = "accent_color", length = 10, nullable = false)
    private String accentColor = "#22C55E";

    /**
     * Category keys for this group. Stored as comma-separated text: "crop" or "poultry,fishery".
     * Resolved to numeric IDs at query time via getCategoryKeysList().
     */
    @Column(name = "category_keys", nullable = false, length = 200)
    private String categoryKeys;

    /** Parse the comma-separated categoryKeys into a List. */
    public List<String> getCategoryKeysList() {
        if (categoryKeys == null || categoryKeys.isBlank()) return List.of();
        return Arrays.asList(categoryKeys.trim().split("\\s*,\\s*"));
    }

    /** RENT / SELL / BOTH. Null = all types. */
    @Column(name = "listing_type", length = 10)
    private String listingType;

    /** Price unit label. e.g. "/quintal", "/day". Null = no unit shown. */
    @Column(name = "unit_en", length = 30)
    private String unitEn;

    @Column(name = "unit_hi", length = 30)
    private String unitHi;

    /**
     * DEAL_GROUP -> open /deals/{ctaValue}/listings
     * CATEGORY   -> open /listings/browse/{ctaValue}
     * URL        -> open in browser
     */
    @Column(name = "cta_type", length = 20, nullable = false)
    private String ctaType = "DEAL_GROUP";

    @Column(name = "cta_value", length = 255)
    private String ctaValue;

    /**
     * If true → frontend must use backend-provided UI (accentColor + backgroundImageUrl).
     * If false → frontend renders its own card design (backend fields ignored for styling).
     */
    @Column(name = "backend_ui", nullable = false)
    private Boolean backendUi = false;

    /** Optional background image URL/path for this deal card. Shown only when backendUi=true. */
    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl;

    /**
     * How many days back to look for "fresh" listings in this deal group.
     * Default 15. Admin can change per group via Admin Deal API.
     * Replaces the hardcoded INTERVAL '15 days' in repository queries.
     */
    @ColumnDefault("15")
    @Column(name = "fresh_deal_days", nullable = false)
    private Integer freshDealDays = 15;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "updated_by", length = 20)
    private String updatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
