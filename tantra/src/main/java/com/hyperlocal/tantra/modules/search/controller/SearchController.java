package com.hyperlocal.tantra.modules.search.controller;

import com.hyperlocal.tantra.modules.search.dto.SearchResponse;
import com.hyperlocal.tantra.modules.search.service.SearchService;
import com.hyperlocal.tantra.utils.ListingQueryUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Global bilingual search endpoint. Publicly accessible.
 * Returns matching listings (full-text tsvector) + matching business profiles.
 * Premium seller results appear first within each relevance tier.
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    @Autowired private SearchService searchService;

    /**
     * GET /api/v1/search/trending — top N trending search terms from the last 7 days.
     * Populated by recording every search query. Public, no auth required.
     */
    @GetMapping("/trending")
    public ResponseEntity<List<String>> trending(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchService.getTrendingSearches(limit));
    }

    /**
     * Search listings + business profiles.
     *
     * Query params:
     *   q            — bilingual text query (Hindi or English)
     *   moduleId     — filter by module
     *   categoryId   — filter by category
     *   listingType  — SELL / RENT
     *   minPrice     — price range lower bound
     *   maxPrice     — price range upper bound
     *   district     — text match on address.district
     *   lat, lng     — user GPS for distance calculation
     *   radius       — km radius (5/10/25/50/100; omit = All India)
     *   sellerType   — ALL (default) / SUBSCRIBED
     *   postedWithin — TODAY / WEEK / MONTH / ALL
     *   lang         — EN (default) / HI / …
     *   page, size   — pagination
     */
    /**
     * Global bilingual search. When authenticated, viewer's own listings are excluded.
     * Attribute filtering: attr_<key>=<value> (same contract as browse endpoints).
     * Sorting: ?sortBy=offeredPrice&sortDir=asc  or  ?sort=offeredPrice,asc.
     */
    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer moduleId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String listingType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) String sellerType,
            @RequestParam(required = false) String postedWithin,
            @RequestParam(required = false, defaultValue = "false") boolean withPhoto,
            @RequestParam(required = false, defaultValue = "false") boolean verifiedSeller,
            @RequestParam(required = false, defaultValue = "EN") String lang,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request,
            Authentication auth) {

        String viewerMobile = auth != null ? auth.getName() : null;
        String[] sort = ListingQueryUtil.resolveSort(sortBy, sortDir, pageable.getSort());
        String attrFilter = ListingQueryUtil.buildAttrFilter(request);
        return ResponseEntity.ok(searchService.search(
                q, moduleId, categoryId, listingType,
                minPrice, maxPrice, district,
                lat, lng, radius,
                sellerType, postedWithin, withPhoto, verifiedSeller,
                viewerMobile, attrFilter, sort[0], sort[1], pageable));
    }
}
