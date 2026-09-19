package com.hyperlocal.tantra.modules.wishlist.service;

import com.hyperlocal.tantra.common.error.ErrorCode;
import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.listing.dto.ListingCardDTO;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import com.hyperlocal.tantra.modules.listing.service.ListingService;
import com.hyperlocal.tantra.modules.wishlist.dto.WishlistItemDTO;
import com.hyperlocal.tantra.modules.wishlist.dto.WishlistStatusDTO;
import com.hyperlocal.tantra.modules.wishlist.entity.UserWishlist;
import com.hyperlocal.tantra.modules.wishlist.repository.UserWishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wishlist (Favourites) — add, remove, list, and status check.
 * Keeps listing.favorite_count in sync on every add/remove.
 */
@Service
public class WishlistService {

    @Autowired private UserWishlistRepository wishlistRepository;
    @Autowired private UserRepository         userRepository;
    @Autowired private ListingRepository      listingRepository;
    @Autowired private ListingService         listingService;

    // ─── Add ─────────────────────────────────────────────────────────────────

    /**
     * Save a listing to the user's wishlist.
     * Idempotent: returns a "already saved" message instead of throwing on duplicate.
     */
    @Transactional
    public LocalizedText add(String mobileNumber, String listingId) {
        User user = currentUser(mobileNumber);

        // Listing must exist (and not be deleted)
        if (!listingRepository.existsByListingId(listingId)) {
            throw new LocalizedException(
                    MessageConstants.LISTING_NOT_FOUND_EN,
                    MessageConstants.LISTING_NOT_FOUND_HI,
                    ErrorCode.NOT_FOUND, 404);
        }

        // Duplicate check — return friendly message instead of a DB error
        if (wishlistRepository.existsByUserIdAndListingId(user.getUserId(), listingId)) {
            return LocalizedText.of(
                    MessageConstants.WISHLIST_ALREADY_EN,
                    MessageConstants.WISHLIST_ALREADY_HI);
        }

        UserWishlist entry = new UserWishlist();
        entry.setUserId(user.getUserId());
        entry.setListingId(listingId);
        wishlistRepository.save(entry);

        // Keep favorite_count in sync
        listingRepository.incrementFavoriteCount(listingId);

        return LocalizedText.of(
                MessageConstants.WISHLIST_ADDED_EN,
                MessageConstants.WISHLIST_ADDED_HI);
    }

    // ─── Remove ──────────────────────────────────────────────────────────────

    /** Remove a listing from the user's wishlist. Throws 404 if not currently saved. */
    @Transactional
    public LocalizedText remove(String mobileNumber, String listingId) {
        User user = currentUser(mobileNumber);

        if (!wishlistRepository.existsByUserIdAndListingId(user.getUserId(), listingId)) {
            throw new LocalizedException(
                    MessageConstants.WISHLIST_NOT_FOUND_EN,
                    MessageConstants.WISHLIST_NOT_FOUND_HI,
                    ErrorCode.NOT_FOUND, 404);
        }

        wishlistRepository.deleteByUserIdAndListingId(user.getUserId(), listingId);
        listingRepository.decrementFavoriteCount(listingId);

        return LocalizedText.of(
                MessageConstants.WISHLIST_REMOVED_EN,
                MessageConstants.WISHLIST_REMOVED_HI);
    }

    // ─── List ─────────────────────────────────────────────────────────────────

    /**
     * Return all wishlist entries for the user, newest first.
     * Each item contains the full listing card. If the listing was deleted after it was
     * wishlisted, {@code listing} is null — the frontend can show a "no longer available" state.
     */
    public List<WishlistItemDTO> getWishlist(String mobileNumber) {
        User user = currentUser(mobileNumber);

        List<UserWishlist> entries = wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId());
        if (entries.isEmpty()) {
            return new ArrayList<>();
        }

        // Batch-fetch all listings in one query
        List<String> listingIds = entries.stream()
                .map(UserWishlist::getListingId)
                .collect(Collectors.toList());

        Map<String, Listing> listingMap = listingRepository.findAllByListingIdIn(listingIds)
                .stream()
                .collect(Collectors.toMap(Listing::getListingId, Function.identity()));

        // Build DTOs preserving the wishlist order
        List<WishlistItemDTO> result = new ArrayList<>();
        for (UserWishlist entry : entries) {
            WishlistItemDTO dto = new WishlistItemDTO();
            dto.setListingId(entry.getListingId());
            dto.setSavedAt(entry.getCreatedAt());

            Listing listing = listingMap.get(entry.getListingId());
            if (listing != null) {
                ListingCardDTO card = listingService.toCard(listing, null, null);
                dto.setListing(card);
            }
            // listing == null → deleted; dto.listing stays null (shown as unavailable in UI)
            result.add(dto);
        }
        return result;
    }

    // ─── Status ───────────────────────────────────────────────────────────────

    /** Check whether the requesting user has a specific listing in their wishlist. */
    public WishlistStatusDTO status(String mobileNumber, String listingId) {
        User user = currentUser(mobileNumber);
        boolean wishlisted = wishlistRepository.existsByUserIdAndListingId(user.getUserId(), listingId);
        return new WishlistStatusDTO(listingId, wishlisted);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User currentUser(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_USER_NOT_FOUND_EN,
                        MessageConstants.LISTING_USER_NOT_FOUND_HI));
    }
}
