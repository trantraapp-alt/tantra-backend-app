package com.hyperlocal.tantra.modules.auth.dto;

import lombok.Data;

@Data
public class SignUpRequestDTO {
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String password;
    private String appUsageRole; // USER (buy/sell/rent/manage) or ADMIN (catalog + verification) — stored as ROLE_<value>
    private String preferredLanguage; // HI, EN
}