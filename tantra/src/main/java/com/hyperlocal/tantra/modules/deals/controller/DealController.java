package com.hyperlocal.tantra.modules.deals.controller;

import com.hyperlocal.tantra.modules.deals.dto.DealCardDTO;
import com.hyperlocal.tantra.modules.deals.service.DealService;
import com.hyperlocal.tantra.modules.listing.dto.ListingCardDTO;
import com.hyperlocal.tantra.modules.listing.dto.PageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public deal cards + listings endpoints.
 * GET /api/v1/deals                              - home screen strip
 * GET /api/v1/deals/{groupKey}/listings          - listings screen (price ASC)
 */
@RestController
@RequestMapping("/api/v1/deals")
public class DealController {

    @Autowired private DealService dealService;

    @GetMapping
    public ResponseEntity<List<DealCardDTO>> getDeals() {
        return ResponseEntity.ok(dealService.getDealCards());
    }

    @GetMapping("/{groupKey}/listings")
    public ResponseEntity<PageResponse<ListingCardDTO>> getListings(
            @PathVariable String groupKey,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(PageResponse.of(
                dealService.getDealListings(groupKey, lat, lng, radius,
                        PageRequest.of(page, size))));
    }
}
