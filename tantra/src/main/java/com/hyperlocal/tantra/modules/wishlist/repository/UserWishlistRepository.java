package com.hyperlocal.tantra.modules.wishlist.repository;

import com.hyperlocal.tantra.modules.wishlist.entity.UserWishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserWishlistRepository extends JpaRepository<UserWishlist, Long> {

    /** All wishlist entries for a user, newest first. */
    List<UserWishlist> findByUserIdOrderByCreatedAtDesc(String userId);

    /** True if the user has already saved this listing. */
    boolean existsByUserIdAndListingId(String userId, String listingId);

    /** Remove one entry (used by remove flow). */
    @Modifying
    @Query("delete from UserWishlist w where w.userId = :userId and w.listingId = :listingId")
    void deleteByUserIdAndListingId(@Param("userId") String userId,
                                    @Param("listingId") String listingId);

    /** How many users have saved this listing — used for count display. */
    long countByListingId(String listingId);
}
