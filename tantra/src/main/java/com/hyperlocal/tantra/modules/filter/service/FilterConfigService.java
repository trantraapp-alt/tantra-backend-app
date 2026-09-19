package com.hyperlocal.tantra.modules.filter.service;

import com.hyperlocal.tantra.modules.filter.dto.FilterItemDTO;
import com.hyperlocal.tantra.modules.filter.entity.FilterConfig;
import com.hyperlocal.tantra.modules.filter.repository.FilterConfigRepository;
import com.hyperlocal.tantra.modules.master.entity.ModuleCategory;
import com.hyperlocal.tantra.modules.master.repository.ModuleCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilterConfigService {

    private final FilterConfigRepository filterConfigRepository;
    private final ModuleCategoryRepository moduleCategoryRepository;

    // ─── Single category (or no category → global defaults) ──────────────────

    /**
     * Filter config for a single category screen.
     *
     * <p>Merge order: GLOBAL → CATEGORY override (higher scope wins).
     *
     * @param categoryId null → returns global defaults only
     */
    public List<FilterItemDTO> getFilters(Integer categoryId) {
        Map<String, FilterItemDTO> base = globalBase();

        if (categoryId != null) {
            moduleCategoryRepository.findById(categoryId).ifPresent(cat -> {
                List<FilterConfig> overrides = filterConfigRepository
                        .findByScopeAndScopeKeyAndIsActiveTrueOrderByDisplayOrderAsc(
                                "CATEGORY", cat.getCategoryKey());
                overrides.forEach(f -> base.put(f.getFilterKey(), toDTO(f)));
            });
        }

        return sorted(base);
    }

    // ─── Multiple categories — merge ranges ───────────────────────────────────

    /**
     * Filter config when user has selected multiple categories simultaneously
     * (e.g. browsing at parent/module level, or multi-select on category screen).
     *
     * <p>Merge rules:
     * <ul>
     *   <li><b>RANGE</b> (price): min = min(all mins), max = max(all maxes),
     *       step = min(all steps) — widest range, finest granularity wins.</li>
     *   <li><b>CHIP_SELECT</b>: options union across categories
     *       (currently all categories share the same options, so first wins).</li>
     *   <li>GLOBAL filters not overridden by any category stay unchanged.</li>
     * </ul>
     *
     * @param categoryIds list of selected category IDs; empty → global defaults
     */
    public List<FilterItemDTO> getFiltersForMultiple(List<Integer> categoryIds) {
        Map<String, FilterItemDTO> base = globalBase();

        if (categoryIds == null || categoryIds.isEmpty()) {
            return sorted(base);
        }

        // Resolve IDs → category keys (single batch JPA call)
        List<String> categoryKeys = moduleCategoryRepository.findAllById(categoryIds)
                .stream()
                .map(ModuleCategory::getCategoryKey)
                .collect(Collectors.toList());

        if (categoryKeys.isEmpty()) {
            return sorted(base);
        }

        // Fetch all overrides for these keys in ONE query
        List<FilterConfig> allOverrides = filterConfigRepository
                .findByScopeAndScopeKeyInAndIsActiveTrueOrderByDisplayOrderAsc("CATEGORY", categoryKeys);

        // Group by filterKey so we can merge across categories
        Map<String, List<FilterConfig>> byFilterKey = new LinkedHashMap<>();
        for (FilterConfig fc : allOverrides) {
            byFilterKey.computeIfAbsent(fc.getFilterKey(), k -> new ArrayList<>()).add(fc);
        }

        // Apply merged override per filterKey
        for (Map.Entry<String, List<FilterConfig>> entry : byFilterKey.entrySet()) {
            List<FilterConfig> configs = entry.getValue();
            FilterConfig first = configs.get(0);

            if ("RANGE".equals(first.getFilterType())) {
                base.put(entry.getKey(), mergeRanges(configs));
            } else {
                // CHIP_SELECT: union; currently all categories share options so first is fine
                base.put(entry.getKey(), toDTO(first));
            }
        }

        return sorted(base);
    }

    // ─── Range merge ──────────────────────────────────────────────────────────

    /**
     * Merges multiple RANGE filter configs into one.
     *
     * <pre>
     * Crop:      min=0, max=200_000,   step=500
     * Equipment: min=0, max=5_000_000, step=10_000
     *
     * Merged →   min=0, max=5_000_000, step=500   (widest range, finest step)
     * </pre>
     */
    private FilterItemDTO mergeRanges(List<FilterConfig> configs) {
        long mergedMin  = Long.MAX_VALUE;
        long mergedMax  = Long.MIN_VALUE;
        long mergedStep = Long.MAX_VALUE;

        for (FilterConfig fc : configs) {
            Map<String, Object> c = fc.getConfig();
            mergedMin  = Math.min(mergedMin,  ((Number) c.get("min")).longValue());
            mergedMax  = Math.max(mergedMax,  ((Number) c.get("max")).longValue());
            mergedStep = Math.min(mergedStep, ((Number) c.get("step")).longValue());
        }

        // displayMax from the config that has the highest max value
        final long finalMax = mergedMax;
        String dispMax = configs.stream()
                .filter(fc -> ((Number) fc.getConfig().get("max")).longValue() == finalMax)
                .findFirst()
                .map(fc -> (String) fc.getConfig().getOrDefault("displayMax", "₹" + finalMax))
                .orElse("₹" + finalMax);

        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("min",        mergedMin  == Long.MAX_VALUE ? 0L    : mergedMin);
        merged.put("max",        mergedMax  == Long.MIN_VALUE ? 0L    : mergedMax);
        merged.put("step",       mergedStep == Long.MAX_VALUE ? 1000L : mergedStep);
        merged.put("displayMin", "₹0");
        merged.put("displayMax", dispMax);

        FilterConfig ref = configs.get(0);
        return FilterItemDTO.builder()
                .filterKey(ref.getFilterKey())
                .labelEn(ref.getLabelEn())
                .labelHi(ref.getLabelHi())
                .filterType(ref.getFilterType())
                .displayOrder(ref.getDisplayOrder())
                .config(merged)
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Load global base filters into an ordered map keyed by filterKey. */
    private Map<String, FilterItemDTO> globalBase() {
        List<FilterConfig> globals = filterConfigRepository
                .findByScopeAndScopeKeyIsNullAndIsActiveTrueOrderByDisplayOrderAsc("GLOBAL");
        Map<String, FilterItemDTO> base = new LinkedHashMap<>();
        for (FilterConfig f : globals) {
            base.put(f.getFilterKey(), toDTO(f));
        }
        return base;
    }

    /** Sort map values by displayOrder. */
    private List<FilterItemDTO> sorted(Map<String, FilterItemDTO> map) {
        return map.values().stream()
                .sorted(Comparator.comparingInt(FilterItemDTO::getDisplayOrder))
                .collect(Collectors.toList());
    }

    private FilterItemDTO toDTO(FilterConfig f) {
        return FilterItemDTO.builder()
                .filterKey(f.getFilterKey())
                .labelEn(f.getLabelEn())
                .labelHi(f.getLabelHi())
                .filterType(f.getFilterType())
                .displayOrder(f.getDisplayOrder())
                .config(f.getConfig())
                .build();
    }
}
