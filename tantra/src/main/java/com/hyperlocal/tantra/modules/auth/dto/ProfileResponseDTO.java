package com.hyperlocal.tantra.modules.auth.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProfileResponseDTO {
    private String userId;
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String appUsageRole;
    private String preferredLanguage;
    private String sessionToken;
    private LocalDateTime sessionExpiry;
}