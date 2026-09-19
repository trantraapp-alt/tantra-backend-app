package com.hyperlocal.tantra.modules.auth.service;

import com.hyperlocal.tantra.modules.auth.dto.SellerInfoDTO;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.business.model.VerificationStatus;
import com.hyperlocal.tantra.modules.business.repository.BusinessProfileRepository;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Assembles the public "Seller Information" card shown on listing detail screens.
 * Seller = a regular user — no separate entity, everything comes from users + listings tables.
 */
@Service
public class SellerInfoService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final BusinessProfileRepository businessProfileRepository;

    public SellerInfoService(UserRepository userRepository,
                             ListingRepository listingRepository,
                             BusinessProfileRepository businessProfileRepository) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.businessProfileRepository = businessProfileRepository;
    }

    /**
     * Returns public seller info for a given userId.
     * Called from listing detail screen — userId comes from ListingCardDTO.userId.
     */
    public SellerInfoDTO getSellerInfo(String userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found"));

        SellerInfoDTO dto = new SellerInfoDTO();
        dto.setUserId(user.getUserId());
        dto.setName(user.getFirstName() + " " + user.getLastName());
        dto.setMemberSince(user.getCreatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());

        // Verified = has at least one APPROVED business profile
        boolean verified = !businessProfileRepository
                .findByUserIdAndVerificationStatusAndIsDeletedFalse(userId, VerificationStatus.APPROVED)
                .isEmpty();
        dto.setVerifiedSeller(verified);

        // Total active listings count
        dto.setTotalActiveListings(listingRepository.countByUserIdAndIsActiveTrueAndIsDeletedFalse(userId));

        // Location from most recent active listing's address snapshot
        List<Listing> listings = listingRepository.findByUserIdAndIsDeletedFalse(userId);
        listings.stream()
                .filter(l -> l.getAddress() != null)
                .findFirst()
                .ifPresent(l -> {
                    String city    = l.getAddress().getCity();
                    String state   = l.getAddress().getState();
                    String loc = (city != null ? city : "") + (state != null ? ", " + state : "");
                    dto.setLocation(loc.isBlank() ? null : loc.trim());
                });

        return dto;
    }
}
