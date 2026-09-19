package com.hyperlocal.tantra.modules.business.repository;

import com.hyperlocal.tantra.modules.business.entity.BusinessProfile;
import com.hyperlocal.tantra.modules.business.model.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {

    List<BusinessProfile> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId);
    Optional<BusinessProfile> findByProfileIdAndIsDeletedFalse(String profileId);
    Page<BusinessProfile> findByVerificationStatusAndIsDeletedFalse(VerificationStatus status, Pageable pageable);
    Page<BusinessProfile> findByVerificationStatusInAndIsDeletedFalse(List<VerificationStatus> statuses, Pageable pageable);
    List<BusinessProfile> findByUserIdAndVerificationStatusAndIsDeletedFalse(String userId, VerificationStatus status);
    boolean existsByProfileId(String profileId);

    Page<BusinessProfile> findByVerificationStatusAndProfileTypeAndIsDeletedFalse(
            VerificationStatus status, String profileType, Pageable pageable);

    long countByIsDeletedFalse();
    long countByVerificationStatusAndIsDeletedFalse(VerificationStatus status);
    long countByVerifiedByAndVerificationStatusAndIsDeletedFalse(String verifiedBy, VerificationStatus status);

    // ─── Enhanced Admin Stats — 3 queries replace 8 individual count queries ──

    /**
     * Single-pass aggregation for all status counts + time-to-approval metrics.
     * Uses PostgreSQL FILTER clause for conditional aggregation — O(n) scan, not 5 × O(n).
     *
     * @param overdueThresholdDays profiles pending longer than this are counted as overdue (default 5)
     */
    /**
     * NOTE ON ALIASES: Spring Data JPA native-query projection matching is case-insensitive but does NOT
     * convert snake_case → camelCase. Aliases must therefore match getter names when both are lowercased.
     * e.g. getAvgApprovalDays() → "avgapprovaldays" → alias must be "avgapprovaldays" (no underscore).
     */
    @Query(value =
            "SELECT" +
            "  COUNT(*)                                                                    AS total," +
            "  COUNT(*) FILTER (WHERE verification_status = 'PENDING')                    AS pending," +
            "  COUNT(*) FILTER (WHERE verification_status = 'APPROVED')                   AS approved," +
            "  COUNT(*) FILTER (WHERE verification_status = 'REJECTED')                   AS rejected," +
            "  COUNT(*) FILTER (WHERE verification_status = 'BLOCKED')                    AS blocked," +
            "  COALESCE(ROUND(CAST(" +
            "    AVG(EXTRACT(EPOCH FROM (verified_at - created_at)) / 86400.0)" +
            "    FILTER (WHERE verified_at IS NOT NULL AND verification_status = 'APPROVED')" +
            "  AS numeric), 1), 0)                                                        AS avgapprovaldays," +
            "  CAST(COALESCE(MAX(EXTRACT(EPOCH FROM (NOW() - created_at)) / 86400)" +
            "    FILTER (WHERE verification_status = 'PENDING'), 0) AS integer)           AS maxpendingdays," +
            "  COUNT(*) FILTER (WHERE verification_status = 'PENDING'" +
            "    AND created_at < NOW() - (CAST(:overdueThresholdDays AS integer) * INTERVAL '1 day'))" +
            "                                                                              AS overduecount" +
            " FROM business_profiles WHERE is_deleted = false",
            nativeQuery = true)
    ProfileSummaryProjection fetchSummaryStats(@Param("overdueThresholdDays") int overdueThresholdDays);

    /**
     * Single-pass aggregation of actions taken by one admin.
     * Replaces 3 individual countByVerifiedBy queries.
     */
    @Query(value =
            "SELECT" +
            "  COUNT(*) FILTER (WHERE verification_status = 'APPROVED') AS approved," +
            "  COUNT(*) FILTER (WHERE verification_status = 'REJECTED') AS rejected," +
            "  COUNT(*) FILTER (WHERE verification_status = 'BLOCKED')  AS blocked" +
            " FROM business_profiles" +
            " WHERE is_deleted = false AND verified_by = :adminId",
            nativeQuery = true)
    AdminReviewProjection fetchAdminReviewStats(@Param("adminId") String adminId);

    /**
     * Category (profile_type) distribution, ordered by count descending.
     * Used to identify under-represented business categories.
     */
    @Query(value =
            "SELECT profile_type AS profiletype, COUNT(*) AS count" +
            " FROM business_profiles" +
            " WHERE is_deleted = false" +
            " GROUP BY profile_type" +
            " ORDER BY count DESC",
            nativeQuery = true)
    List<CategoryCountProjection> fetchCategoryBreakdown();

    // ─── Projection interfaces ────────────────────────────────────────────────

    /**
     * Maps the single-row summary aggregation result.
     * Getter names must match SQL aliases case-insensitively (no underscore conversion).
     * e.g. SQL alias "avgapprovaldays" → getter getAvgapprovaldays() — NOT getAvgApprovalDays().
     */
    interface ProfileSummaryProjection {
        Long getTotal();
        Long getPending();
        Long getApproved();
        Long getRejected();
        Long getBlocked();
        Double getAvgapprovaldays();
        Integer getMaxpendingdays();
        Long getOverduecount();
    }

    /** Maps the per-admin review tally. */
    interface AdminReviewProjection {
        Long getApproved();
        Long getRejected();
        Long getBlocked();
    }

    /** Maps one row of the category breakdown. SQL alias "profiletype" → getProfiletype(). */
    interface CategoryCountProjection {
        String getProfiletype();
        Long getCount();
    }

    // ─── Directory browse (subscription-first + geo) ──────────────────────────

    @Query(value = "SELECT bp.* FROM business_profiles bp" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = bp.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " LEFT JOIN subscription_plans sp ON sp.id = ss.plan_id" +
            " WHERE bp.is_deleted = false AND bp.is_active = true" +
            " AND bp.verification_status = 'APPROVED'" +
            " AND (:profileType IS NULL OR bp.profile_type = :profileType)" +
            " AND (:district IS NULL OR bp.address->>'district' ILIKE :district)" +
            " AND (:latMin IS NULL OR (" +
            "       bp.latitude  BETWEEN :latMin AND :latMax" +
            "   AND bp.longitude BETWEEN :lngMin AND :lngMax))" +
            " ORDER BY COALESCE(sp.sort_weight, 99) ASC, bp.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM business_profiles bp" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = bp.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " WHERE bp.is_deleted = false AND bp.is_active = true" +
            " AND bp.verification_status = 'APPROVED'" +
            " AND (:profileType IS NULL OR bp.profile_type = :profileType)" +
            " AND (:district IS NULL OR bp.address->>'district' ILIKE :district)" +
            " AND (:latMin IS NULL OR (" +
            "       bp.latitude  BETWEEN :latMin AND :latMax" +
            "   AND bp.longitude BETWEEN :lngMin AND :lngMax))",
            nativeQuery = true)
    Page<BusinessProfile> findDirectory(
            @Param("profileType") String profileType,
            @Param("district")    String district,
            @Param("latMin")      Double latMin,
            @Param("latMax")      Double latMax,
            @Param("lngMin")      Double lngMin,
            @Param("lngMax")      Double lngMax,
            Pageable pageable);

    @Query(value = "SELECT bp.* FROM business_profiles bp" +
            " INNER JOIN (" +
            "   SELECT user_id, SUM(contact_reveal_count) AS reveal_total" +
            "   FROM listings" +
            "   WHERE is_deleted = false AND status = 'ACTIVE'" +
            "   GROUP BY user_id" +
            " ) stats ON stats.user_id = bp.user_id" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = bp.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " LEFT JOIN subscription_plans sp ON sp.id = ss.plan_id" +
            " WHERE bp.is_deleted = false AND bp.is_active = true" +
            " AND bp.verification_status = 'APPROVED'" +
            " AND (:district IS NULL OR bp.address->>'district' ILIKE :district)" +
            " ORDER BY COALESCE(sp.sort_weight, 99) ASC, stats.reveal_total DESC" +
            " LIMIT :limit",
            nativeQuery = true)
    List<BusinessProfile> findTopSellers(@Param("district") String district,
                                          @Param("limit") int limit);

    @Query(value = "SELECT bp.* FROM business_profiles bp" +
            " INNER JOIN user_subscriptions ss" +
            " ON ss.user_id = bp.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " INNER JOIN subscription_plans sp" +
            " ON sp.id = ss.plan_id AND sp.profile_highlight = true AND sp.home_feed_slots > 0" +
            " WHERE bp.is_deleted = false AND bp.is_active = true" +
            " AND bp.verification_status = 'APPROVED'" +
            " AND (:district IS NULL OR bp.address->>'district' ILIKE :district)" +
            " ORDER BY sp.sort_weight ASC, bp.created_at DESC" +
            " LIMIT :limit",
            nativeQuery = true)
    List<BusinessProfile> findFeaturedProfiles(@Param("district") String district,
                                                @Param("limit") int limit);
}
