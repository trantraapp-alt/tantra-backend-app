package com.hyperlocal.tantra.modules.listing.dto;

import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.listing.model.Address;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Payload the app posts to create a listing. Common fields are named explicitly; everything
 * category-specific arrives in {@code attributes} keyed by the form's field keys.
 */
@Data
public class ListingRequest {
    private Integer categoryId;
    private Integer moduleId;
    private ListingType listingType = ListingType.SELL;

    private BigDecimal actualPrice;
    private BigDecimal offeredPrice;
    private BigDecimal quantity;
    private String unit;
    private Boolean isNegotiable;

    private Boolean useDefaultAddress;
    private Address address;

    private List<String> images;
    private Map<String, Object> attributes;
}
