package com.hyperlocal.tantra.modules.home.service;

import com.hyperlocal.tantra.modules.business.entity.BusinessProfile;
import com.hyperlocal.tantra.modules.business.repository.BusinessProfileRepository;
import com.hyperlocal.tantra.modules.home.dto.HomeResponse;
import com.hyperlocal.tantra.modules.home.dto.ModuleTabResponse;
import com.hyperlocal.tantra.modules.listing.dto.ListingCardDTO;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import com.hyperlocal.tantra.modules.listing.service.ListingService;
import com.hyperlocal.tantra.modules.master.entity.AppModule;
import com.hyperlocal.tantra.modules.master.entity.ModuleCategory;
import com.hyperlocal.tantra.modules.master.repository.AppModuleRepository;
import com.hyperlocal.tantra.modules.master.repository.ModuleCategoryRepository;
import com.hyperlocal.tantra.modules.promo.entity.PromoCard;
import com.hyperlocal.tantra.modules.promo.service.PromoCardService;
import com.hyperlocal.tantra.modules.subscription.entity.SubscriptionPlan;
import com.hyperlocal.tantra.modules.subscription.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Assembles the Flipkart-style home feed in a single service call.
 * Response is cached (key = district:lang) and invalidated when listings/subscriptions change.
 */
@Service
public class HomeService {

    private static final Logger log = LoggerFactory.getLogger(HomeService.class);

    private static final int FEATURED_LIMIT      = 10;
    private static final int MODULE_LISTING_LIMIT = 6;
    private static final int TOP_CATEGORIES_LIMIT = 4;
    private static final int FEATURED_PROFILES    = 6;
    private static final int RECENT_LIMIT         = 8;

    @Autowired private AppModuleRepository       moduleRepository;
    @Autowired private ModuleCategoryRepository  categoryRepository;
    @Autowired private ListingRepository         listingRepository;
    @Autowired private BusinessProfileRepository profileRepository;
    @Autowired private ListingService            listingService;
    @Autowired private SubscriptionService       subscriptionService;
    @Autowired private PromoCardService          promoCardService;

    /**
     * Builds the full home page response.
     *
     * @param district  optional — filter to listings in a specific district
     * @param lang      language code (EN/HI/…) — used for badge label resolution
     */
    @Cacheable(value = "homeFeed", key = "#district + ':' + #lang")
    public HomeResponse buildHomeFeed(String district, String lang) {
        log.info("[HOME] Building home feed — district={} lang={}", district, lang);
        String districtParam = (district == null || district.isBlank()) ? null : district;

        HomeResponse response = new HomeResponse();

        // 1. All active modules
        response.setModules(moduleRepository.findByIsActiveTrue());

        // 2. Admin promo/feature cards (banners, campaigns, announcements)
        List<PromoCard> promoCards = promoCardService.getActiveCards(districtParam, null);
        if (!promoCards.isEmpty()) {
            response.setPromoCards(promoCards);
        }

        // 3. Featured listings (premium subscribers, cross-module)
        List<Listing> featuredRaw = listingRepository.findFeaturedForHomeFeed(districtParam, FEATURED_LIMIT);
        if (!featuredRaw.isEmpty()) {
            HomeResponse.FeaturedSection featured = new HomeResponse.FeaturedSection();
            featured.setTitle(Map.of("en", "Featured Listings", "hi", "विशेष लिस्टिंग"));
            featured.setListings(toCards(featuredRaw, null, null));
            response.setFeaturedListings(featured);
        }

        // 4. Module sections — each module gets top categories + its listings
        List<HomeResponse.ModuleSection> sections = moduleRepository.findByIsActiveTrue()
                .stream()
                .map(module -> buildModuleSection(module, districtParam))
                .filter(s -> !s.getListings().isEmpty())
                .collect(Collectors.toList());
        response.setModuleSections(sections);

        // 4. Featured business profiles
        List<BusinessProfile> profilesRaw = profileRepository.findFeaturedProfiles(districtParam, FEATURED_PROFILES);
        if (!profilesRaw.isEmpty()) {
            response.setFeaturedProfiles(profilesRaw.stream()
                    .map(p -> toProfileCard(p))
                    .collect(Collectors.toList()));
        }

        // 5. Recent listings (freshness section, no subscription filter)
        List<Listing> recent = listingRepository.findAll(
                PageRequest.of(0, RECENT_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .filter(l -> !Boolean.TRUE.equals(l.getIsDeleted()) && l.getStatus() == ListingStatus.ACTIVE)
                .collect(Collectors.toList());
        response.setRecentListings(toCards(recent, null, null));

        log.info("[HOME] Home feed assembled — modules={} sections={} featured={} profiles={}",
                response.getModules().size(), sections.size(),
                featuredRaw.size(), profilesRaw.size());
        return response;
    }

    private HomeResponse.ModuleSection buildModuleSection(AppModule module, String district) {
        HomeResponse.ModuleSection section = new HomeResponse.ModuleSection();
        section.setModule(module);

        // Top categories for this module (by display order)
        List<ModuleCategory> topCats = categoryRepository
                .findByModuleIdAndParentIdIsNullAndIsActiveTrueOrderByDisplayOrderAsc(module.getId())
                .stream()
                .limit(TOP_CATEGORIES_LIMIT)
                .collect(Collectors.toList());
        section.setTopCategories(topCats);

        // Listings for this module (premium-first)
        List<Listing> raw = listingRepository.findForModuleSection(module.getId(), district, MODULE_LISTING_LIMIT);
        section.setListings(toCards(raw, null, null));
        return section;
    }

    private List<ListingCardDTO> toCards(List<Listing> listings, Double lat, Double lng) {
        return listings.stream()
                .map(l -> listingService.toCard(l, lat, lng))
                .collect(Collectors.toList());
    }

    /**
     * Builds the full module tab page (e.g. tapping "Agriculture").
     * Returns: carousel + featured listings + per-category sections (each with 6 listings).
     *
     * @param moduleKey  e.g. "agriculture" — case-insensitive
     * @param district   optional district filter (derived from user's saved location)
     */
    public ModuleTabResponse buildModuleTab(String moduleKey, String district) {
        AppModule module = moduleRepository.findByModuleKeyIgnoreCase(moduleKey)
                .orElseThrow(() -> new IllegalArgumentException("Module not found: " + moduleKey));

        String districtParam = (district == null || district.isBlank()) ? null : district;

        ModuleTabResponse response = new ModuleTabResponse();
        response.setModule(module);

        // 1. Carousel — global promo cards (shown at top)
        List<PromoCard> promos = promoCardService.getActiveCards(districtParam, null);
        if (!promos.isEmpty()) response.setPromoCards(promos);

        // 2. Featured listings — only premium/subscribed sellers in this module
        List<Listing> featuredRaw = listingRepository.findFeaturedForModule(
                module.getId(), districtParam, FEATURED_LIMIT);
        if (!featuredRaw.isEmpty()) {
            ModuleTabResponse.FeaturedSection featured = new ModuleTabResponse.FeaturedSection();
            featured.setTitle(Map.of("en", "Featured " + module.getModuleNameEn(),
                                     "hi", "विशेष " + module.getModuleNameHi()));
            featured.setListings(toCards(featuredRaw, null, null));
            response.setFeaturedListings(featured);
        }

        // 3. Per-category sections — each top-level category gets 6 listings
        List<ModuleCategory> categories = categoryRepository
                .findByModuleIdAndParentIdIsNullAndIsActiveTrueOrderByDisplayOrderAsc(module.getId());

        List<ModuleTabResponse.CategorySection> sections = categories.stream()
                .map(cat -> {
                    List<Listing> raw = listingRepository.findForCategorySection(
                            cat.getId(), districtParam, MODULE_LISTING_LIMIT);
                    if (raw.isEmpty()) return null;
                    ModuleTabResponse.CategorySection sec = new ModuleTabResponse.CategorySection();
                    sec.setCategory(cat);
                    sec.setListings(toCards(raw, null, null));
                    return sec;
                })
                .filter(s -> s != null)
                .collect(Collectors.toList());

        response.setCategorySections(sections);
        return response;
    }

    private HomeResponse.BusinessProfileCard toProfileCard(BusinessProfile p) {
        HomeResponse.BusinessProfileCard card = new HomeResponse.BusinessProfileCard();
        card.setProfileId(p.getProfileId());
        card.setUserId(p.getUserId());
        card.setBusinessName(p.getBusinessName());
        card.setProfileType(p.getProfileType());
        card.setVerificationStatus(p.getVerificationStatus() != null ? p.getVerificationStatus().name() : null);
        card.setAddress(p.getAddress());

        subscriptionService.getActivePlan(p.getUserId()).ifPresentOrElse(plan -> {
            card.setIsHighlighted(plan.getProfileHighlight());
            card.setHighlightColor(plan.getHighlightColor());
            card.setBadge(plan.getBadgeLabel());
            card.setPlanKey(plan.getPlanKey());
        }, () -> card.setIsHighlighted(false));

        return card;
    }
}
