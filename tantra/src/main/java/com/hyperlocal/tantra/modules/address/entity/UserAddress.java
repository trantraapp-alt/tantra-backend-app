package com.hyperlocal.tantra.modules.address.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A saved address in the user's address book (Profile ▸ Address). Up to 10 per user, one default.
 * Kept as flat, queryable columns (not jsonb) since addresses are filtered/displayed directly.
 * A listing takes an independent snapshot of the chosen address, so editing/deleting here never
 * changes past listings.
 */
@Entity
@Table(name = "user_addresses", indexes = {
        @Index(name = "idx_user_addr_user", columnList = "user_id")
})
@Data
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public, human-facing id (e.g. "TN7ABC12"). */
    @Column(name = "address_id", unique = true, nullable = false, length = 20)
    private String addressId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    /** Optional nickname: Home / Farm / Shop. */
    @Column(name = "label", length = 40)
    private String label;

    @Column(name = "full_address", length = 500)
    private String fullAddress;

    @Column(name = "country", length = 80)
    private String country;

    @Column(name = "state", length = 80)
    private String state;

    @Column(name = "district", length = 80)
    private String district;

    @Column(name = "city", length = 80)
    private String city;

    @Column(name = "village", length = 120)
    private String village;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "mobile_number", length = 15)
    private String mobileNumber;

    @Column(name = "alt_mobile_number", length = 15)
    private String altMobileNumber;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
