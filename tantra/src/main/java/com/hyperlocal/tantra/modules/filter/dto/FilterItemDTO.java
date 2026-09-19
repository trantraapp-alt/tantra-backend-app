package com.hyperlocal.tantra.modules.filter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * One filter in the filter bottom-sheet.
 *
 * <pre>
 * CHIP_SELECT example:
 * {
 *   "filterKey"    : "listingType",
 *   "labelEn"      : "Listing Type",
 *   "labelHi"      : "लिस्टिंग प्रकार",
 *   "filterType"   : "CHIP_SELECT",
 *   "displayOrder" : 1,
 *   "config": {
 *     "multiSelect"  : false,
 *     "defaultValue" : "ALL",
 *     "options": [
 *       {"value": "ALL",  "labelEn": "All",  "labelHi": "सभी"},
 *       {"value": "SELL", "labelEn": "Sell", "labelHi": "बेचना"},
 *       {"value": "RENT", "labelEn": "Rent", "labelHi": "किराया"}
 *     ]
 *   }
 * }
 *
 * RANGE example:
 * {
 *   "filterKey"    : "priceRange",
 *   "labelEn"      : "Price Range",
 *   "labelHi"      : "मूल्य सीमा",
 *   "filterType"   : "RANGE",
 *   "displayOrder" : 2,
 *   "config": {
 *     "min"        : 0,
 *     "max"        : 200000,
 *     "step"       : 500,
 *     "displayMin" : "₹0",
 *     "displayMax" : "₹2L+"
 *   }
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterItemDTO {

    private String filterKey;
    private String labelEn;
    private String labelHi;
    private String filterType;
    private Integer displayOrder;

    /**
     * Polymorphic payload — shape driven by filterType.
     * Jackson serialises Map<String, Object> directly as a JSON object.
     */
    private Map<String, Object> config;
}
