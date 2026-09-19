package com.hyperlocal.tantra.modules.business.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

/**
 * Admin stats dashboard — approval tracker + three business-critical metrics.
 *
 * <p>Business metrics added:
 * <ol>
 *   <li><b>Time-to-approval</b> (avgApprovalDays / maxPendingDays / overdueCount) — seller retention signal.
 *       If sellers wait too long for verification they churn before posting their first listing.</li>
 *   <li><b>Success rate</b> — onboarding quality signal.
 *       High rejection rate means wrong sellers are applying or the form/instructions are unclear.</li>
 *   <li><b>Category breakdown</b> — business strategy signal.
 *       Identifies under-represented categories where targeted outreach can grow supply.</li>
 * </ol>
 *
 * <p>All counts are fetched in <b>3 DB round-trips</b> (single aggregation query each),
 * replacing the previous 8 individual count queries.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BusinessProfileStats {

    // ── Status counts ──────────────────────────────────────────────────────────
    private long total;
    private long pending;
    private long approved;
    private long rejected;
    private long blocked;

    // ── Time-to-approval — seller retention signal ────────────────────────────
    /**
     * Average days from profile submission to admin approval (APPROVED profiles only).
     * Null if no profile has been approved yet.
     */
    private Double avgApprovalDays;

    /**
     * Days the longest-waiting PENDING profile has been in the queue.
     * Zero if there are no pending profiles.
     */
    private Integer maxPendingDays;

    /**
     * Count of PENDING profiles older than the configured overdue threshold (default 5 days).
     * These sellers are at highest churn risk and should be prioritised.
     */
    private Long overdueCount;

    // ── Onboarding quality signal ─────────────────────────────────────────────
    /**
     * Percentage of <em>decided</em> profiles that were approved.
     * Formula: approved / (approved + rejected + blocked) × 100.
     * Excludes PENDING (not yet decided). Returns 0 if no decisions have been made.
     */
    private Double successRate;

    // ── Business strategy signal ──────────────────────────────────────────────
    /** Per-category profile counts, ordered by count descending. */
    private List<CategoryStat> categoryBreakdown;

    // ── Admin's own tally ─────────────────────────────────────────────────────
    /** Actions taken by the currently-authenticated admin in this session. */
    private ReviewedByMe reviewedByMe;

    // ── Inner types ────────────────────────────────────────────────────────────

    @Data
    public static class CategoryStat {
        /** Profile type key (e.g. "seed_dealer", "vet_clinic"). */
        private String profileType;
        private long count;
        /** Share of total profiles (rounded to 1 decimal place). */
        private double percentage;
    }

    @Data
    public static class ReviewedByMe {
        private long approved;
        private long rejected;
        private long blocked;
    }
}
