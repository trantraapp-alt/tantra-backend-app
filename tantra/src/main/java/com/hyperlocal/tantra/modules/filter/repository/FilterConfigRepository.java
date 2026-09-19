package com.hyperlocal.tantra.modules.filter.repository;

import com.hyperlocal.tantra.modules.filter.entity.FilterConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilterConfigRepository extends JpaRepository<FilterConfig, Integer> {

    /** All active GLOBAL filters (scope_key IS NULL), sorted for display. */
    List<FilterConfig> findByScopeAndScopeKeyIsNullAndIsActiveTrueOrderByDisplayOrderAsc(String scope);

    /**
     * Category-specific active overrides for a single category.
     * Used by the single-categoryId path.
     */
    List<FilterConfig> findByScopeAndScopeKeyAndIsActiveTrueOrderByDisplayOrderAsc(
            String scope, String scopeKey);

    /**
     * Batch fetch of category overrides for multiple category keys in one query.
     * Used by the multi-category merge path.
     */
    List<FilterConfig> findByScopeAndScopeKeyInAndIsActiveTrueOrderByDisplayOrderAsc(
            String scope, List<String> scopeKeys);
}
