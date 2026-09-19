package com.hyperlocal.tantra.modules.listing.entity;

import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.listing.model.Address;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Indexes added: geo (lat/lng), category+geo, module+geo, full-text search vector

/**
 * A product a user has listed to sell/rent. Common, queryable data lives in typed columns while
 * category-specific answers live in the {@code attributes} jsonb — so one table serves every
 * category of every module.
 */
@Entity
@Table(name = "listings", indexes = {
        @Index(name = "idx_listings_geo",          columnList = "latitude, longitude"),
        @Index(name = "idx_listings_category_geo", columnList = "category_id, latitude, longitude"),
        @Index(name = "idx_listings_module_geo",   columnList = "module_id, latitude, longitude"),
        @Index(name = "idx_listings_active",       columnList = "is_active, is_deleted, created_at"),
        @Index(name = "idx_listings_price",        columnList = "offered_price"),
        @Index(name = "idx_listings_user",         columnList = "user_id")
})
@Data
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public, opaque, 10-char type-prefixed id — LT + 8 (e.g. "LT9F3KD2P1"). */
    @Column(name = "listing_id", unique = true, nullable = false, length = 20)
    private String listingId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    @Column(name = "module_id")
    private Integer moduleId;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 10)
    private ListingType listingType = ListingType.SELL;

    /** The form definition + version this listing was created against (for deterministic edits). */
    @Column(name = "form_id")
    private Integer formId;

    @Column(name = "form_version")
    private Integer formVersion;

    @Column(name = "actual_price", precision = 12, scale = 2)
    private BigDecimal actualPrice;

    @Column(name = "offered_price", precision = 12, scale = 2)
    private BigDecimal offeredPrice;

    @Column(name = "discount_pct", precision = 5, scale = 2)
    private BigDecimal discountPct;

    @Column(name = "quantity", precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit", length = 40)
    private String unit;

    @Column(name = "is_negotiable")
    private Boolean isNegotiable = false;

    /** The account's mobile (auto-filled from the owner). */
    @Column(name = "user_mobile_number", length = 15)
    private String userMobileNumber;

    /** Contact number the seller chose for THIS listing on the post-listing confirmation (the default
     *  address's number or a newly typed one). Belongs to the listing only — not saved on any address. */
    @Column(name = "contact_number", length = 15)
    private String contactNumber;

    /** true = show the contact number to buyers directly; false (default) = hide it until the buyer's
     *  contact request is approved. Read by buyer-browse to decide reveal vs. masked + request. */
    @Column(name = "show_contact", nullable = false)
    private Boolean showContact = false;

    /**
     * true = seller is willing to deliver the item to the buyer's location.
     * Only meaningful for SELL listings; always false on RENT listings.
     * Shown as a "Delivery Available" badge on browse cards and listing detail.
     */
    @ColumnDefault("false")
    @Column(name = "delivery_available", nullable = false)
    private Boolean deliveryAvailable = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images", columnDefinition = "jsonb")
    private List<String> images = new ArrayList<>();

    /**
     * Reference to the saved address (UserAddress.addressId) this listing was created from — so the
     * frontend can identify / highlight the source address. Null when a raw inline address was sent.
     * The {@code address} below is an independent SNAPSHOT that stays fixed even if the saved address
     * is later edited or deleted.
     */
    @Column(name = "address_id", length = 20)
    private String addressId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address", columnDefinition = "jsonb")
    private Address address;

    /** Category-specific answers keyed by field_key, as defined by the form metadata. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ListingStatus status = ListingStatus.ACTIVE;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "favorite_count", nullable = false)
    private Long favoriteCount = 0L;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @ColumnDefault("0")
    @Column(name = "contact_reveal_count", nullable = false)
    private Long contactRevealCount = 0L;

    /** Extracted from address snapshot for efficient geo queries — updated on every address change. */
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    /** Pre-computed full-text search vector (tsvector). Written only via native SQL in ListingRepository. */
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector;

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "updated_by", length = 20)
    private String updatedBy;

    /**
     * true = this listing appears in the Flash Deals section.
     * Admin or the seller (on supported plans) can flag it.
     */
    @Column(name = "flash_deal", nullable = false, columnDefinition = "boolean default false")
    private Boolean flashDeal = false;

    /**
     * When the discounted price / flash deal expires.
     * null = no expiry. After this time, discountPct is ignored on the card.
     */
    @Column(name = "discount_expires_at")
    private LocalDateTime discountExpiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
