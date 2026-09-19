package com.hyperlocal.tantra.modules.listing.controller;

import com.hyperlocal.tantra.modules.listing.dto.FilterFormResponse;
import com.hyperlocal.tantra.modules.listing.service.FilterFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns the filter form definition for any listing browse screen.
 *
 * GET /api/v1/filter-form              — global (home / all-categories browse)
 * GET /api/v1/filter-form?categoryId=4 — category-specific (adds variety/breed/brand groups)
 */
@RestController
@RequestMapping("/api/v1/filter-form")
public class FilterFormController {

    @Autowired private FilterFormService filterFormService;

    @GetMapping
    public ResponseEntity<FilterFormResponse> getFilterForm(
            @RequestParam(required = false) Integer categoryId) {
        return ResponseEntity.ok(filterFormService.buildForCategory(categoryId));
    }
}
