package com.hyperlocal.tantra.modules.wishlist.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One wishlist entry: a user saving a listing.
 * The unique constraint (user_id, listing_id) prevents duplicate saves.
 */
@Entity
@Table(
    name = "user_wishlist",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_wishlist",
        columnNames = {"user_id", "listing_id"}
    )
)
@Data
public class UserWishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "listing_id", nullable = false, length = 20)
    private String listingId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
