package com.hyperlocal.tantra.modules.carousel.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CarouselItemRequest {

    /** Localized button label: {"en":"Browse Crops","hi":"फसल देखें"} */
    private Map<String, String> label;

    private String iconUrl;

    /** BROWSE_CATEGORY / BROWSE_MODULE / EXTERNAL_URL / NONE */
    private String ctaType;

    /**
     * Category key (e.g. "crop"), module key, or URL depending on ctaType.
     */
    private String ctaValue;

    /**
     * Optional listing type filter for BROWSE_CATEGORY: SELL / RENT / BOTH.
     * Null = no filter.
     */
    private String listingType;

    private String bgColor;
    private String textColor;

    /** Lower = shown first. */
    private Integer displayOrder;

    private Boolean isActive;
}
