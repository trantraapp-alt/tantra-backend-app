package com.hyperlocal.tantra.modules.search.service;

import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.business.entity.BusinessProfile;
import com.hyperlocal.tantra.modules.business.repository.BusinessProfileRepository;
import com.hyperlocal.tantra.modules.home.dto.HomeResponse;
import com.hyperlocal.tantra.modules.home.service.HomeService;
import com.hyperlocal.tantra.modules.listing.dto.ListingCardDTO;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import com.hyperlocal.tantra.modules.listing.service.ListingService;
import com.hyperlocal.tantra.modules.search.dto.SearchResponse;
import com.hyperlocal.tantra.modules.subscription.service.SubscriptionService;
import com.hyperlocal.tantra.utils.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global bilingual search across listings (full-text tsvector) + business profiles.
 * Premium sellers appear before organic results within the same relevance tier.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private static final int PROFILE_SEARCH_LIMIT = 6;

    @Autowired private ListingRepository      listingRepository;
    @Autowired private BusinessProfileRepository profileRepository;
    @Autowired private ListingService         listingService;
    @Autowired private SubscriptionService    subscriptionService;
    @Autowired private UserRepository         userRepository;
    @Autowired private com.hyperlocal.tantra.modules.search.repository.SearchQueryRepository searchQueryRepository;

    /**
     * Global bilingual search across listings + business profiles.
     *
     * @param viewerMobile JWT mobile of the authenticated viewer, or null if unauthenticated.
     *                     When provided, the viewer's own listings are excluded (buyer perspective).
     * @param attrFilter   JSONB filter string built from attr_* request params, or null.
     * @param sortBy       "offeredPrice" | "createdAt" | null (default: relevance → subscription → newest).
     * @param sortDir      "asc" | "desc".
     */
    public SearchResponse search(
            String q,
            Integer moduleId,
            Integer categoryId,
            String  listingType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String  district,
            Double  lat,
            Double  lng,
            Integer radiusKm,
            String  sellerType,
            String  postedWithin,
            boolean withPhoto,
            boolean verifiedSeller,
            String  viewerMobile,
            String  attrFilter,
            String  sortBy,
            String  sortDir,
            Pageable pageable) {

        log.info("[SEARCH] q='{}' module={} category={} district={} lat={} lng={} radius={} sortBy={}",
                q, moduleId, categoryId, district, lat, lng, radiusKm, sortBy);

        // Resolve viewer's userId to exclude their own listings from search results
        String excludeUserId = resolveViewerUserId(viewerMobile);

        // Strip sort from Pageable — native query owns ORDER BY
        Pageable cleanPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize());

        // Compute bounding box for geo pre-filter
        Double latMin = null, latMax = null, lngMin = null, lngMax = null;
        if (lat != null && lng != null && radiusKm != null) {
            latMin = lat - GeoUtil.latOffset(radiusKm); latMax = lat + GeoUtil.latOffset(radiusKm);
            lngMin = lng - GeoUtil.lngOffset(radiusKm, lat); lngMax = lng + GeoUtil.lngOffset(radiusKm, lat);
        }

        LocalDateTime postedAfter = postedAfter(postedWithin);

        // Track the search query for trending (async, non-blocking, fire-and-forget)
        if (q != null && !q.isBlank()) {
            try {
                com.hyperlocal.tantra.modules.search.entity.SearchQuery sq =
                        new com.hyperlocal.tantra.modules.search.entity.SearchQuery();
                sq.setQueryTerm(q.toLowerCase().trim());
                searchQueryRepository.save(sq);
            } catch (Exception ex) {
                log.warn("[SEARCH] Failed to record search query '{}': {}", q, ex.getMessage());
            }
        }

        // Full-text listing search
        Page<Listing> rawPage = listingRepository.searchListings(
                q, categoryId, moduleId, listingType,
                minPrice, maxPrice, district, excludeUserId, attrFilter, sortBy, sortDir,
                postedAfter, latMin, latMax, lngMin, lngMax,
                sellerType, withPhoto, verifiedSeller, cleanPageable);

        List<ListingCardDTO> cards = rawPage.getContent().stream()
                .map(l -> listingService.toCard(l, lat, lng))
                .filter(c -> {
                    if (radiusKm != null && c.getDistanceKm() != null) return c.getDistanceKm() <= radiusKm;
                    return true;
                })
                .collect(Collectors.toList());

        // Business profile search (simple name/type match, premium-first, max 6)
        List<HomeResponse.BusinessProfileCard> profileCards = List.of();
        if (q != null && !q.isBlank() && pageable.getPageNumber() == 0) {
            profileCards = profileRepository
                    .findDirectory(null, district, latMin, latMax, lngMin, lngMax,
                            PageRequest.of(0, PROFILE_SEARCH_LIMIT))
                    .getContent().stream()
                    .filter(p -> matchesQuery(p, q))
                    .map(this::toProfileCard)
                    .collect(Collectors.toList());
        }

        SearchResponse response = new SearchResponse();
        response.setQuery(q);
        response.setTotalListings(rawPage.getTotalElements());
        response.setTotalPages(rawPage.getTotalPages());
        response.setCurrentPage(rawPage.getNumber());
        response.setPageSize(rawPage.getSize());
        response.setListings(cards);
        response.setBusinessProfiles(profileCards.isEmpty() ? null : profileCards);

        log.info("[SEARCH] Found {} listings, {} profiles for q='{}'",
                rawPage.getTotalElements(), profileCards.size(), q);
        return response;
    }

    /**
     * Returns top N trending search terms from the last 7 days.
     * Used to populate the "🔥 Trending Searches" chips on the home feed.
     */
    public List<String> getTrendingSearches(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return searchQueryRepository.findTrendingTerms(since, Math.min(limit, 20));
    }

    private boolean matchesQuery(BusinessProfile p, String q) {
        if (q == null || q.isBlank()) return true;
        String lower = q.toLowerCase();
        return (p.getBusinessName() != null && p.getBusinessName().toLowerCase().contains(lower))
            || (p.getProfileType() != null && p.getProfileType().toLowerCase().contains(lower));
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

    private LocalDateTime postedAfter(String postedWithin) {
        if (postedWithin == null) return null;
        switch (postedWithin.toUpperCase()) {
            case "TODAY": return LocalDateTime.now().minusDays(1);
            case "WEEK":  return LocalDateTime.now().minusWeeks(1);
            case "MONTH": return LocalDateTime.now().minusMonths(1);
            default:      return null;
        }
    }

    /**
     * Resolves a viewer's mobile number to their userId for exclusion filtering.
     * Returns null when mobileNumber is blank or not found — null = no exclusion applied.
     */
    private String resolveViewerUserId(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) return null;
        return userRepository.findByMobileNumber(mobileNumber)
                .map(User::getUserId).orElse(null);
    }
}
