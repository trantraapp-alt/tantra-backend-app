package com.hyperlocal.tantra.modules.subscription.repository;

/**
 * @deprecated Replaced by {@link UserSubscriptionRepository}.
 * This interface is intentionally empty and NOT a Spring Data bean — @Repository removed
 * so Spring does not attempt to instantiate it against the deprecated SellerSubscription entity.
 */
@Deprecated
public interface SellerSubscriptionRepository {
    // intentionally empty — use UserSubscriptionRepository
}
