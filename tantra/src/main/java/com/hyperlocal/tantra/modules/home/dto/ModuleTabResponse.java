package com.hyperlocal.tantra.modules.home.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyperlocal.tantra.modules.listing.dto.ListingCardDTO;
import com.hyperlocal.tantra.modules.master.entity.AppModule;
import com.hyperlocal.tantra.modules.master.entity.ModuleCategory;
import com.hyperlocal.tantra.modules.promo.entity.PromoCard;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** Full module tab response — carousel + featured + per-category listing sections. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModuleTabResponse {

    private AppModule module;

    /** Banner carousel — shown at top of the module tab. */
    private List<PromoCard> promoCards;

    /** Featured (premium/subscribed) sellers within this module. */
    private FeaturedSection featuredListings;

    /**
     * One section per top-level category in this module.
     * Each section shows up to 6 listings + a "See All" categoryKey.
     */
    private List<CategorySection> categorySections;

    @Data
    public static class FeaturedSection {
        private Map<String, String> title;
        private List<ListingCardDTO> listings;
    }

    @Data
    public static class CategorySection {
        private ModuleCategory category;
        private List<ListingCardDTO> listings;
    }
}
