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

    /** Contact number entered on the listing form (default-address number or a newly typed one).
     *  Saved only on the listing — never written back to any saved address. */
    private String contactNumber;

    /** Seller's choice: true = show the contact number to buyers directly; false (default) = hide it,
     *  buyers must send a contact request the seller approves before the number is revealed. */
    private Boolean showContact;

    private Boolean useDefaultAddress;
    /** A saved address to snapshot into the listing (address book). Takes precedence over `address`. */
    private String addressId;
    private Address address;

    private List<String> images;
    private Map<String, Object> attributes;
}
