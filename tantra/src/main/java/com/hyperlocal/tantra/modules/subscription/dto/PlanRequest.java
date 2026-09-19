package com.hyperlocal.tantra.modules.subscription.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class PlanRequest {

    /** Unique key e.g. FREE_TRIAL / QUARTERLY / HALF_YEARLY. Required on create; ignored on update. */
    private String planKey;

    /** Localized name: {"en":"Free Trial","hi":"निःशुल्क परीक्षण"} */
    private Map<String, String> name;

    /** Plan price for the full duration period. 0.00 = free. */
    private BigDecimal price;

    /** Duration in months: 1, 3, 6, 12. */
    private Integer durationMonths;

    /** Maximum active listings. -1 = unlimited. */
    private Integer maxListings;

    /** Max contact reveals per billing period. -1 = unlimited. */
    private Integer maxContactViews;

    /** Max edits per listing. -1 = unlimited. */
    private Integer maxModificationsPerListing;

    /** Days listings get boosted above FREE in search/feed. */
    private Integer priorityVisibilityDays;

    /**
     * Feature bullet points per language shown on the plan card.
     * {"en":["15 Listings","15 Contacts"],"hi":["15 लिस्टिंग","15 संपर्क"]}
     */
    private Map<String, List<String>> features;

    /** Sort weight; lower = displayed first. */
    private Integer sortWeight;

    private Boolean listingHighlight;
    private Boolean profileHighlight;
    private Integer homeFeedSlots;
    private String highlightColor;
    private Map<String, String> badgeLabel;
    private Boolean isActive;
}
