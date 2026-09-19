package com.hyperlocal.tantra.modules.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Public seller info shown on the listing detail screen — "Seller Information" card.
 *
 * Fields map to the 4 tiles:
 *   STATUS       → verifiedSeller (true = "Verified Seller", false = "Regular Seller")
 *   MEMBER SINCE → memberSince
 *   LAST LOGIN   → lastLoginAt
 *   DELIVERY     → deliveryAvailable (comes from the listing itself, not this DTO)
 */
@Data
public class SellerInfoDTO {

    private String userId;

    /** Display name: firstName + lastName */
    private String name;

    /** City, State from seller's most recent active listing. Null if no listings yet. */
    private String location;

    /** true = at least one APPROVED business profile exists for this seller. */
    private Boolean verifiedSeller;

    /** user.createdAt — shown as "Member Since Jan 2025" */
    private LocalDateTime memberSince;

    /** user.lastLoginAt — shown as "Last login X ago" */
    private LocalDateTime lastLoginAt;

    /** Count of seller's active, non-deleted listings. */
    private Long totalActiveListings;
}
