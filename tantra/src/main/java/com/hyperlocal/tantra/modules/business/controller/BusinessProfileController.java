package com.hyperlocal.tantra.modules.business.controller;

import com.hyperlocal.tantra.modules.business.dto.BusinessProfileRequest;
import com.hyperlocal.tantra.modules.business.dto.BusinessProfileResponse;
import com.hyperlocal.tantra.modules.business.entity.BusinessProfile;
import com.hyperlocal.tantra.modules.business.service.BusinessProfileService;
import com.hyperlocal.tantra.modules.forms.dto.FormRenderResponse;
import com.hyperlocal.tantra.modules.forms.service.FormRenderService;
import com.hyperlocal.tantra.modules.home.dto.HomeResponse;
import com.hyperlocal.tantra.modules.listing.dto.PageResponse;
import com.hyperlocal.tantra.modules.subscription.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Business profile — owner CRUD (Profile ▸ Business Profile) + public visible view.
 * Authenticated via /api/v1/business-profiles/** in SecurityConfig; owner from the JWT.
 */
@RestController
@RequestMapping("/api/v1/business-profiles")
public class BusinessProfileController {

    private static final Logger log = LoggerFactory.getLogger(BusinessProfileController.class);

    @Autowired private BusinessProfileService service;
    @Autowired private FormRenderService formRenderService;
    @Autowired private SubscriptionService subscriptionService;
    @Autowired private com.hyperlocal.tantra.modules.business.repository.BusinessProfileRepository businessProfileRepository;

    /**
     * Top sellers ranked by contact reveals — used for the seller stories strip on the home feed.
     * Public endpoint, no auth required.
     */
    @GetMapping("/top-sellers")
    public ResponseEntity<List<com.hyperlocal.tantra.modules.business.entity.BusinessProfile>> topSellers(
            @RequestParam(required = false) String district,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(businessProfileRepository.findTopSellers(district, Math.min(limit, 20)));
    }

    /** The metadata-driven form to render for creating/editing a business profile of this type. */
    @GetMapping("/form")
    public ResponseEntity<FormRenderResponse> form(@RequestParam(required = false) String profileType) {
        return ResponseEntity.ok(formRenderService.renderBusinessProfileForm(profileType));
    }

    @PostMapping
    public ResponseEntity<BusinessProfileResponse> create(@RequestBody BusinessProfileRequest request, Authentication auth) {
        return ResponseEntity.ok(service.create(request, auth.getName()));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BusinessProfile>> mine(Authentication auth) {
        return ResponseEntity.ok(service.getMine(auth.getName()));
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<BusinessProfile> get(@PathVariable String profileId, Authentication auth) {
        return ResponseEntity.ok(service.getVisible(profileId, auth.getName()));
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<BusinessProfileResponse> update(
            @PathVariable String profileId, @RequestBody BusinessProfileRequest request, Authentication auth) {
        return ResponseEntity.ok(service.update(profileId, request, auth.getName()));
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<BusinessProfileResponse> delete(@PathVariable String profileId, Authentication auth) {
        return ResponseEntity.ok(service.delete(profileId, auth.getName()));
    }

    // ─── Public Directory (OLX/Flipkart style) ───────────────────────────────

    /**
     * Browse approved business profiles with full filters: type, district, geo radius.
     * Premium seller profiles appear first (subscription sort weight).
     * Publicly accessible — no auth required.
     */
    @GetMapping("/directory")
    public ResponseEntity<PageResponse<HomeResponse.BusinessProfileCard>> directory(
            @RequestParam(required = false) String profileType,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("[DIRECTORY] profileType={} district={} lat={} lng={} radius={}", profileType, district, lat, lng, radius);
        Page<HomeResponse.BusinessProfileCard> result = service.getDirectory(profileType, district, lat, lng, radius, pageable);
        return ResponseEntity.ok(PageResponse.of(result));
    }
}
