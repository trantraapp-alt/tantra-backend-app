package com.hyperlocal.tantra.modules.wishlist.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyperlocal.tantra.modules.listing.dto.ListingCardDTO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One item in the user's wishlist — wraps the full listing card plus when it was saved.
 * {@code listing} is null when the original listing has been deleted.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WishlistItemDTO {

    private String listingId;
    private LocalDateTime savedAt;

    /** Full listing card details. Null if the listing no longer exists (deleted/removed). */
    private ListingCardDTO listing;
}
