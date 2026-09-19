package com.hyperlocal.tantra.modules.listing.service;

import com.hyperlocal.tantra.modules.forms.entity.FormDefinition;
import com.hyperlocal.tantra.modules.forms.entity.OptionItem;
import com.hyperlocal.tantra.modules.forms.model.FormField;
import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.forms.repository.FormDefinitionRepository;
import com.hyperlocal.tantra.modules.forms.repository.OptionItemRepository;
import com.hyperlocal.tantra.modules.forms.repository.OptionSetRepository;
import com.hyperlocal.tantra.modules.listing.dto.FilterFormResponse;
import com.hyperlocal.tantra.modules.listing.dto.FilterGroup;
import com.hyperlocal.tantra.modules.listing.model.FilterInputType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the filter form for any listing browse screen.
 * System groups (listingType, priceRange, postedWithin, sellerType, location) are defined in code.
 * Category-specific groups (variety, breed, brand, etc.) are derived from the existing FormDefinition
 * for that category, resolving dropdown options from OptionItem rows.
 */
@Service
public class FilterFormService {

    @Autowired private FormDefinitionRepository formRepository;
    @Autowired private OptionSetRepository setRepository;
    @Autowired private OptionItemRepository itemRepository;
    @Autowired private com.hyperlocal.tantra.modules.listing.repository.ListingRepository listingRepository;

    private static final Set<String> FILTERABLE_ATTR_KEYS = new LinkedHashSet<>(Arrays.asList(
            "variety", "breed", "brand", "serviceType", "species", "model", "type",
            "cropType", "animalType", "equipmentType"
    ));

    public FilterFormResponse buildForCategory(Integer categoryId) {
        FilterFormResponse resp = new FilterFormResponse();
        resp.setCategoryId(categoryId);

        List<FilterGroup> groups = new ArrayList<>(systemGroups(categoryId));
        if (categoryId != null) {
            groups.addAll(categoryAttributeGroups(categoryId));
        }
        resp.setGroups(groups);
        return resp;
    }

    // ─── System groups ───────────────────────────────────────────────────────

    private List<FilterGroup> systemGroups(Integer categoryId) {
        List<FilterGroup> groups = new ArrayList<>();
        groups.add(listingTypeGroup());
        groups.add(priceRangeGroup(categoryId));
        groups.add(postedWithinGroup());
        groups.add(sellerTypeGroup());
        groups.add(locationGroup());
        return groups;
    }

    private FilterGroup listingTypeGroup() {
        FilterGroup g = new FilterGroup();
        g.setGroupKey("listingType");
        g.setLabel(LocalizedText.of("Listing Type", "सूचना प्रकार"));
        g.setInputType(FilterInputType.RADIO);
        g.setDisplayOrder(1);
        g.setQueryParam("listingType");
        g.setOptions(Arrays.asList(
                opt("SELL", "Sell",       "बेचें"),
                opt("RENT", "Rent",       "किराया"),
                opt("BUY",  "Buy",        "खरीदें"),
                opt("BOTH", "Sell & Rent", "बेचें और किराया")
        ));
        return g;
    }

    private FilterGroup priceRangeGroup(Integer categoryId) {
        FilterGroup g = new FilterGroup();
        g.setGroupKey("priceRange");
        g.setLabel(LocalizedText.of("Price Range", "मूल्य सीमा"));
        g.setInputType(FilterInputType.PRICE_RANGE);
        g.setDisplayOrder(2);
        g.setQueryParams(Arrays.asList("minPrice", "maxPrice"));
        if (categoryId != null) {
            java.math.BigDecimal min = listingRepository.findMinPrice(categoryId);
            java.math.BigDecimal max = listingRepository.findMaxPrice(categoryId);
            if (min != null && max != null) {
                g.setMin(min);
                g.setMax(max);
                g.setStep(100);
            }
        }
        return g;
    }

    private FilterGroup postedWithinGroup() {
        FilterGroup g = new FilterGroup();
        g.setGroupKey("postedWithin");
        g.setLabel(LocalizedText.of("Posted Within", "कब पोस्ट किया"));
        g.setInputType(FilterInputType.RADIO);
        g.setDisplayOrder(3);
        g.setQueryParam("postedWithin");
        g.setOptions(Arrays.asList(
                opt("TODAY", "Today",      "आज"),
                opt("WEEK",  "This Week",  "इस सप्ताह"),
                opt("MONTH", "This Month", "इस महीने")
        ));
        return g;
    }

    private FilterGroup sellerTypeGroup() {
        FilterGroup g = new FilterGroup();
        g.setGroupKey("sellerType");
        g.setLabel(LocalizedText.of("Seller Type", "विक्रेता प्रकार"));
        g.setInputType(FilterInputType.RADIO);
        g.setDisplayOrder(4);
        g.setQueryParam("sellerType");
        g.setOptions(Arrays.asList(
                opt("ALL",        "All Sellers",     "सभी विक्रेता"),
                opt("SUBSCRIBED", "Premium Sellers", "प्रीमियम विक्रेता")
        ));
        return g;
    }

    private FilterGroup locationGroup() {
        FilterGroup g = new FilterGroup();
        g.setGroupKey("location");
        g.setLabel(LocalizedText.of("Location", "स्थान"));
        g.setInputType(FilterInputType.LOCATION);
        g.setDisplayOrder(5);
        g.setQueryParams(Arrays.asList("district", "state"));
        return g;
    }

    // ─── Category-specific attribute groups ──────────────────────────────────

    private List<FilterGroup> categoryAttributeGroups(Integer categoryId) {
        Optional<FormDefinition> defOpt = formRepository
                .findFirstByCategoryIdAndListingTypeAndIsActiveTrueOrderByVersionDesc(categoryId, ListingType.SELL);
        if (defOpt.isEmpty()) {
            defOpt = formRepository.findFirstByCategoryIdAndListingTypeAndIsActiveTrueOrderByVersionDesc(
                    categoryId, ListingType.BOTH);
        }
        if (defOpt.isEmpty()) return Collections.emptyList();

        List<FormField> fields = defOpt.get().getFields();
        if (fields == null || fields.isEmpty()) return Collections.emptyList();

        List<FilterGroup> result = new ArrayList<>();
        int order = 10;
        for (FormField field : fields) {
            if (!FILTERABLE_ATTR_KEYS.contains(field.getFieldKey())) continue;
            if (field.getOptionSetKey() == null || field.getOptionSetKey().isBlank()) continue;

            FilterGroup g = new FilterGroup();
            g.setGroupKey(field.getFieldKey());
            g.setLabel(field.getLabel());
            g.setInputType(FilterInputType.DROPDOWN);
            g.setDisplayOrder(order++);
            g.setAttributeKey(field.getFieldKey());
            g.setOptionSetKey(field.getOptionSetKey());
            g.setOptions(resolveOptions(field.getOptionSetKey()));
            result.add(g);
        }
        return result;
    }

    private List<FilterGroup.FilterOption> resolveOptions(String setKey) {
        return setRepository.findBySetKey(setKey)
                .map(set -> itemRepository
                        .findByOptionSetIdAndIsActiveTrueOrderByDisplayOrderAsc(set.getId())
                        .stream()
                        .map(this::toFilterOption)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private FilterGroup.FilterOption toFilterOption(OptionItem item) {
        FilterGroup.FilterOption fo = new FilterGroup.FilterOption();
        fo.setValue(item.getItemKey());
        fo.setLabel(item.getLabel());
        fo.setId(item.getId());
        fo.setParentId(item.getParentItemId());
        return fo;
    }

    private FilterGroup.FilterOption opt(String value, String en, String hi) {
        FilterGroup.FilterOption fo = new FilterGroup.FilterOption();
        fo.setValue(value);
        fo.setLabel(LocalizedText.of(en, hi));
        return fo;
    }
}
