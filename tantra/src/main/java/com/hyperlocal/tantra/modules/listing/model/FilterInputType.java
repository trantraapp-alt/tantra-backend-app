package com.hyperlocal.tantra.modules.listing.model;

/**
 * Widget type the frontend should render for a filter group.
 * PRICE_RANGE and LOCATION map to multiple query params; the rest map to a single queryParam.
 */
public enum FilterInputType {
    RADIO,
    DROPDOWN,
    MULTISELECT,
    PRICE_RANGE,  // queryParams: ["minPrice", "maxPrice"]
    LOCATION,     // queryParams: ["district", "state"]
    GEO_RADIUS    // queryParams: ["lat", "lng", "radius"]
}
