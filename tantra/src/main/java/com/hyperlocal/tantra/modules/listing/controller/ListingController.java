package com.hyperlocal.tantra.modules.listing.controller;

import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.listing.dto.ListingPatchRequest;
import com.hyperlocal.tantra.modules.listing.dto.ListingRequest;
import com.hyperlocal.tantra.modules.listing.dto.ListingResponse;
import com.hyperlocal.tantra.modules.listing.dto.PageResponse;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import com.hyperlocal.tantra.modules.listing.service.ListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Listings API for authenticated users. Writes return a compact bilingual response
 * (success + listingId + userId + message{en,hi}); the owner is resolved from the JWT.
 * Locked to authenticated via /api/v1/listings/** in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/listings")
public class ListingController {

    @Autowired private ListingService service;

    // ----- create / update / delete (compact bilingual response) -----

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

    // ----- reads -----

    /** My-Listings page: caller's own listings (paginated), optional SELL/RENT + status filters. */
    @GetMapping("/mine")
    public ResponseEntity<PageResponse<Listing>> myListings(
            @RequestParam(required = false) ListingType listingType,
            @RequestParam(required = false) ListingStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {
        return ResponseEntity.ok(PageResponse.of(service.getMyListings(auth.getName(), listingType, status, pageable)));
    }

    /** Browse active listings in a category (paginated). */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<PageResponse<Listing>> byCategory(
            @PathVariable Integer categoryId,
            @RequestParam(required = false) ListingType listingType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(service.getByCategory(categoryId, listingType, pageable)));
    }

    @GetMapping("/{listingId}")
    public ResponseEntity<Listing> getByListingId(@PathVariable String listingId) {
        return ResponseEntity.ok(service.getByListingId(listingId));
    }
}
