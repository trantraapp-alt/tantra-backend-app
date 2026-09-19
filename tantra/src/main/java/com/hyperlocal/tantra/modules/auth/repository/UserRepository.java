package com.hyperlocal.tantra.modules.auth.repository;

import com.hyperlocal.tantra.modules.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMobileNumber(String mobileNumber);

    Optional<User> findByUserId(String userId);

    // ── Admin search ──────────────────────────────────────────────────────────

    @Query(value =
        "SELECT u.* FROM users u " +
        "WHERE (:searchLike IS NULL " +
        "    OR u.mobile_number LIKE :searchLike " +
        "    OR LOWER(u.first_name || ' ' || u.last_name) LIKE LOWER(:searchLike)) " +
        "  AND (:filterUserId IS NULL OR u.user_id = :filterUserId) " +
        "  AND (:isBlocked IS NULL OR u.is_blocked = :isBlocked) " +
        "  AND (:fromDate IS NULL OR u.created_at >= CAST(:fromDate AS timestamp)) " +
        "  AND (:toDate   IS NULL OR u.created_at <= CAST(:toDate   AS timestamp)) " +
        "  AND (:hasSub IS NULL " +
        "    OR (:hasSub = TRUE AND EXISTS (" +
        "        SELECT 1 FROM user_subscriptions us " +
        "        WHERE us.user_id = u.user_id AND us.status = 'ACTIVE' AND us.expires_at > NOW())) " +
        "    OR (:hasSub = FALSE AND NOT EXISTS (" +
        "        SELECT 1 FROM user_subscriptions us " +
        "        WHERE us.user_id = u.user_id AND us.status = 'ACTIVE' AND us.expires_at > NOW()))) " +
        "ORDER BY u.created_at DESC",
        countQuery =
        "SELECT COUNT(*) FROM users u " +
        "WHERE (:searchLike IS NULL " +
        "    OR u.mobile_number LIKE :searchLike " +
        "    OR LOWER(u.first_name || ' ' || u.last_name) LIKE LOWER(:searchLike)) " +
        "  AND (:filterUserId IS NULL OR u.user_id = :filterUserId) " +
        "  AND (:isBlocked IS NULL OR u.is_blocked = :isBlocked) " +
        "  AND (:fromDate IS NULL OR u.created_at >= CAST(:fromDate AS timestamp)) " +
        "  AND (:toDate   IS NULL OR u.created_at <= CAST(:toDate   AS timestamp)) " +
        "  AND (:hasSub IS NULL " +
        "    OR (:hasSub = TRUE AND EXISTS (" +
        "        SELECT 1 FROM user_subscriptions us " +
        "        WHERE us.user_id = u.user_id AND us.status = 'ACTIVE' AND us.expires_at > NOW())) " +
        "    OR (:hasSub = FALSE AND NOT EXISTS (" +
        "        SELECT 1 FROM user_subscriptions us " +
        "        WHERE us.user_id = u.user_id AND us.status = 'ACTIVE' AND us.expires_at > NOW())))",
        nativeQuery = true)
    Page<User> adminSearch(
        @Param("searchLike")   String searchLike,
        @Param("filterUserId") String filterUserId,
        @Param("isBlocked")    Boolean isBlocked,
        @Param("fromDate")     String fromDate,
        @Param("toDate")       String toDate,
        @Param("hasSub")       Boolean hasSub,
        Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :ts WHERE u.mobileNumber = :mobile")
    void updateLastLoginAt(@Param("mobile") String mobile, @Param("ts") LocalDateTime ts);

    /** Called by LastActiveFilter with a 15-min cache cooldown — avoids per-request DB writes. */
    @Modifying
    @Query("UPDATE User u SET u.lastActiveAt = :ts WHERE u.userId = :userId")
    void updateLastActiveAt(@Param("userId") String userId, @Param("ts") LocalDateTime ts);
}