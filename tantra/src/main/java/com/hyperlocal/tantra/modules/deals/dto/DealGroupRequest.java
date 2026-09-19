package com.hyperlocal.tantra.modules.deals.dto;

import lombok.Data;
import java.util.List;

@Data
public class DealGroupRequest {
    private String groupKey;
    private String icon;
    private String labelEn;
    private String labelHi;
    private String badgeEn;
    private String badgeHi;
    private String accentColor;
    private Boolean backendUi;
    private String backgroundImageUrl;
    private List<String> categoryKeys;
    private String listingType;
    private String unitEn;
    private String unitHi;
    private String ctaType;
    private String ctaValue;
    private Integer displayOrder;
    private Boolean isActive;
    /** How many days back to look for fresh listings. Default 15. */
    private Integer freshDealDays;
}
