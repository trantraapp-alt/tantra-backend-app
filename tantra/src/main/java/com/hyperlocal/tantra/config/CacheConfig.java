package com.hyperlocal.tantra.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory caching for rarely-changing form metadata and dropdown values, so opening a form
 * doesn't hit the DB every time. Uses Spring's built-in ConcurrentMapCacheManager (no extra
 * dependency); caches are evicted whenever an admin edits a form or an option set.
 *
 * <p>For production, swap in Caffeine (TTL + max size) — same annotations, just a different
 * CacheManager bean.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String FORMS = "forms";
    public static final String OPTION_ITEMS = "optionItems";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(FORMS, OPTION_ITEMS);
    }
}
