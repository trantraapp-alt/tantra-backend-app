package com.hyperlocal.tantra.modules.auth.dto;

import lombok.Data;

@Data
public class SignInRequestDTO {
    private String mobileNumber;
    private String password;
}