package com.hyperlocal.tantra.modules.promo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class PromoCardRequest {

    /** Localized headline: {"en":"Summer Sale","hi":"गर्मी की सेल"} */
    private Map<String, String> title;

    /** Localized sub-heading (optional) */
    private Map<String, String> subtitle;

    private String imageUrl;

    /** Background hex color, e.g. "#FFF3E0" */
    private String bgColor;

    /** Text hex color */
    private String textColor;

    /** Localized CTA label: {"en":"Shop Now","hi":"अभी खरीदें"} */
    private Map<String, String> ctaLabel;

    /** Localized eyebrow label above the headline: {"en":"Rabi Season 2026","hi":"रबी सीज़न 2026"} */
    private Map<String, String> eyebrow;

    /** CTA pill button background hex, e.g. "#F0C040" */
    private String ctaBgColor;

    /** Frontend SVG illustration key: WHEAT / SEEDLING / TRACTOR (null = image-only) */
    private String illustrationKey;

    /** MODULE / CATEGORY / EXTERNAL / NONE */
    private String ctaType;

    /** Module ID, category ID string, or external URL */
    private String ctaValue;

    /** Lower = shown first */
    private Integer displayOrder;

    /** null = nationwide. Set to district name to target a specific district. */
    private String targetDistrict;

    private Boolean isActive;

    /** ISO datetime for card start. null = no gate. */
    private LocalDateTime validFrom;

    /** ISO datetime for card expiry. null = no expiry. */
    private LocalDateTime validTo;
}
