package com.hyperlocal.tantra.modules.listing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.listing.model.FilterInputType;
import lombok.Data;

import java.util.List;

/**
 * One collapsible section in the filter drawer.
 * RADIO/DROPDOWN groups carry an options list and a single queryParam.
 * PRICE_RANGE and LOCATION carry queryParams (a list) and no options.
 * Category-specific attribute groups carry attributeKey instead of queryParam —
 * the frontend passes the selected value as ?attributes[attributeKey]=value.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterGroup {

    private String groupKey;
    private LocalizedText label;
    private FilterInputType inputType;
    private int displayOrder;

    /** Single API query param — used for RADIO / DROPDOWN / MULTISELECT groups. */
    private String queryParam;

    /** Multiple API query params — used for PRICE_RANGE (minPrice + maxPrice) and LOCATION (district + state). */
    private List<String> queryParams;

    /**
     * For category-specific attribute filters: the key inside the listing's attributes JSONB.
     * When non-null, the frontend should pass the selected value as ?attributeKey=value (future).
     * Null for all system filter groups.
     */
    private String attributeKey;

    /** The optionSetKey from the form definition — for cascading lookup if needed. */
    private String optionSetKey;

    /** Resolved inline options for RADIO / DROPDOWN / MULTISELECT. */
    private List<FilterOption> options;

    /** PRICE_RANGE only: lowest offeredPrice in this category (slider start). */
    private java.math.BigDecimal min;

    /** PRICE_RANGE only: highest offeredPrice in this category (slider end). */
    private java.math.BigDecimal max;

    /** PRICE_RANGE only: slider increment step (default 100). */
    private Integer step;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FilterOption {
        private String value;
        private LocalizedText label;
        /** option_item.id — used for cascading child lookups via /option-sets/{key}/items?parentItemId= */
        private Integer id;
        /** parent option_item.id — non-null for child options in a cascading set. */
        private Integer parentId;
    }
}
