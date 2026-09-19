package com.hyperlocal.tantra.modules.home.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyperlocal.tantra.modules.listing.dto.ListingCardDTO;
import com.hyperlocal.tantra.modules.master.entity.AppModule;
import com.hyperlocal.tantra.modules.master.entity.ModuleCategory;
import com.hyperlocal.tantra.modules.promo.entity.PromoCard;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Flipkart-style home page response assembled server-side in one call. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeResponse {

    private LocalDateTime generatedAt = LocalDateTime.now();

    /** All active modules (Crop, Seed, Animal…). */
    private List<AppModule> modules;

    /**
     * Admin-managed promotional/feature cards (banners, announcements, seasonal campaigns).
     * Shown at the top of the home feed — render as a horizontal carousel.
     */
    private List<PromoCard> promoCards;

    /** Cross-module premium seller listings (STANDARD+ subscription, home_feed_slots > 0). */
    private FeaturedSection featuredListings;

    /** One section per module — top categories + subscription-first listings. */
    private List<ModuleSection> moduleSections;

    /** Featured / premium business profiles (verified + PREMIUM+ subscription). */
    private List<BusinessProfileCard> featuredProfiles;

    /** Latest 8 listings regardless of subscription — shows freshness. */
    private List<ListingCardDTO> recentListings;

    // ─── Inner types ─────────────────────────────────────────────────────────

    @Data
    public static class FeaturedSection {
        private Map<String, String> title;
        private List<ListingCardDTO> listings;
    }

    @Data
    public static class ModuleSection {
        private AppModule module;
        private List<ModuleCategory> topCategories;
        private List<ListingCardDTO> listings;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BusinessProfileCard {
        private String profileId;
        private String userId;
        private String businessName;
        private String profileType;
        private String verificationStatus;
        private Object address;

        // Subscription badge
        private Boolean isHighlighted;
        private String highlightColor;
        private Map<String, String> badge;
        private String planKey;
    }
}
