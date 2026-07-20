package com.hyperlocal.tantra.modules.listing.repository;

import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    Optional<Listing> findByListingId(String listingId);
    List<Listing> findByUserIdAndIsDeletedFalse(String userId);
    List<Listing> findByCategoryIdAndStatusAndIsDeletedFalse(Integer categoryId, ListingStatus status);
    boolean existsByListingId(String listingId);
}
