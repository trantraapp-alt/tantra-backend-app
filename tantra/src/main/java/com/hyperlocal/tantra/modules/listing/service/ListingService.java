package com.hyperlocal.tantra.modules.listing.service;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.listing.dto.ListingRequest;
import com.hyperlocal.tantra.modules.listing.dto.ListingResponse;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import com.hyperlocal.tantra.utils.IdGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Creates and reads listings. Splits the incoming payload into typed common columns and the
 * category-specific {@code attributes} jsonb, and injects the backend-owned fields (ids, audit,
 * counters) from the authenticated user rather than trusting the client.
 */
@Service
public class ListingService {

    @Autowired private ListingRepository listingRepository;
    @Autowired private UserRepository userRepository;

    @Transactional
    public ListingResponse createListing(ListingRequest req, String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_USER_NOT_FOUND_EN, MessageConstants.LISTING_USER_NOT_FOUND_HI));

        if (req.getCategoryId() == null) {
            throw new LocalizedException(
                    MessageConstants.LISTING_CATEGORY_REQUIRED_EN, MessageConstants.LISTING_CATEGORY_REQUIRED_HI);
        }

        Listing listing = new Listing();
        listing.setListingId(generateUniqueListingId());

        // Backend-owned identity/audit — never taken from the client.
        listing.setUserId(user.getUserId());
        listing.setUserMobileNumber(user.getMobileNumber());
        listing.setCreatedBy(user.getUserId());
        listing.setUpdatedBy(user.getUserId());

        listing.setCategoryId(req.getCategoryId());
        listing.setModuleId(req.getModuleId());
        if (req.getListingType() != null) listing.setListingType(req.getListingType());

        listing.setActualPrice(req.getActualPrice());
        listing.setOfferedPrice(req.getOfferedPrice());
        listing.setDiscountPct(computeDiscount(req.getActualPrice(), req.getOfferedPrice()));
        listing.setQuantity(req.getQuantity());
        listing.setUnit(req.getUnit());
        listing.setIsNegotiable(Boolean.TRUE.equals(req.getIsNegotiable()));

        if (req.getImages() != null) listing.setImages(req.getImages());
        if (req.getAttributes() != null) listing.setAttributes(req.getAttributes());

        // "Use default address" will pull from a saved address book once that exists; for now the
        // client sends the address either way, so we persist whatever was provided.
        listing.setAddress(req.getAddress());

        Listing saved = listingRepository.save(listing);
        return ListingResponse.ok(
                saved.getListingId(),
                saved.getUserId(),
                LocalizedText.of(MessageConstants.LISTING_CREATE_SUCCESS_EN, MessageConstants.LISTING_CREATE_SUCCESS_HI));
    }

    public List<Listing> getMyListings(String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_USER_NOT_FOUND_EN, MessageConstants.LISTING_USER_NOT_FOUND_HI));
        return listingRepository.findByUserIdAndIsDeletedFalse(user.getUserId());
    }

    public Listing getByListingId(String listingId) {
        return listingRepository.findByListingId(listingId)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_NOT_FOUND_EN, MessageConstants.LISTING_NOT_FOUND_HI));
    }

    private BigDecimal computeDiscount(BigDecimal actual, BigDecimal offered) {
        if (actual == null || offered == null || actual.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return actual.subtract(offered)
                .multiply(BigDecimal.valueOf(100))
                .divide(actual, 2, RoundingMode.HALF_UP);
    }

    private String generateUniqueListingId() {
        String id;
        do {
            id = IdGeneratorUtil.generateShortUniqueId();
        } while (listingRepository.existsByListingId(id));
        return id;
    }
}
