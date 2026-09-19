package com.hyperlocal.tantra.modules.listing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * Full filter form definition returned to the frontend.
 * categoryId is null when the form is global (home / all-listings browse).
 * groups are ordered by displayOrder ascending — render them top-to-bottom in the filter drawer.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterFormResponse {

    /** The category this filter form was built for. Null = global (no category). */
    private Integer categoryId;

    /**
     * Ordered list of filter groups.
     * System groups (listingType, priceRange, postedWithin, sellerType, location) appear first.
     * Category-specific attribute groups (variety, breed, brand, etc.) follow.
     */
    private List<FilterGroup> groups;
}
