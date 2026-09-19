package com.hyperlocal.tantra.modules.msp.controller;

import com.hyperlocal.tantra.modules.msp.entity.MspPrice;
import com.hyperlocal.tantra.modules.msp.repository.MspPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Public read-only endpoint for Government MSP (Minimum Support Price) data.
 * Admin creates/updates MSP entries via the admin API (secured separately).
 *
 * GET /api/v1/msp/current        — MSP for current year (all seasons)
 * GET /api/v1/msp?season=&year=  — filtered lookup
 */
@RestController
@RequestMapping("/api/v1/msp")
public class MspController {

    @Autowired
    private MspPriceRepository mspPriceRepository;

    /**
     * Current year's full MSP table — all active crops across all seasons.
     * Used by the "Govt MSP" banner and the MSP detail sheet on tap.
     */
    @GetMapping("/current")
    public ResponseEntity<List<MspPrice>> current() {
        int year = LocalDate.now().getYear();
        return ResponseEntity.ok(
                mspPriceRepository.findByYearAndIsActiveTrueOrderBySeasonAscCropKeyAsc(year));
    }

    /**
     * Admin: bulk-save MSP prices. Existing rows are upserted by JPA save.
     * POST /api/v1/msp/bulk  (secured via ROLE_ADMIN in SecurityConfig /api/v1/admin/**)
     */
    @Transactional
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkSave(@RequestBody List<MspPrice> prices) {
        if (prices.isEmpty()) return ResponseEntity.ok(Map.of("saved", 0));
        int year = prices.get(0).getYear();
        mspPriceRepository.deleteByYear(year);
        mspPriceRepository.flush();
        List<MspPrice> saved = mspPriceRepository.saveAll(prices);
        return ResponseEntity.ok(Map.of("saved", saved.size()));
    }

    /**
     * Filtered lookup by season and/or year.
     * e.g. GET /api/v1/msp?season=RABI&year=2026
     */
    @GetMapping
    public ResponseEntity<List<MspPrice>> filtered(
            @RequestParam(required = false) String season,
            @RequestParam(required = false) Integer year) {

        int targetYear = year != null ? year : LocalDate.now().getYear();

        if (season != null && !season.isBlank()) {
            return ResponseEntity.ok(
                    mspPriceRepository.findBySeasonAndYearAndIsActiveTrueOrderByCropKey(
                            season.toUpperCase(), targetYear));
        }
        return ResponseEntity.ok(
                mspPriceRepository.findByYearAndIsActiveTrueOrderBySeasonAscCropKeyAsc(targetYear));
    }
}
