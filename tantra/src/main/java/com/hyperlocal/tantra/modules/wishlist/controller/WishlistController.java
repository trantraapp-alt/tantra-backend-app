package com.hyperlocal.tantra.modules.wishlist.controller;

import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.wishlist.dto.WishlistItemDTO;
import com.hyperlocal.tantra.modules.wishlist.dto.WishlistStatusDTO;
import com.hyperlocal.tantra.modules.wishlist.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Wishlist (Favourites) — authenticated; user resolved from JWT.
 * Secured via /api/v1/wishlist/** in SecurityConfig.
 *
 *  POST   /api/v1/wishlist/{listingId}         → add listing to wishlist
 *  DELETE /api/v1/wishlist/{listingId}         → remove listing from wishlist
 *  GET    /api/v1/wishlist                     → fetch all wishlist items (full cards)
 *  GET    /api/v1/wishlist/{listingId}/status  → check if listing is wishlisted
 */
@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    @Autowired private WishlistService service;

    /** Add a listing to the authenticated user's wishlist. */
    @PostMapping("/{listingId}")
    public ResponseEntity<LocalizedText> add(
            @PathVariable String listingId,
            Authentication auth) {
        return ResponseEntity.ok(service.add(auth.getName(), listingId));
    }

    /** Remove a listing from the authenticated user's wishlist. */
    @DeleteMapping("/{listingId}")
    public ResponseEntity<LocalizedText> remove(
            @PathVariable String listingId,
            Authentication auth) {
        return ResponseEntity.ok(service.remove(auth.getName(), listingId));
    }

    /** Get all wishlist items for the authenticated user, newest first. */
    @GetMapping
    public ResponseEntity<List<WishlistItemDTO>> list(Authentication auth) {
        return ResponseEntity.ok(service.getWishlist(auth.getName()));
    }

    /** Check if the authenticated user has a specific listing in their wishlist. */
    @GetMapping("/{listingId}/status")
    public ResponseEntity<WishlistStatusDTO> status(
            @PathVariable String listingId,
            Authentication auth) {
        return ResponseEntity.ok(service.status(auth.getName(), listingId));
    }
}
