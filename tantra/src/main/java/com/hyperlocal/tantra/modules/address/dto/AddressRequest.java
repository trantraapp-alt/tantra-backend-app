package com.hyperlocal.tantra.modules.address.dto;

import lombok.Data;

/**
 * Create/update payload for a saved address. The full edit form sends all fields; `isDefault`
 * marks this address as the user's default (backend unsets any previous default).
 */
@Data
public class AddressRequest {
    private String label;
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
    private Boolean isDefault;
}
