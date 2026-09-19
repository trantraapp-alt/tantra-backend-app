package com.hyperlocal.tantra.modules.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full user profile shown on the admin user detail screen.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserDetailDTO {

    // ── Basic Info ────────────────────────────────────────────────────────────
    private String userId;
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String preferredLanguage;
    private String appUsageRole;
    private LocalDateTime joinedAt;
    private LocalDateTime lastLoginAt;

    /** "ACTIVE" or "BLOCKED" */
    private String status;
    private String blockedReason;
    private LocalDateTime blockedAt;

    // ── Activity Summary ─────────────────────────────────────────────────────
    private ActivitySummary activity;

    // ── Subscription ─────────────────────────────────────────────────────────
    private SubscriptionInfo subscription;

    // ── Business Profile ─────────────────────────────────────────────────────
    private BusinessProfileInfo businessProfile;

    // ── Addresses ────────────────────────────────────────────────────────────
    private List<AddressSummary> addresses;

    // ── Inner types ───────────────────────────────────────────────────────────

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ActivitySummary {
        private long totalListings;
        private long activeListings;
        private long soldListings;
        private LocalDateTime lastListingAt;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubscriptionInfo {
        private String subscriptionId;
        private String planName;
        private String planKey;
        private LocalDateTime startedAt;
        private LocalDateTime expiresAt;
        /** ACTIVE / EXPIRED / CANCELLED / NONE */
        private String status;
        private String grantedBy;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BusinessProfileInfo {
        private String profileId;
        private String businessName;
        private String profileType;
        /** PENDING / APPROVED / REJECTED / BLOCKED */
        private String verificationStatus;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddressSummary {
        private String addressId;
        private String label;
        private String fullAddress;
        private String district;
        private String state;
        private Boolean isDefault;
    }
}
