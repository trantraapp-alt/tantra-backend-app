package com.hyperlocal.tantra.modules.listing.dto;

import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Unified filter bag for category browse, nearby, and public seller listings.
 * Every field is optional; null = no filter applied.
 */
@Data
public class ListingFilter {

    /** Text search query — matched against search_vector. */
    private String q;

    private Integer moduleId;
    private Integer categoryId;

    private ListingType listingType;
    private ListingStatus status;

    /** Price filters apply to offered_price. */
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private String district;
    private String state;
    private String village;

    /** Geo-filter: user's current GPS coordinates + radius in km. */
    private Double latitude;
    private Double longitude;
    private Integer radiusKm;

    /** SUBSCRIBED = only premium sellers; null/ALL = everyone. */
    private String sellerType;

    /** TODAY / WEEK / MONTH / ALL */
    private String postedWithin;

    /** Seller whose listings to show (for public seller page). */
    private String userId;

    /**
     * Seller userId to exclude from results.
     * Set to the authenticated viewer's userId so sellers do not see
     * their own listings in the buyer-facing feed (browse, nearby, search, flash deals).
     * Leave null to include all sellers (e.g. unauthenticated requests, /by-seller page).
     */
    private String excludeUserId;

    /**
     * JSONB attribute filter — a compact JSON object built from attr_* query params.
     * e.g. attr_cropType=PULSE + attr_variety=BASMATI → {"cropType":"PULSE","variety":"BASMATI"}
     * Applied via PostgreSQL @> (contains) on the listing's attributes JSONB column.
     * Multiple entries are AND-combined. Null = no attribute filtering.
     */
    private String attrFilter;

    /**
     * Explicit sort field: "offeredPrice" | "createdAt" | null.
     * Null means default ordering (subscription premium first, then newest).
     */
    private String sortBy;

    /**
     * Sort direction: "asc" | "desc".
     * Only meaningful when sortBy is set. Defaults to "desc".
     */
    private String sortDir;

    /** True = only listings with at least one photo. */
    private Boolean withPhoto;

    /** True = only listings from sellers with an APPROVED business profile. */
    private Boolean verifiedSeller;
}
