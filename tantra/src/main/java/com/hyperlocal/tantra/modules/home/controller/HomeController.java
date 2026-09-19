package com.hyperlocal.tantra.modules.home.controller;

import com.hyperlocal.tantra.modules.home.dto.HomeResponse;
import com.hyperlocal.tantra.modules.home.dto.ModuleTabResponse;
import com.hyperlocal.tantra.modules.home.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Flipkart-style home feed. Publicly accessible — no auth required.
 * Cached per district+lang combination (TTL configured in CacheConfig).
 */
@RestController
@RequestMapping("/api/v1/home")
public class HomeController {

    @Autowired private HomeService homeService;

    /**
     * Returns the full home feed:
     * - All modules
     * - Featured premium listings (cross-module)
     * - Per-module sections with top categories + listings
     * - Featured business profiles
     * - Recent listings
     *
     * @param district  optional — filter by district for localized feed
     * @param lang      EN (default) / HI / …
     */
    @GetMapping
    public ResponseEntity<HomeResponse> home(
            @RequestParam(required = false) String district,
            @RequestParam(required = false, defaultValue = "EN") String lang) {
        return ResponseEntity.ok(homeService.buildHomeFeed(district, lang.toUpperCase()));
    }

    /**
     * Full module tab page — carousel + featured + per-category listing sections.
     *
     * GET /api/v1/home/tab/agriculture
     * GET /api/v1/home/tab/agriculture?district=Bhopal
     *
     * @param moduleKey  module slug, e.g. "agriculture", "animal_livestock"
     * @param district   optional — filter by user's district for localised feed
     */
    @GetMapping("/tab/{moduleKey}")
    public ResponseEntity<ModuleTabResponse> moduleTab(
            @PathVariable String moduleKey,
            @RequestParam(required = false) String district) {
        return ResponseEntity.ok(homeService.buildModuleTab(moduleKey, district));
    }
}
