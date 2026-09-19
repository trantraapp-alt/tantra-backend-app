package com.hyperlocal.tantra.modules.subscription.repository;

import com.hyperlocal.tantra.modules.subscription.entity.UserSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    Optional<UserSubscription> findBySubscriptionId(String subscriptionId);

    /** The single active subscription for a user (buyer or seller), if any. */
    @Query("select s from UserSubscription s where s.userId = :userId " +
           "and s.status = 'ACTIVE' and s.expiresAt > :now")
    Optional<UserSubscription> findActiveByUserId(@Param("userId") String userId,
                                                   @Param("now") LocalDateTime now);

    /** Full subscription history for a user (latest first). */
    List<UserSubscription> findByUserIdOrderByCreatedAtDesc(String userId);

    /** All subscriptions expiring before a given date that are still marked ACTIVE (for the expiry job). */
    @Query("select s from UserSubscription s where s.status = 'ACTIVE' and s.expiresAt < :threshold")
    List<UserSubscription> findExpired(@Param("threshold") LocalDateTime threshold);

    /** Admin queue — all subscriptions, paginated, optional status filter. */
    @Query("select s from UserSubscription s " +
           "where (:status is null or s.status = :status) " +
           "order by s.createdAt desc")
    Page<UserSubscription> findAll(@Param("status") String status, Pageable pageable);

    boolean existsByUserIdAndStatus(String userId, String status);
}
