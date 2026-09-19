package com.hyperlocal.tantra.modules.listing.controller;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.contact.service.ContactRevealService;
import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.listing.dto.*;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import com.hyperlocal.tantra.modules.listing.service.ListingService;
import com.hyperlocal.tantra.modules.master.entity.ModuleCategory;
import com.hyperlocal.tantra.modules.master.repository.ModuleCategoryRepository;
import com.hyperlocal.tantra.utils.ListingQueryUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Listing API for authenticated users. Supports full browse, nearby, filtered search,
 * similar listings, OLX-style contact reveal, and public seller listings page.
 */
@RestController
@RequestMapping("/api/v1/listings")
public class ListingController {

    @Autowired private ListingService service;
    @Autowired private ContactRevealService contactRevealService;
    @Autowired private ModuleCategoryRepository categoryRepository;

    // ─── Writes ──────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ListingResponse> create(@RequestBody ListingRequest request, Authentication auth) {
        return ResponseEntity.ok(service.createListing(request, auth.getName()));
    }

    @PutMapping("/{listingId}")
    public ResponseEntity<ListingResponse> update(
            @PathVariable String listingId, @RequestBody ListingRequest request, Authentication auth) {
        return ResponseEntity.ok(service.updateListing(listingId, request, auth.getName()));
    }

    @PatchMapping("/{listingId}")
    public ResponseEntity<ListingResponse> patch(
            @PathVariable String listingId, @RequestBody ListingPatchRequest request, Authentication auth) {
        return ResponseEntity.ok(service.patchListing(listingId, request, auth.getName()));
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<ListingResponse> delete(@PathVariable String listingId, Authentication auth) {
        return ResponseEntity.ok(service.deleteListing(listingId, auth.getName()));
    }

    // ─── My listings ─────────────────────────────────────────────────────────

    @GetMapping("/mine")
    public ResponseEntity<PageResponse<Listing>> myListings(
            @RequestParam(required = false) ListingType listingType,
            @RequestParam(required = false) ListingStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {
        return ResponseEntity.ok(PageResponse.of(service.getMyListings(auth.getName(), listingType, status, pageable)));
    }

    // ─── Browse: category + full filters ─────────────────────────────────────

    /**
     * Browse active listings in a category. Supports all filters including geo radius and attributes.
     * Premium seller listings appear first (subscription-sort) unless an explicit sort is requested.
     * When authenticated, the viewer's own listings are excluded (buyer perspective).
     *
     * Attribute filtering: append attr_<attributeKey>=<value> for each attribute filter.
     *   e.g. attr_cropType=PULSE&attr_variety=BASMATI  →  AND logic (all must match).
     * Sorting: ?sortBy=offeredPrice&sortDir=asc  or  ?sort=offeredPrice,asc  (Spring-style).
     *   Supported sortBy values: offeredPrice, createdAt.
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<PageResponse<ListingCardDTO>> byCategory(
            @PathVariable Integer categoryId,
            @RequestParam(required = false) ListingType listingType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) String sellerType,
            @RequestParam(required = false) String postedWithin,
            @RequestParam(required = false, defaultValue = "false") boolean withPhoto,
            @RequestParam(required = false, defaultValue = "false") boolean verifiedSeller,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request,
            Authentication auth) {

        String viewerMobile = auth != null ? auth.getName() : null;
        String[] sort = ListingQueryUtil.resolveSort(sortBy, sortDir, pageable.getSort());
        ListingFilter filter = new ListingFilter();
        filter.setCategoryId(categoryId);
        filter.setListingType(listingType);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setDistrict(district);
        filter.setState(state);
        filter.setLatitude(lat);
        filter.setLongitude(lng);
        filter.setRadiusKm(radius);
        filter.setSellerType(sellerType);
        filter.setPostedWithin(postedWithin);
        filter.setWithPhoto(withPhoto);
        filter.setVerifiedSeller(verifiedSeller);
        filter.setAttrFilter(ListingQueryUtil.buildAttrFilter(request));
        filter.setSortBy(sort[0]);
        filter.setSortDir(sort[1]);
        return ResponseEntity.ok(PageResponse.of(service.browseListings(filter, pageable, viewerMobile)));
    }

    /**
     * Carousel browse endpoint — accepts the stable category key string (e.g. "crop", "seed",
     * "fertilizer", "pesticide", "equipment") so the frontend never needs to hard-code numeric IDs.
     * Supports optional listingType to split Equipment into RENT / SELL views.
     * When authenticated, the viewer's own listings are excluded (buyer perspective).
     * Attribute filtering and server-side sorting are supported (same contract as /category/{id}).
     */
    @GetMapping("/browse/{categoryKey}")
    public ResponseEntity<PageResponse<ListingCardDTO>> browseByKey(
            @PathVariable String categoryKey,
            @RequestParam(required = false) ListingType listingType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false) String postedWithin,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request,
            Authentication auth) {

        String viewerMobile = auth != null ? auth.getName() : null;
        String[] sort = ListingQueryUtil.resolveSort(sortBy, sortDir, pageable.getSort());
        ModuleCategory cat = categoryRepository.findByCategoryKey(categoryKey)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.CATEGORY_NOT_FOUND_EN, MessageConstants.CATEGORY_NOT_FOUND_HI));

        ListingFilter filter = new ListingFilter();
        filter.setCategoryId(cat.getId());
        filter.setListingType(listingType);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setDistrict(district);
        filter.setState(state);
        filter.setLatitude(lat);
        filter.setLongitude(lng);
        filter.setRadiusKm(radius);
        filter.setPostedWithin(postedWithin);
        filter.setAttrFilter(ListingQueryUtil.buildAttrFilter(request));
        filter.setSortBy(sort[0]);
        filter.setSortDir(sort[1]);
        return ResponseEntity.ok(PageResponse.of(service.browseListings(filter, pageable, viewerMobile)));
    }

    // ─── Nearby listings ─────────────────────────────────────────────────────

    /**
     * Nearby listings. Default sort: distance ASC (closest first), premium sellers first per tier.
     * Radius presets: 5 / 10 / 25 / 50 / 100 km. Omit radius for All India.
     * When authenticated, the viewer's own listings are excluded (buyer perspective).
     * Attribute filtering and server-side sorting are supported (same contract as /category/{id}).
     */
    @GetMapping("/nearby")
    public ResponseEntity<PageResponse<ListingCardDTO>> nearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false, defaultValue = "25") Integer radius,
            @RequestParam(required = false) Integer moduleId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) ListingType listingType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String sellerType,
            @RequestParam(required = false, defaultValue = "false") boolean withPhoto,
            @RequestParam(required = false, defaultValue = "false") boolean verifiedSeller,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request,
            Authentication auth) {

        String viewerMobile = auth != null ? auth.getName() : null;
        String[] sort = ListingQueryUtil.resolveSort(sortBy, sortDir, pageable.getSort());
        ListingFilter filter = new ListingFilter();
        filter.setLatitude(lat);
        filter.setLongitude(lng);
        filter.setRadiusKm(radius);
        filter.setModuleId(moduleId);
        filter.setCategoryId(categoryId);
        filter.setListingType(listingType);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setDistrict(district);
        filter.setSellerType(sellerType);
        filter.setWithPhoto(withPhoto);
        filter.setVerifiedSeller(verifiedSeller);
        filter.setAttrFilter(ListingQueryUtil.buildAttrFilter(request));
        filter.setSortBy(sort[0]);
        filter.setSortDir(sort[1]);
        return ResponseEntity.ok(PageResponse.of(service.browseListings(filter, pageable, viewerMobile)));
    }

    // ─── Public seller listings page ─────────────────────────────────────────

    /**
     * Public seller page — shows all active listings posted by a specific seller.
     * Own listings are intentionally NOT excluded here (the seller viewing their own public
     * profile should see all their listings). Buyer-feed exclusion does not apply.
     */
    @GetMapping("/by-seller/{userId}")
    public ResponseEntity<PageResponse<ListingCardDTO>> bySeller(
            @PathVariable String userId,
            @RequestParam(required = false) ListingType listingType,
            @PageableDefault(size = 20) Pageable pageable) {

        ListingFilter filter = new ListingFilter();
        filter.setUserId(userId);
        filter.setListingType(listingType);
        // Pass null viewerMobile — do not exclude any user on a seller's public page
        return ResponseEntity.ok(PageResponse.of(service.browseListings(filter, pageable, null)));
    }

    // ─── Flash Deals ─────────────────────────────────────────────────────────

    /**
     * Returns active flash deals for the home screen "⚡ Flash Deals" section.
     * Listings must have flash_deal=true, an active discount, and not be expired.
     * When authenticated, the viewer's own flash-deal listings are excluded.
     */
    @GetMapping("/flash-deals")
    public ResponseEntity<List<ListingCardDTO>> flashDeals(
            @RequestParam(required = false) String district,
            @RequestParam(required = false, defaultValue = "6") int limit,
            Authentication auth) {
        String viewerMobile = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(service.getFlashDeals(district, Math.min(limit, 12), viewerMobile));
    }

    // ─── Detail + similar ────────────────────────────────────────────────────

    /**
     * Listing detail page. View count is incremented unless the authenticated viewer is the owner
     * (prevents self-views from inflating analytics).
     */
    @GetMapping("/{listingId}")
    public ResponseEntity<ListingCardDTO> getByListingId(
            @PathVariable String listingId,
            Authentication auth) {
        String viewerMobile = auth != null ? auth.getName() : null;
        Listing l = service.getByListingId(listingId, viewerMobile);
        return ResponseEntity.ok(service.toCard(l, null, null));
    }

    /**
     * Similar listings in the same category. When authenticated, excludes the viewer's own listings.
     */
    @GetMapping("/{listingId}/similar")
    public ResponseEntity<List<ListingCardDTO>> similar(
            @PathVariable String listingId,
            @RequestParam(required = false, defaultValue = "6") Integer limit,
            Authentication auth) {
        String viewerMobile = auth != null ? auth.getName() : null;
        Listing l = service.getByListingId(listingId, viewerMobile);
        return ResponseEntity.ok(service.getSimilarListings(listingId, l.getCategoryId(), limit, viewerMobile));
    }

    // Contact reveal moved to ContactRevealController → POST /api/v1/contacts/reveal/{listingId}
}
