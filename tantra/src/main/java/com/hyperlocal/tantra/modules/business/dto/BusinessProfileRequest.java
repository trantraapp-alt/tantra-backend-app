package com.hyperlocal.tantra.modules.business.dto;

import com.hyperlocal.tantra.modules.listing.model.Address;
import lombok.Data;

import java.util.Map;

/**
 * Create/update payload for a business profile. Common fields are top-level; type-specific fields
 * (from the business-profile form) go in {@code attributes}, keyed by fieldKey.
 */
@Data
public class BusinessProfileRequest {
    private String profileType;
    private String businessName;
    private Address address;
    private Boolean isVisible;
    private Map<String, Object> attributes;
}
