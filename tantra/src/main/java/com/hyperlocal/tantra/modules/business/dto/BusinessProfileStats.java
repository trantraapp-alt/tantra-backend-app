package com.hyperlocal.tantra.modules.business.dto;

import lombok.Data;

/**
 * Admin "Approval Tracker" summary — counts of business profiles by verification status. Each count is a
 * clickable tile in the dashboard; tapping one opens the matching list
 * ({@code GET /api/v1/admin/business-profiles?status=...}). {@code reviewedByMe} is the current admin's own
 * action tally.
 */
@Data
public class BusinessProfileStats {

    private long total;
    private long pending;
    private long approved;
    private long rejected;
    private long blocked;

    private ReviewedByMe reviewedByMe;

    @Data
    public static class ReviewedByMe {
        private long approved;
        private long rejected;
        private long blocked;
    }
}
