package com.hyperlocal.tantra.modules.promo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Admin-managed promotional/feature cards displayed on the home feed.
 * Cards can target a module, category, or an external URL.
 * Admin creates/edits via /api/v1/admin/promo-cards.
 * Frontend reads active cards via /api/v1/home or /api/v1/promo-cards.
 */
@Entity
@Table(name = "promo_cards", indexes = {
        @Index(name = "idx_promo_active_order", columnList = "is_active, display_order"),
        @Index(name = "idx_promo_district",     columnList = "target_district")
})
@Data
public class PromoCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public opaque id: PC + 8 chars */
    @Column(name = "card_id", unique = true, nullable = false, length = 20)
    private String cardId;

    /** Localized headline: {"en":"Summer Sale","hi":"गर्मी की सेल"} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "title", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> title;

    /** Localized sub-heading (optional) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subtitle", columnDefinition = "jsonb")
    private Map<String, String> subtitle;

    /** Full URL of banner/card image */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Card background hex color, e.g. "#FFF3E0". Shown when imageUrl is absent. */
    @Column(name = "bg_color", length = 10)
    private String bgColor = "#FFFFFF";

    /** Text hex color on the card */
    @Column(name = "text_color", length = 10)
    private String textColor = "#000000";

    /** Localized CTA button label: {"en":"Shop Now","hi":"अभी खरीदें"} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cta_label", columnDefinition = "jsonb")
    private Map<String, String> ctaLabel;

    /**
     * Visual size of the card on the home feed:
     *   CAROUSEL — full-width hero banner in the top carousel (default)
     *   MINI     — half-width mini banner (two rendered side by side)
     *   SCHEME   — government scheme card rendered in the scheme strip
     */
    @Column(name = "card_type", length = 20)
    private String cardType = "CAROUSEL";

    /**
     * How the CTA navigates:
     *   MODULE   — open module listing screen (ctaValue = moduleId)
     *   CATEGORY — open category listing screen (ctaValue = categoryId)
     *   EXTERNAL — open external URL (ctaValue = full URL)
     *   NONE     — card is display-only, no tap action
     */
    @Column(name = "cta_type", length = 20)
    private String ctaType = "NONE";

    /** Module ID, category ID, or full URL depending on ctaType. */
    @Column(name = "cta_value", length = 500)
    private String ctaValue;

    /** Lower number = shown first in the carousel. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /**
     * Optional district filter. null = show nationwide.
     * Set to a district name to show the card only in that district's feed.
     */
    @Column(name = "target_district", length = 100)
    private String targetDistrict;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Card is not shown before this date. null = no start gate. */
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    /** Card is not shown after this date. null = no expiry. */
    @Column(name = "valid_to")
    private LocalDateTime validTo;

    /**
     * Small eyebrow label rendered above the headline.
     * Localized: {"en":"Rabi Season 2026","hi":"रबी सीज़न 2026"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eyebrow", columnDefinition = "jsonb")
    private Map<String, String> eyebrow;

    /** Background hex for the CTA pill button, e.g. "#F0C040". */
    @Column(name = "cta_bg_color", length = 20)
    private String ctaBgColor;

    /**
     * Key for the frontend SVG illustration to render on this slide.
     * Known values: WHEAT, SEEDLING, TRACTOR.
     * null = no illustration (image-only card).
     */
    @Column(name = "illustration_key", length = 30)
    private String illustrationKey;

    /** Admin userId who created this card. */
    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
