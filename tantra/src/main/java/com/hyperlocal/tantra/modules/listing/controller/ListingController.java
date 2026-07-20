package com.hyperlocal.tantra.modules.listing.controller;

import com.hyperlocal.tantra.modules.listing.dto.ListingRequest;
import com.hyperlocal.tantra.modules.listing.dto.ListingResponse;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.service.ListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Listing submission and reads for authenticated users. Locked to authenticated via
 * /api/v1/listings/** in SecurityConfig; the user is resolved from the JWT, not the request body.
 */
@RestController
@RequestMapping("/api/v1/listings")
public class ListingController {

    @Autowired private ListingService service;

    @PostMapping
    public ResponseEntity<ListingResponse> create(@RequestBody ListingRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.createListing(request, authentication.getName()));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Listing>> myListings(Authentication authentication) {
        return ResponseEntity.ok(service.getMyListings(authentication.getName()));
    }

    @GetMapping("/{listingId}")
    public ResponseEntity<Listing> getByListingId(@PathVariable String listingId) {
        return ResponseEntity.ok(service.getByListingId(listingId));
    }
}
