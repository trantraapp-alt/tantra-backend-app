package com.hyperlocal.tantra.modules.admin.dto;

import lombok.Data;

/**
 * Request body for POST /api/v1/admin/users/{userId}/block
 */
@Data
public class AdminBlockRequest {

    /**
     * One of: SPAM_LISTINGS | FAKE_PROFILE | ABUSIVE_BEHAVIOR | OTHER
     */
    private String reason;

    /** Optional free-text note from admin (shown in audit trail). */
    private String notes;
}
