package com.hyperlocal.tantra.modules.listing.repository;

import com.hyperlocal.tantra.modules.listing.entity.ListingView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingViewRepository extends JpaRepository<ListingView, Long> {

    /**
     * Attempts to insert a new view record.
     * The UNIQUE constraint on (listing_id, viewer_user_id) handles dedup at the DB level.
     *
     * Returns:
     *   1 → new row inserted (first time this user viewed this listing) → safe to increment view_count
     *   0 → conflict, row already exists (user has seen it before) → do nothing
     *
     * No SELECT needed — the DB guarantees atomicity under concurrent inserts.
     */
    @Modifying
    @Query(value =
            "INSERT INTO listing_views (listing_id, viewer_user_id, viewed_at) " +
            "VALUES (:listingId, :viewerUserId, NOW()) " +
            "ON CONFLICT (listing_id, viewer_user_id) DO NOTHING",
            nativeQuery = true)
    int insertIfNotExists(
            @Param("listingId")     String listingId,
            @Param("viewerUserId")  String viewerUserId);
}
