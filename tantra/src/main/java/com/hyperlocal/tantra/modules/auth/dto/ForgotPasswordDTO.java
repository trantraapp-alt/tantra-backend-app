package com.hyperlocal.tantra.modules.auth.dto;

import lombok.Data;

@Data
public class ForgotPasswordDTO {
    private String mobileNumber;
    private String otp;
    private String newPassword;
}