package com.hyperlocal.tantra.modules.carousel.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Admin-managed carousel button/tile shown on the app home screen.
 * Each item maps to a browse action: a category page, a module page, or an external URL.
 * Ordered by displayOrder ascending; inactive items are hidden from the public endpoint.
 */
@Entity
@Table(name = "carousel_items", indexes = {
        @Index(name = "idx_carousel_active_order", columnList = "is_active, display_order")
})
@Data
public class CarouselItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Localized button label: {"en":"Browse Crops","hi":"फसल देखें"} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "label", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> label;

    /** Optional icon/image URL rendered on the carousel tile. */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /**
     * Navigation action on tap:
     *   BROWSE_CATEGORY — open /listings/browse/{ctaValue} (ctaValue = categoryKey e.g. "crop")
     *   BROWSE_MODULE   — open module listing screen   (ctaValue = moduleKey e.g. "agriculture")
     *   EXTERNAL_URL    — open full URL                (ctaValue = URL)
     *   NONE            — display-only, no tap action
     */
    @Column(name = "cta_type", length = 20, nullable = false)
    private String ctaType = "BROWSE_CATEGORY";

    /**
     * Depends on ctaType:
     *   BROWSE_CATEGORY → category key, e.g. "crop", "seed", "equipment"
     *   BROWSE_MODULE   → module key, e.g. "agriculture"
     *   EXTERNAL_URL    → full URL
     */
    @Column(name = "cta_value", length = 255)
    private String ctaValue;

    /**
     * Optional listing type filter applied when ctaType = BROWSE_CATEGORY.
     * Useful for splitting equipment into RENT vs SELL tabs.
     * Values: SELL, RENT, BOTH. Null = no filter (all types shown).
     */
    @Column(name = "listing_type", length = 10)
    private String listingType;

    /** Card background hex color, e.g. "#2E7D32". Shown behind icon/label. */
    @Column(name = "bg_color", length = 10)
    private String bgColor = "#FFFFFF";

    /** Text hex color for the label on the tile. */
    @Column(name = "text_color", length = 10)
    private String textColor = "#000000";

    /** Lower number = shown first in the carousel. */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Admin userId who last modified this item. */
    @Column(name = "updated_by", length = 20)
    private String updatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
