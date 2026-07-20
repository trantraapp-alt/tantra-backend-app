package com.hyperlocal.tantra.modules.listing.entity;

import com.hyperlocal.tantra.modules.forms.model.ListingType;
import com.hyperlocal.tantra.modules.listing.model.Address;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A product a user has listed to sell/rent. Common, queryable data lives in typed columns while
 * category-specific answers live in the {@code attributes} jsonb — so one table serves every
 * category of every module.
 */
@Entity
@Table(name = "listings")
@Data
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public, human-facing id (e.g. "TN7ABC12"), generated via IdGeneratorUtil. */
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

    @Column(name = "user_mobile_number", length = 15)
    private String userMobileNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images", columnDefinition = "jsonb")
    private List<String> images = new ArrayList<>();

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

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "updated_by", length = 20)
    private String updatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
