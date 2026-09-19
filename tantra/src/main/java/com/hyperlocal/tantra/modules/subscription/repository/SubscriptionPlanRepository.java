package com.hyperlocal.tantra.modules.subscription.repository;

import com.hyperlocal.tantra.modules.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Integer> {

    Optional<SubscriptionPlan> findByPlanKey(String planKey);

    List<SubscriptionPlan> findByIsActiveTrueOrderBySortWeightAsc();

    List<SubscriptionPlan> findAllByOrderBySortWeightAsc();

    boolean existsByPlanKey(String planKey);
}
