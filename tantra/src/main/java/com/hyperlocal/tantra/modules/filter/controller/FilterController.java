package com.hyperlocal.tantra.modules.filter.controller;

import com.hyperlocal.tantra.modules.filter.dto.FilterItemDTO;
import com.hyperlocal.tantra.modules.filter.service.FilterConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter configuration for the listing browse / category screen.
 *
 * <pre>
 * ─── Single category ──────────────────────────────────────────────────────
 * GET /api/v1/filters/config
 *                                → global defaults (no category)
 * GET /api/v1/filters/config?categoryId=5
 *                                → price range for crop (₹0 – ₹2L)
 *
 * ─── Multiple categories ──────────────────────────────────────────────────
 * GET /api/v1/filters/config?categoryIds=5,9
 *                                → crop (₹2L) + equipment (₹50L)
 *                                  merged → ₹0 – ₹50L, step 500 (finest)
 * GET /api/v1/filters/config?categoryIds=5,6,7
 *                                → crop + seed + pesticide merged
 * </pre>
 *
 * When both {@code categoryId} and {@code categoryIds} are supplied,
 * {@code categoryIds} takes precedence.
 *
 * Response is wrapped in ApiResponse by ResponseWrapperAdvice:
 * { "success": true, "data": [ ...FilterItemDTO... ] }
 */
@RestController
@RequestMapping("/api/v1/filters")
@RequiredArgsConstructor
public class FilterController {

    private final FilterConfigService filterConfigService;

    @GetMapping("/config")
    public List<FilterItemDTO> getFilterConfig(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String categoryIds) {

        // Multiple categories: ?categoryIds=5,9  or  ?categoryIds=5&categoryIds=9
        if (categoryIds != null && !categoryIds.isBlank()) {
            List<Integer> ids = Arrays.stream(categoryIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            return filterConfigService.getFiltersForMultiple(ids);
        }

        // Single category or no category
        return filterConfigService.getFilters(categoryId);
    }
}
