package com.hyperlocal.tantra.modules.contact.repository;

import com.hyperlocal.tantra.modules.contact.entity.ContactReveal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ContactRevealRepository extends JpaRepository<ContactReveal, Long> {

    /** Check if this buyer has already revealed this listing's contact today (dedup). */
    @Query("select c from ContactReveal c where c.listingId = :listingId " +
           "and c.buyerUserId = :buyerUserId and c.createdAt >= :since")
    Optional<ContactReveal> findRecentReveal(@Param("listingId") String listingId,
                                              @Param("buyerUserId") String buyerUserId,
                                              @Param("since") LocalDateTime since);

    long countByListingId(String listingId);

    long countBySellerUserId(String sellerUserId);
}
