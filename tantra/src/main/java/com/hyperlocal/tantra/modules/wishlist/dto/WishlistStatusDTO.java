package com.hyperlocal.tantra.modules.wishlist.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Simple boolean status: whether the requesting user has this listing in their wishlist.
 */
@Data
@AllArgsConstructor
public class WishlistStatusDTO {
    private String listingId;
    private boolean wishlisted;
}
