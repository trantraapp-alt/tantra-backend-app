package com.hyperlocal.tantra.modules.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Compact user row shown in the admin user list screen.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserListDTO {

    private String userId;
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String appUsageRole;

    /** "ACTIVE" or "BLOCKED" */
    private String status;

    private LocalDateTime joinedAt;
    private LocalDateTime lastLoginAt;

    /** "FREE" or the plan name e.g. "PREMIUM" */
    private String subscriptionBadge;

    /** "NONE" / "PENDING" / "APPROVED" / "REJECTED" */
    private String businessProfileBadge;
}
