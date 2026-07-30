package com.hyperlocal.tantra.modules.listing.dto;

import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Partial update for the My-Listings grid — send only what changed. Only fields flagged
 * {@code inlineEditable} in the form metadata are applied (backend-enforced); {@code status}
 * lets the owner mark a listing SOLD/INACTIVE.
 */
@Data
public class ListingPatchRequest {
    private BigDecimal actualPrice;
    private BigDecimal offeredPrice;
    private BigDecimal quantity;
    private String unit;
    private Boolean isNegotiable;
    private ListingStatus status;
    private Map<String, Object> attributes;
}
