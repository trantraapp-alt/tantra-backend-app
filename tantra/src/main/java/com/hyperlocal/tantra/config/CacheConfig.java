package com.hyperlocal.tantra.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory caching. For production scale, swap ConcurrentMapCacheManager for a
 * Spring + Redis CacheManager (same cache annotations, different bean — zero code change).
 *
 * Cache names:
 *   forms        — FormDefinition per categoryId+listingType (evicted on admin form save)
 *   optionItems  — OptionItem lists per setKey+parentId (evicted on admin option save)
 *   homeFeed     — assembled HomeResponse per district+lang (10-min TTL in Caffeine/Redis prod)
 *   planCache    — active SubscriptionPlan per userId (5-min TTL)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String FORMS          = "forms";
    public static final String OPTION_ITEMS   = "optionItems";
    public static final String HOME_FEED      = "homeFeed";
    public static final String PLAN_CACHE     = "planCache";
    public static final String PROMO_CARDS    = "promoCards";
    public static final String PUBLIC_STATS   = "publicStats";
    public static final String CAROUSEL_ITEMS = "carouselItems";
    public static final String DEAL_CARDS     = "dealCards";
    /** Guards last_active_at DB writes — stores last-ping timestamp per userId, 15-min cooldown. */
    public static final String LAST_ACTIVE    = "lastActive";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(FORMS, OPTION_ITEMS, HOME_FEED, PLAN_CACHE, PROMO_CARDS, PUBLIC_STATS, CAROUSEL_ITEMS, DEAL_CARDS, LAST_ACTIVE);
    }
}
