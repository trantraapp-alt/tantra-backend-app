package com.hyperlocal.tantra.modules.listing.repository;

import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    Optional<Listing> findByListingId(String listingId);
    List<Listing> findByUserIdAndIsDeletedFalse(String userId);

    /** Admin-only: returns ALL listings for a user including deleted ones. */
    List<Listing> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByListingId(String listingId);

    /** Total active (non-deleted, active) listings for a seller — used on the seller info card. */
    long countByUserIdAndIsActiveTrueAndIsDeletedFalse(String userId);

    // ─── My Listings ─────────────────────────────────────────────────────────

    @Query("select l from Listing l where l.userId = :userId and l.isDeleted = false " +
           "and (:listingType is null or l.listingType = :listingType) " +
           "and (:status is null or l.status = :status)")
    Page<Listing> findMine(@Param("userId") String userId,
                            @Param("listingType") ListingType listingType,
                            @Param("status") ListingStatus status,
                            Pageable pageable);

    // ─── Category Browse (subscription-first sort) ────────────────────────────

    @Query(value =
            "SELECT l.* FROM listings l" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " LEFT JOIN subscription_plans sp ON sp.id = ss.plan_id" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND (:categoryId      IS NULL OR l.category_id  = :categoryId)" +
            " AND (:moduleId        IS NULL OR l.module_id    = :moduleId)" +
            " AND (:listingType     IS NULL OR l.listing_type = :listingType)" +
            " AND (:minPrice        IS NULL OR l.offered_price >= :minPrice)" +
            " AND (:maxPrice        IS NULL OR l.offered_price <= :maxPrice)" +
            " AND (:district        IS NULL OR l.address->>'district' ILIKE :district)" +
            " AND (:state           IS NULL OR l.address->>'state'    ILIKE :state)" +
            " AND (:userId          IS NULL OR l.user_id = :userId)" +
            " AND (:excludeUserId   IS NULL OR l.user_id != :excludeUserId)" +
            " AND (:attrFilter      IS NULL OR l.attributes @> CAST(:attrFilter AS jsonb))" +
            " AND (CAST(:postedAfter AS timestamp) IS NULL OR l.created_at >= CAST(:postedAfter AS timestamp))" +
            " AND (CAST(:latMin AS float8) IS NULL OR (" +
            "       l.latitude  BETWEEN CAST(:latMin AS float8) AND CAST(:latMax AS float8)" +
            "   AND l.longitude BETWEEN CAST(:lngMin AS float8) AND CAST(:lngMax AS float8)))" +
            " AND (CAST(:sellerType AS text) IS NULL OR CAST(:sellerType AS text) = 'ALL'" +
            "   OR (CAST(:sellerType AS text) = 'SUBSCRIBED' AND ss.id IS NOT NULL))" +
            " AND (CAST(:withPhoto AS boolean) = false OR (l.images IS NOT NULL AND jsonb_array_length(l.images) > 0))" +
            " AND (CAST(:verifiedSeller AS boolean) = false OR EXISTS (" +
            "       SELECT 1 FROM business_profiles bp WHERE bp.user_id = l.user_id" +
            "       AND bp.verification_status = 'APPROVED' AND bp.is_active = true AND bp.is_deleted = false))" +
            " ORDER BY" +
            " CASE WHEN :sortBy = 'offeredPrice' AND :sortDir = 'asc'  THEN l.offered_price END ASC  NULLS LAST," +
            " CASE WHEN :sortBy = 'offeredPrice' AND :sortDir = 'desc' THEN l.offered_price END DESC NULLS LAST," +
            " CASE WHEN :sortBy = 'createdAt'    AND :sortDir = 'asc'  THEN l.created_at    END ASC  NULLS LAST," +
            " CASE WHEN :sortBy = 'createdAt'    AND :sortDir = 'desc' THEN l.created_at    END DESC NULLS LAST," +
            " COALESCE(sp.sort_weight, 99) ASC, l.created_at DESC",
            countQuery =
            "SELECT COUNT(*) FROM listings l" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " LEFT JOIN subscription_plans sp ON sp.id = ss.plan_id" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND (:categoryId      IS NULL OR l.category_id  = :categoryId)" +
            " AND (:moduleId        IS NULL OR l.module_id    = :moduleId)" +
            " AND (:listingType     IS NULL OR l.listing_type = :listingType)" +
            " AND (:minPrice        IS NULL OR l.offered_price >= :minPrice)" +
            " AND (:maxPrice        IS NULL OR l.offered_price <= :maxPrice)" +
            " AND (:district        IS NULL OR l.address->>'district' ILIKE :district)" +
            " AND (:state           IS NULL OR l.address->>'state'    ILIKE :state)" +
            " AND (:userId          IS NULL OR l.user_id = :userId)" +
            " AND (:excludeUserId   IS NULL OR l.user_id != :excludeUserId)" +
            " AND (:attrFilter      IS NULL OR l.attributes @> CAST(:attrFilter AS jsonb))" +
            " AND (CAST(:postedAfter AS timestamp) IS NULL OR l.created_at >= CAST(:postedAfter AS timestamp))" +
            " AND (CAST(:latMin AS float8) IS NULL OR (" +
            "       l.latitude  BETWEEN CAST(:latMin AS float8) AND CAST(:latMax AS float8)" +
            "   AND l.longitude BETWEEN CAST(:lngMin AS float8) AND CAST(:lngMax AS float8)))" +
            " AND (CAST(:sellerType AS text) IS NULL OR CAST(:sellerType AS text) = 'ALL'" +
            "   OR (CAST(:sellerType AS text) = 'SUBSCRIBED' AND ss.id IS NOT NULL))" +
            " AND (CAST(:withPhoto AS boolean) = false OR (l.images IS NOT NULL AND jsonb_array_length(l.images) > 0))" +
            " AND (CAST(:verifiedSeller AS boolean) = false OR EXISTS (" +
            "       SELECT 1 FROM business_profiles bp WHERE bp.user_id = l.user_id" +
            "       AND bp.verification_status = 'APPROVED' AND bp.is_active = true AND bp.is_deleted = false))",
            nativeQuery = true)
    Page<Listing> findWithFilters(
            @Param("categoryId")     Integer categoryId,
            @Param("moduleId")       Integer moduleId,
            @Param("listingType")    String listingType,
            @Param("minPrice")       BigDecimal minPrice,
            @Param("maxPrice")       BigDecimal maxPrice,
            @Param("district")       String district,
            @Param("state")          String state,
            @Param("userId")         String userId,
            @Param("excludeUserId")  String excludeUserId,
            @Param("attrFilter")     String attrFilter,
            @Param("sortBy")         String sortBy,
            @Param("sortDir")        String sortDir,
            @Param("postedAfter")    LocalDateTime postedAfter,
            @Param("latMin")         Double latMin,
            @Param("latMax")         Double latMax,
            @Param("lngMin")         Double lngMin,
            @Param("lngMax")         Double lngMax,
            @Param("sellerType")     String sellerType,
            @Param("withPhoto")      boolean withPhoto,
            @Param("verifiedSeller") boolean verifiedSeller,
            Pageable pageable);

    // ─── Category Browse — nearby sort (distance ASC, premium first per tier) ──

    @Query(value =
            "SELECT l.* FROM listings l" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " LEFT JOIN subscription_plans sp ON sp.id = ss.plan_id" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND l.latitude  IS NOT NULL AND l.longitude IS NOT NULL" +
            " AND (:categoryId      IS NULL OR l.category_id  = :categoryId)" +
            " AND (:moduleId        IS NULL OR l.module_id    = :moduleId)" +
            " AND (:listingType     IS NULL OR l.listing_type = :listingType)" +
            " AND (:minPrice        IS NULL OR l.offered_price >= :minPrice)" +
            " AND (:maxPrice        IS NULL OR l.offered_price <= :maxPrice)" +
            " AND (:district        IS NULL OR l.address->>'district' ILIKE :district)" +
            " AND (:state           IS NULL OR l.address->>'state'    ILIKE :state)" +
            " AND (:userId          IS NULL OR l.user_id = :userId)" +
            " AND (:excludeUserId   IS NULL OR l.user_id != :excludeUserId)" +
            " AND (:attrFilter      IS NULL OR l.attributes @> CAST(:attrFilter AS jsonb))" +
            " AND (CAST(:postedAfter AS timestamp) IS NULL OR l.created_at >= CAST(:postedAfter AS timestamp))" +
            " AND (CAST(:latMin AS float8) IS NULL OR (" +
            "       l.latitude  BETWEEN CAST(:latMin AS float8) AND CAST(:latMax AS float8)" +
            "   AND l.longitude BETWEEN CAST(:lngMin AS float8) AND CAST(:lngMax AS float8)))" +
            " AND (CAST(:sellerType AS text) IS NULL OR CAST(:sellerType AS text) = 'ALL'" +
            "   OR (CAST(:sellerType AS text) = 'SUBSCRIBED' AND ss.id IS NOT NULL))" +
            " AND (CAST(:withPhoto AS boolean) = false OR (l.images IS NOT NULL AND jsonb_array_length(l.images) > 0))" +
            " AND (CAST(:verifiedSeller AS boolean) = false OR EXISTS (" +
            "       SELECT 1 FROM business_profiles bp WHERE bp.user_id = l.user_id" +
            "       AND bp.verification_status = 'APPROVED' AND bp.is_active = true AND bp.is_deleted = false))" +
            " ORDER BY" +
            " CASE WHEN :sortBy = 'offeredPrice' AND :sortDir = 'asc'  THEN l.offered_price END ASC  NULLS LAST," +
            " CASE WHEN :sortBy = 'offeredPrice' AND :sortDir = 'desc' THEN l.offered_price END DESC NULLS LAST," +
            " CASE WHEN :sortBy = 'createdAt'    AND :sortDir = 'asc'  THEN l.created_at    END ASC  NULLS LAST," +
            " CASE WHEN :sortBy = 'createdAt'    AND :sortDir = 'desc' THEN l.created_at    END DESC NULLS LAST," +
            " COALESCE(sp.sort_weight, 99) ASC," +
            " (6371 * acos(LEAST(1.0, cos(radians(CAST(:userLat AS float8))) * cos(radians(l.latitude))" +
            "   * cos(radians(l.longitude) - radians(CAST(:userLng AS float8)))" +
            "   + sin(radians(CAST(:userLat AS float8))) * sin(radians(l.latitude))))) ASC",
            countQuery =
            "SELECT COUNT(*) FROM listings l" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " LEFT JOIN subscription_plans sp ON sp.id = ss.plan_id" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND l.latitude  IS NOT NULL AND l.longitude IS NOT NULL" +
            " AND (:categoryId      IS NULL OR l.category_id  = :categoryId)" +
            " AND (:moduleId        IS NULL OR l.module_id    = :moduleId)" +
            " AND (:listingType     IS NULL OR l.listing_type = :listingType)" +
            " AND (:minPrice        IS NULL OR l.offered_price >= :minPrice)" +
            " AND (:maxPrice        IS NULL OR l.offered_price <= :maxPrice)" +
            " AND (:district        IS NULL OR l.address->>'district' ILIKE :district)" +
            " AND (:state           IS NULL OR l.address->>'state'    ILIKE :state)" +
            " AND (:userId          IS NULL OR l.user_id = :userId)" +
            " AND (:excludeUserId   IS NULL OR l.user_id != :excludeUserId)" +
            " AND (:attrFilter      IS NULL OR l.attributes @> CAST(:attrFilter AS jsonb))" +
            " AND (CAST(:postedAfter AS timestamp) IS NULL OR l.created_at >= CAST(:postedAfter AS timestamp))" +
            " AND (CAST(:latMin AS float8) IS NULL OR (" +
            "       l.latitude  BETWEEN CAST(:latMin AS float8) AND CAST(:latMax AS float8)" +
            "   AND l.longitude BETWEEN CAST(:lngMin AS float8) AND CAST(:lngMax AS float8)))" +
            " AND (CAST(:sellerType AS text) IS NULL OR CAST(:sellerType AS text) = 'ALL'" +
            "   OR (CAST(:sellerType AS text) = 'SUBSCRIBED' AND ss.id IS NOT NULL))" +
            " AND (CAST(:withPhoto AS boolean) = false OR (l.images IS NOT NULL AND jsonb_array_length(l.images) > 0))" +
            " AND (CAST(:verifiedSeller AS boolean) = false OR EXISTS (" +
            "       SELECT 1 FROM business_profiles bp WHERE bp.user_id = l.user_id" +
            "       AND bp.verification_status = 'APPROVED' AND bp.is_active = true AND bp.is_deleted = false))",
            nativeQuery = true)
    Page<Listing> findWithFiltersNearby(
            @Param("categoryId")     Integer categoryId,
            @Param("moduleId")       Integer moduleId,
            @Param("listingType")    String listingType,
            @Param("minPrice")       BigDecimal minPrice,
            @Param("maxPrice")       BigDecimal maxPrice,
            @Param("district")       String district,
            @Param("state")          String state,
            @Param("userId")         String userId,
            @Param("excludeUserId")  String excludeUserId,
            @Param("attrFilter")     String attrFilter,
            @Param("sortBy")         String sortBy,
            @Param("sortDir")        String sortDir,
            @Param("postedAfter")    LocalDateTime postedAfter,
            @Param("latMin")         Double latMin,
            @Param("latMax")         Double latMax,
            @Param("lngMin")         Double lngMin,
            @Param("lngMax")         Double lngMax,
            @Param("sellerType")     String sellerType,
            @Param("withPhoto")      boolean withPhoto,
            @Param("verifiedSeller") boolean verifiedSeller,
            @Param("userLat")        Double userLat,
            @Param("userLng")        Double userLng,
            Pageable pageable);

    // ─── Full-Text Search (PostgreSQL tsvector) ───────────────────────────────

    @Query(value =
            "SELECT l.* FROM listings l" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " LEFT JOIN subscription_plans sp ON sp.id = ss.plan_id" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND (:q IS NULL OR l.search_vector @@ plainto_tsquery('simple', :q))" +
            " AND (:categoryId      IS NULL OR l.category_id  = :categoryId)" +
            " AND (:moduleId        IS NULL OR l.module_id    = :moduleId)" +
            " AND (:listingType     IS NULL OR l.listing_type = :listingType)" +
            " AND (:minPrice        IS NULL OR l.offered_price >= :minPrice)" +
            " AND (:maxPrice        IS NULL OR l.offered_price <= :maxPrice)" +
            " AND (:district        IS NULL OR l.address->>'district' ILIKE :district)" +
            " AND (:excludeUserId   IS NULL OR l.user_id != :excludeUserId)" +
            " AND (:attrFilter      IS NULL OR l.attributes @> CAST(:attrFilter AS jsonb))" +
            " AND (CAST(:postedAfter AS timestamp) IS NULL OR l.created_at >= CAST(:postedAfter AS timestamp))" +
            " AND (CAST(:latMin AS float8) IS NULL OR (" +
            "       l.latitude  BETWEEN CAST(:latMin AS float8) AND CAST(:latMax AS float8)" +
            "   AND l.longitude BETWEEN CAST(:lngMin AS float8) AND CAST(:lngMax AS float8)))" +
            " AND (CAST(:sellerType AS text) IS NULL OR CAST(:sellerType AS text) = 'ALL'" +
            "   OR (CAST(:sellerType AS text) = 'SUBSCRIBED' AND ss.id IS NOT NULL))" +
            " AND (CAST(:withPhoto AS boolean) = false OR (l.images IS NOT NULL AND jsonb_array_length(l.images) > 0))" +
            " AND (CAST(:verifiedSeller AS boolean) = false OR EXISTS (" +
            "       SELECT 1 FROM business_profiles bp WHERE bp.user_id = l.user_id" +
            "       AND bp.verification_status = 'APPROVED' AND bp.is_active = true AND bp.is_deleted = false))" +
            " ORDER BY" +
            " CASE WHEN :sortBy = 'offeredPrice' AND :sortDir = 'asc'  THEN l.offered_price END ASC  NULLS LAST," +
            " CASE WHEN :sortBy = 'offeredPrice' AND :sortDir = 'desc' THEN l.offered_price END DESC NULLS LAST," +
            " CASE WHEN :sortBy = 'createdAt'    AND :sortDir = 'asc'  THEN l.created_at    END ASC  NULLS LAST," +
            " CASE WHEN :sortBy = 'createdAt'    AND :sortDir = 'desc' THEN l.created_at    END DESC NULLS LAST," +
            " COALESCE(sp.sort_weight, 99) ASC," +
            " CASE WHEN :q IS NOT NULL THEN ts_rank(l.search_vector, plainto_tsquery('simple', :q)) ELSE 0 END DESC," +
            " l.created_at DESC",
            countQuery =
            "SELECT COUNT(*) FROM listings l" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND (:q IS NULL OR l.search_vector @@ plainto_tsquery('simple', :q))" +
            " AND (:categoryId      IS NULL OR l.category_id  = :categoryId)" +
            " AND (:moduleId        IS NULL OR l.module_id    = :moduleId)" +
            " AND (:listingType     IS NULL OR l.listing_type = :listingType)" +
            " AND (:minPrice        IS NULL OR l.offered_price >= :minPrice)" +
            " AND (:maxPrice        IS NULL OR l.offered_price <= :maxPrice)" +
            " AND (:district        IS NULL OR l.address->>'district' ILIKE :district)" +
            " AND (:excludeUserId   IS NULL OR l.user_id != :excludeUserId)" +
            " AND (:attrFilter      IS NULL OR l.attributes @> CAST(:attrFilter AS jsonb))" +
            " AND (CAST(:postedAfter AS timestamp) IS NULL OR l.created_at >= CAST(:postedAfter AS timestamp))" +
            " AND (CAST(:latMin AS float8) IS NULL OR (" +
            "       l.latitude  BETWEEN CAST(:latMin AS float8) AND CAST(:latMax AS float8)" +
            "   AND l.longitude BETWEEN CAST(:lngMin AS float8) AND CAST(:lngMax AS float8)))" +
            " AND (CAST(:sellerType AS text) IS NULL OR CAST(:sellerType AS text) = 'ALL'" +
            "   OR (CAST(:sellerType AS text) = 'SUBSCRIBED' AND ss.id IS NOT NULL))" +
            " AND (CAST(:withPhoto AS boolean) = false OR (l.images IS NOT NULL AND jsonb_array_length(l.images) > 0))" +
            " AND (CAST(:verifiedSeller AS boolean) = false OR EXISTS (" +
            "       SELECT 1 FROM business_profiles bp WHERE bp.user_id = l.user_id" +
            "       AND bp.verification_status = 'APPROVED' AND bp.is_active = true AND bp.is_deleted = false))",
            nativeQuery = true)
    Page<Listing> searchListings(
            @Param("q")              String q,
            @Param("categoryId")     Integer categoryId,
            @Param("moduleId")       Integer moduleId,
            @Param("listingType")    String listingType,
            @Param("minPrice")       BigDecimal minPrice,
            @Param("maxPrice")       BigDecimal maxPrice,
            @Param("district")       String district,
            @Param("excludeUserId")  String excludeUserId,
            @Param("attrFilter")     String attrFilter,
            @Param("sortBy")         String sortBy,
            @Param("sortDir")        String sortDir,
            @Param("postedAfter")    LocalDateTime postedAfter,
            @Param("latMin")         Double latMin,
            @Param("latMax")         Double latMax,
            @Param("lngMin")         Double lngMin,
            @Param("lngMax")         Double lngMax,
            @Param("sellerType")     String sellerType,
            @Param("withPhoto")      boolean withPhoto,
            @Param("verifiedSeller") boolean verifiedSeller,
            Pageable pageable);

    // ─── Home Feed ────────────────────────────────────────────────────────────

    @Query(value =
            "SELECT l.* FROM listings l" +
            " INNER JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " INNER JOIN subscription_plans sp ON sp.id = ss.plan_id AND sp.home_feed_slots > 0" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND (:district IS NULL OR l.address->>'district' ILIKE :district)" +
            " ORDER BY sp.sort_weight ASC, l.created_at DESC" +
            " LIMIT :limit",
            nativeQuery = true)
    List<Listing> findFeaturedForHomeFeed(@Param("district") String district,
                                           @Param("limit") int limit);

    @Query(value =
            "SELECT l.* FROM listings l" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " LEFT JOIN subscription_plans sp ON sp.id = ss.plan_id" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND l.module_id = :moduleId" +
            " AND (:district IS NULL OR l.address->>'district' ILIKE :district)" +
            " ORDER BY COALESCE(sp.sort_weight, 99) ASC, l.created_at DESC" +
            " LIMIT :limit",
            nativeQuery = true)
    List<Listing> findForModuleSection(@Param("moduleId") int moduleId,
                                        @Param("district") String district,
                                        @Param("limit") int limit);

    @Query(value =
            "SELECT l.* FROM listings l" +
            " INNER JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " INNER JOIN subscription_plans sp ON sp.id = ss.plan_id AND sp.home_feed_slots > 0" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND l.module_id = :moduleId" +
            " AND (:district IS NULL OR l.address->>'district' ILIKE :district)" +
            " ORDER BY sp.sort_weight ASC, l.created_at DESC" +
            " LIMIT :limit",
            nativeQuery = true)
    List<Listing> findFeaturedForModule(@Param("moduleId") int moduleId,
                                        @Param("district") String district,
                                        @Param("limit") int limit);

    @Query(value =
            "SELECT l.* FROM listings l" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " LEFT JOIN subscription_plans sp ON sp.id = ss.plan_id" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND l.category_id = :categoryId" +
            " AND (:district IS NULL OR l.address->>'district' ILIKE :district)" +
            " ORDER BY COALESCE(sp.sort_weight, 99) ASC, l.created_at DESC" +
            " LIMIT :limit",
            nativeQuery = true)
    List<Listing> findForCategorySection(@Param("categoryId") int categoryId,
                                          @Param("district") String district,
                                          @Param("limit") int limit);

    @Query(value =
            "SELECT l.* FROM listings l" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " LEFT JOIN subscription_plans sp ON sp.id = ss.plan_id" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND l.category_id = :categoryId" +
            " AND l.listing_id  != :excludeListingId" +
            " AND (:excludeUserId IS NULL OR l.user_id != :excludeUserId)" +
            " ORDER BY COALESCE(sp.sort_weight, 99) ASC, l.created_at DESC" +
            " LIMIT :limit",
            nativeQuery = true)
    List<Listing> findSimilar(@Param("categoryId")      Integer categoryId,
                               @Param("excludeListingId") String excludeListingId,
                               @Param("excludeUserId")    String excludeUserId,
                               @Param("limit")            int limit);

    // ─── Update search vector ─────────────────────────────────────────────────

    @Modifying
    @Query(value =
            "UPDATE listings SET search_vector =" +
            " setweight(to_tsvector('simple', coalesce(:categoryText, '')), 'A') ||" +
            " setweight(to_tsvector('simple', coalesce(:locationText,  '')), 'B') ||" +
            " setweight(to_tsvector('simple', coalesce(:attrText,      '')), 'C')" +
            " WHERE listing_id = :listingId",
            nativeQuery = true)
    void updateSearchVector(@Param("listingId")    String listingId,
                             @Param("categoryText") String categoryText,
                             @Param("locationText") String locationText,
                             @Param("attrText")     String attrText);

    // ─── Flash Deals ─────────────────────────────────────────────────────────

    @Query(value =
            "SELECT l.* FROM listings l" +
            " LEFT JOIN user_subscriptions ss" +
            " ON ss.user_id = l.user_id AND ss.status = 'ACTIVE' AND ss.expires_at > NOW()" +
            " LEFT JOIN subscription_plans sp ON sp.id = ss.plan_id" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND l.flash_deal = true AND l.discount_pct IS NOT NULL" +
            " AND (l.discount_expires_at IS NULL OR l.discount_expires_at > NOW())" +
            " AND (:district IS NULL OR l.address->>'district' ILIKE :district)" +
            " AND (:excludeUserId IS NULL OR l.user_id != :excludeUserId)" +
            " ORDER BY COALESCE(sp.sort_weight, 99) ASC, l.discount_pct DESC" +
            " LIMIT :limit",
            nativeQuery = true)
    List<Listing> findFlashDeals(@Param("district")      String district,
                                  @Param("excludeUserId") String excludeUserId,
                                  @Param("limit")         int limit);

    // ─── Fresh Deals — listings from last 15 days, recent first then price low ──

    @Query(value =
            "SELECT l.* FROM listings l" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND l.created_at >= CAST(:cutoffDate AS timestamp)" +
            " AND l.category_id IN (:categoryIds)" +
            " AND (:listingType IS NULL OR l.listing_type = :listingType)" +
            " AND (CAST(:latMin AS float8) IS NULL OR (" +
            "       l.latitude  BETWEEN CAST(:latMin AS float8) AND CAST(:latMax AS float8)" +
            "   AND l.longitude BETWEEN CAST(:lngMin AS float8) AND CAST(:lngMax AS float8)))" +
            " ORDER BY l.created_at DESC, l.offered_price ASC",
            countQuery =
            "SELECT COUNT(*) FROM listings l" +
            " WHERE l.is_deleted = false AND l.status = 'ACTIVE' AND l.is_active = true" +
            " AND l.created_at >= CAST(:cutoffDate AS timestamp)" +
            " AND l.category_id IN (:categoryIds)" +
            " AND (:listingType IS NULL OR l.listing_type = :listingType)" +
            " AND (CAST(:latMin AS float8) IS NULL OR (" +
            "       l.latitude  BETWEEN CAST(:latMin AS float8) AND CAST(:latMax AS float8)" +
            "   AND l.longitude BETWEEN CAST(:lngMin AS float8) AND CAST(:lngMax AS float8)))",
            nativeQuery = true)
    Page<Listing> findDealGroupListings(
            @Param("categoryIds") List<Integer> categoryIds,
            @Param("listingType") String listingType,
            @Param("cutoffDate")  String cutoffDate,
            @Param("latMin")      Double latMin,
            @Param("latMax")      Double latMax,
            @Param("lngMin")      Double lngMin,
            @Param("lngMax")      Double lngMax,
            Pageable pageable);

    @Query(value =
            "SELECT COUNT(*) FROM listings" +
            " WHERE is_deleted = false AND status = 'ACTIVE' AND is_active = true" +
            " AND created_at >= CAST(:cutoffDate AS timestamp)" +
            " AND category_id IN (:categoryIds)" +
            " AND (:listingType IS NULL OR listing_type = :listingType)",
            nativeQuery = true)
    long countDealGroupListings(@Param("categoryIds")  List<Integer> categoryIds,
                                @Param("listingType")  String listingType,
                                @Param("cutoffDate")   String cutoffDate);

    @Query(value =
            "SELECT MIN(offered_price) FROM listings" +
            " WHERE is_deleted = false AND status = 'ACTIVE' AND is_active = true" +
            " AND created_at >= CAST(:cutoffDate AS timestamp)" +
            " AND category_id IN (:categoryIds)" +
            " AND (:listingType IS NULL OR listing_type = :listingType)",
            nativeQuery = true)
    java.math.BigDecimal minDealGroupPrice(@Param("categoryIds") List<Integer> categoryIds,
                                           @Param("listingType") String listingType,
                                           @Param("cutoffDate")  String cutoffDate);

    // ─── Public Stats ─────────────────────────────────────────────────────────

    @Query(value = "SELECT COUNT(DISTINCT user_id) FROM listings WHERE is_deleted = false", nativeQuery = true)
    long countDistinctSellers();

    @Query(value = "SELECT COUNT(DISTINCT address->>'district') FROM listings WHERE is_deleted = false AND address->>'district' IS NOT NULL", nativeQuery = true)
    long countDistinctDistricts();

    @Query(value = "SELECT COALESCE(SUM(offered_price * quantity), 0) FROM listings WHERE is_deleted = false AND status = 'ACTIVE'", nativeQuery = true)
    java.math.BigDecimal sumTotalTradeValue();

    @Query(value = "SELECT COUNT(*) FROM listings WHERE is_deleted = false AND status = 'ACTIVE' AND is_active = true", nativeQuery = true)
    long countActiveListings();

    @Query(value = "SELECT COUNT(*) FROM listings WHERE is_deleted = false AND created_at >= CURRENT_DATE", nativeQuery = true)
    long countTodayNew();

    // ─── Legacy / simple queries ──────────────────────────────────────────────

    List<Listing> findByCategoryIdAndStatusAndIsDeletedFalse(Integer categoryId, ListingStatus status);

    @Query("select l from Listing l where l.categoryId = :categoryId and l.isDeleted = false " +
           "and l.status = :status and (:listingType is null or l.listingType = :listingType)")
    Page<Listing> findByCategory(@Param("categoryId") Integer categoryId,
                                  @Param("status") ListingStatus status,
                                  @Param("listingType") ListingType listingType,
                                  Pageable pageable);

    // ─── Price bounds for filter slider ──────────────────────────────────────

    @Query(value =
           "SELECT MIN(offered_price) FROM listings " +
           "WHERE is_deleted = false AND status = 'ACTIVE' " +
           "AND (:categoryId IS NULL OR category_id = :categoryId) " +
           "AND offered_price IS NOT NULL",
           nativeQuery = true)
    java.math.BigDecimal findMinPrice(@Param("categoryId") Integer categoryId);

    @Query(value =
           "SELECT MAX(offered_price) FROM listings " +
           "WHERE is_deleted = false AND status = 'ACTIVE' " +
           "AND (:categoryId IS NULL OR category_id = :categoryId) " +
           "AND offered_price IS NOT NULL",
           nativeQuery = true)
    java.math.BigDecimal findMaxPrice(@Param("categoryId") Integer categoryId);

    // ─── Wishlist / Favourite Count ───────────────────────────────────────────

    @Modifying
    @Query(value = "UPDATE listings SET favorite_count = favorite_count + 1 WHERE listing_id = :listingId",
           nativeQuery = true)
    void incrementFavoriteCount(@Param("listingId") String listingId);

    @Modifying
    @Query(value = "UPDATE listings SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE listing_id = :listingId",
           nativeQuery = true)
    void decrementFavoriteCount(@Param("listingId") String listingId);

    /** Atomically increments view_count by 1. Called only when a genuinely new viewer is confirmed. */
    @Modifying
    @Query(value = "UPDATE listings SET view_count = view_count + 1 WHERE listing_id = :listingId",
            nativeQuery = true)
    void incrementViewCount(@Param("listingId") String listingId);

    @Modifying
    @Query(value = "UPDATE listings SET contact_reveal_count = contact_reveal_count + 1 WHERE listing_id = :listingId",
           nativeQuery = true)
    void incrementContactRevealCount(@Param("listingId") String listingId);

    /**
     * Fetch multiple listings by their IDs for the wishlist list endpoint.
     * Excludes deleted listings; keeps ACTIVE, INACTIVE, and SOLD so the frontend
     * can show their status (e.g. dim "SOLD" cards). DRAFT is excluded — it should
     * never be in a wishlist, but filtered here as an extra safety net.
     */
    @Query("select l from Listing l where l.listingId in :ids" +
           " and l.isDeleted = false and l.status != com.hyperlocal.tantra.modules.listing.model.ListingStatus.DRAFT")
    List<Listing> findAllByListingIdIn(@Param("ids") List<String> ids);
}
