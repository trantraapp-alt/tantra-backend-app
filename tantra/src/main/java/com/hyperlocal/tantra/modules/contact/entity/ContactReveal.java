package com.hyperlocal.tantra.modules.contact.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Tracks every contact reveal event (OLX-style "Get Contact" tap).
 * One row per buyer+listing combination per session — prevents double-counting,
 * provides analytics for sellers, and enables future rate-limiting.
 */
@Entity
@Table(name = "contact_reveals", indexes = {
        @Index(name = "idx_cr_listing",      columnList = "listing_id"),
        @Index(name = "idx_cr_buyer",        columnList = "buyer_user_id"),
        @Index(name = "idx_cr_listing_buyer",columnList = "listing_id, buyer_user_id"),
        @Index(name = "idx_cr_seller",       columnList = "seller_user_id")
})
@Data
public class ContactReveal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false, length = 20)
    private String listingId;

    @Column(name = "seller_user_id", nullable = false, length = 20)
    private String sellerUserId;

    @Column(name = "buyer_user_id", nullable = false, length = 20)
    private String buyerUserId;

    /** Contact number that was revealed (masked in logs). */
    @Column(name = "contact_number", length = 15)
    private String contactNumber;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
