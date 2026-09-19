package com.hyperlocal.tantra.modules.admin.dto;

import lombok.Data;

/**
 * Request body for POST /api/v1/admin/users/{userId}/subscription
 */
@Data
public class AdminGrantSubscriptionRequest {

    /** Plan ID to assign. */
    private Integer planId;

    /** Duration in days from today. e.g. 30, 90, 365 */
    private int durationDays;

    /** Optional note shown in subscription record. */
    private String notes;
}
