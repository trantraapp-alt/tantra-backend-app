package com.hyperlocal.tantra.modules.listing.model;

import lombok.Data;

/**
 * The common address block shared by every category, stored as a jsonb column on a listing.
 * Kept out of the dynamic form fields because it is identical across all categories.
 */
@Data
public class Address {
    private String fullAddress;
    private String country;
    private String state;
    private String district;
    private String city;
    private String village;
    private String pinCode;
    private Double latitude;
    private Double longitude;
    private String mobileNumber;
    private String altMobileNumber;
}
