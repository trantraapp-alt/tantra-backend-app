package com.hyperlocal.tantra.modules.auth.dto;

import lombok.Data;

@Data
public class SignUpRequestDTO {
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String password;
    private String appUsageRole; // BUYER, SELLER, BOTH
    private String preferredLanguage; // HI, EN
}