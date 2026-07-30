package com.hyperlocal.tantra.modules.business.entity;

import com.hyperlocal.tantra.modules.business.model.VerificationStatus;
import com.hyperlocal.tantra.modules.listing.model.Address;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * A dealer's business profile (Seed Dealer, Vet Clinic, Nursery, …). Verified manually by an admin;
 * once APPROVED it drives the "verified" badge on the owner's matching listings. A user may hold
 * several profiles. {@code isVisible} lets buyers discover the dealer directly.
 *
 * <p>Note: never store raw Aadhaar. GST is optional; real KYC will store only verification refs.</p>
 */
@Entity
@Table(name = "business_profiles", indexes = {
        @Index(name = "idx_bp_user", columnList = "user_id"),
        @Index(name = "idx_bp_status", columnList = "verification_status")
})
@Data
public class BusinessProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public, opaque, 10-char type-prefixed id — BP + 8 (e.g. "BP4T1NRK92"). */
    @Column(name = "profile_id", unique = true, nullable = false, length = 20)
    private String profileId;

    @Column(name = "user_id", nullable = false, length = 20)
    private String userId;

    /** Value from the admin-managed 'business_profile_type' option set (e.g. seed_dealer, vet_clinic). */
    @Column(name = "profile_type", nullable = false, length = 60)
    private String profileType;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    /** Config-driven, type-specific fields answered against the business-profile form (owner name,
     *  description, contact, GST, registration no., …). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, Object> attributes = new java.util.HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address", columnDefinition = "jsonb")
    private Address address;

    /** The form definition + version this profile was created against (deterministic edits). */
    @Column(name = "form_id")
    private Integer formId;

    @Column(name = "form_version")
    private Integer formVersion;

    /** Verification document references (added later via a KYC flow — not raw ids). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "documents", columnDefinition = "jsonb")
    private Map<String, Object> documents;

    /** Whether buyers can discover this profile / contact the dealer. */
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 15)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "verified_by", length = 20)
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    /** Reason shown to the owner when an admin permanently blocks the profile (BLOCKED). */
    @Column(name = "block_reason", length = 255)
    private String blockReason;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
