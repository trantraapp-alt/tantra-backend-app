package com.hyperlocal.tantra.modules.stats.controller;

import com.hyperlocal.tantra.modules.stats.dto.StatTileDTO;
import com.hyperlocal.tantra.modules.stats.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public stats endpoint — no auth required.
 * Returns DB-configured stat tiles with live computed values.
 * Admin controls which tiles show and their labels/icons via the stat_tiles table.
 */
@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    /** GET /api/v1/stats/public — returns active stat tiles for the home screen trust ribbon. */
    @GetMapping("/public")
    public ResponseEntity<List<StatTileDTO>> publicStats() {
        return ResponseEntity.ok(statsService.getStatTiles());
    }
}
