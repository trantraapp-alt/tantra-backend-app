package com.hyperlocal.tantra.modules.listing.repository;

import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    Optional<Listing> findByListingId(String listingId);
    List<Listing> findByUserIdAndIsDeletedFalse(String userId);
    List<Listing> findByCategoryIdAndStatusAndIsDeletedFalse(Integer categoryId, ListingStatus status);
    boolean existsByListingId(String listingId);

    /** My-Listings — paginated, with optional listingType/status filters. */
    @Query("select l from Listing l where l.userId = :userId and l.isDeleted = false " +
            "and (:listingType is null or l.listingType = :listingType) " +
            "and (:status is null or l.status = :status)")
    Page<Listing> findMine(@Param("userId") String userId,
                           @Param("listingType") ListingType listingType,
                           @Param("status") ListingStatus status,
                           Pageable pageable);

    /** Category browse — paginated, active listings, optional listingType filter. */
    @Query("select l from Listing l where l.categoryId = :categoryId and l.isDeleted = false " +
            "and l.status = :status and (:listingType is null or l.listingType = :listingType)")
    Page<Listing> findByCategory(@Param("categoryId") Integer categoryId,
                                 @Param("status") ListingStatus status,
                                 @Param("listingType") ListingType listingType,
                                 Pageable pageable);
}
