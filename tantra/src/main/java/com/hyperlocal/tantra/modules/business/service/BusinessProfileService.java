package com.hyperlocal.tantra.modules.business.service;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.audit.service.AuditService;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.business.dto.BusinessProfileRequest;
import com.hyperlocal.tantra.modules.business.dto.BusinessProfileResponse;
import com.hyperlocal.tantra.modules.business.entity.BusinessProfile;
import com.hyperlocal.tantra.modules.business.model.VerificationStatus;
import com.hyperlocal.tantra.modules.business.repository.BusinessProfileRepository;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.notification.service.NotificationService;
import com.hyperlocal.tantra.utils.IdGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business profile lifecycle: owner CRUD (create/update reset it to PENDING for re-verification),
 * admin approve/reject, and the "verified" lookup used to badge listings. Every action is audited;
 * the owner is notified on approve/reject. Owner is always resolved from the JWT.
 */
@Service
public class BusinessProfileService {

    @Autowired private BusinessProfileRepository profileRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditService auditService;
    @Autowired private NotificationService notificationService;

    // ---------------- owner CRUD ----------------

    @Transactional
    public BusinessProfileResponse create(BusinessProfileRequest req, String mobileNumber) {
        User user = currentUser(mobileNumber);
        if (isBlank(req.getBusinessName()) || isBlank(req.getProfileType())) {
            throw new LocalizedException(
                    MessageConstants.BUSINESS_PROFILE_INVALID_EN, MessageConstants.BUSINESS_PROFILE_INVALID_HI);
        }
        BusinessProfile profile = new BusinessProfile();
        profile.setProfileId(generateUniqueProfileId());
        profile.setUserId(user.getUserId());
        apply(profile, req);
        profile.setVerificationStatus(VerificationStatus.PENDING);

        BusinessProfile saved = profileRepository.save(profile);
        auditService.record("BUSINESS_PROFILE_CREATED", "BUSINESS_PROFILE", saved.getProfileId(), user.getUserId());
        return respond(saved, MessageConstants.BUSINESS_PROFILE_CREATE_SUCCESS_EN,
                MessageConstants.BUSINESS_PROFILE_CREATE_SUCCESS_HI);
    }

    @Transactional
    public BusinessProfileResponse update(String profileId, BusinessProfileRequest req, String mobileNumber) {
        User user = currentUser(mobileNumber);
        BusinessProfile profile = owned(profileId, user);
        // A BLOCKED profile is a permanent take-down — the owner cannot edit or resubmit it.
        if (profile.getVerificationStatus() == VerificationStatus.BLOCKED) {
            throw new LocalizedException(
                    MessageConstants.BUSINESS_PROFILE_BLOCKED_EDIT_EN, MessageConstants.BUSINESS_PROFILE_BLOCKED_EDIT_HI);
        }
        apply(profile, req);
        // Content changed → needs re-verification.
        profile.setVerificationStatus(VerificationStatus.PENDING);
        profile.setVerifiedBy(null);
        profile.setVerifiedAt(null);
        profile.setRejectReason(null);
        profile.setUpdatedAt(LocalDateTime.now());

        BusinessProfile saved = profileRepository.save(profile);
        auditService.record("BUSINESS_PROFILE_UPDATED", "BUSINESS_PROFILE", saved.getProfileId(), user.getUserId());
        return respond(saved, MessageConstants.BUSINESS_PROFILE_UPDATE_SUCCESS_EN,
                MessageConstants.BUSINESS_PROFILE_UPDATE_SUCCESS_HI);
    }

    @Transactional
    public BusinessProfileResponse delete(String profileId, String mobileNumber) {
        User user = currentUser(mobileNumber);
        BusinessProfile profile = owned(profileId, user);
        profile.setIsDeleted(true);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);
        auditService.record("BUSINESS_PROFILE_DELETED", "BUSINESS_PROFILE", profile.getProfileId(), user.getUserId());
        return respond(profile, MessageConstants.BUSINESS_PROFILE_DELETE_SUCCESS_EN,
                MessageConstants.BUSINESS_PROFILE_DELETE_SUCCESS_HI);
    }

    public List<BusinessProfile> getMine(String mobileNumber) {
        User user = currentUser(mobileNumber);
        return profileRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(user.getUserId());
    }

    /** View a profile: owner always; others only if APPROVED + visible (for buyers to see dealers). */
    public BusinessProfile getVisible(String profileId, String mobileNumber) {
        BusinessProfile profile = profileRepository.findByProfileIdAndIsDeletedFalse(profileId)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.BUSINESS_PROFILE_NOT_FOUND_EN, MessageConstants.BUSINESS_PROFILE_NOT_FOUND_HI));
        User user = currentUser(mobileNumber);
        boolean isOwner = user.getUserId().equals(profile.getUserId());
        boolean publiclyVisible = profile.getVerificationStatus() == VerificationStatus.APPROVED
                && Boolean.TRUE.equals(profile.getIsVisible());
        if (!isOwner && !publiclyVisible) {
            throw new LocalizedException(
                    MessageConstants.BUSINESS_PROFILE_NOT_FOUND_EN, MessageConstants.BUSINESS_PROFILE_NOT_FOUND_HI);
        }
        return profile;
    }

    /** Approved + visible profiles for a seller — used to badge their listings / show the dealer. */
    public List<BusinessProfile> getVerifiedProfiles(String userId) {
        return profileRepository.findByUserIdAndVerificationStatusAndIsDeletedFalse(userId, VerificationStatus.APPROVED)
                .stream().filter(p -> Boolean.TRUE.equals(p.getIsVisible())).toList();
    }

    // ---------------- admin verification ----------------

    /**
     * Approval Tracker — status counts for the admin dashboard (global + this admin's own tally). Each count
     * is a tile the dashboard links to the matching {@code ?status=} list.
     */
    public com.hyperlocal.tantra.modules.business.dto.BusinessProfileStats getStats(String adminMobile) {
        User admin = currentUser(adminMobile);
        var stats = new com.hyperlocal.tantra.modules.business.dto.BusinessProfileStats();
        stats.setTotal(profileRepository.countByIsDeletedFalse());
        stats.setPending(profileRepository.countByVerificationStatusAndIsDeletedFalse(VerificationStatus.PENDING));
        stats.setApproved(profileRepository.countByVerificationStatusAndIsDeletedFalse(VerificationStatus.APPROVED));
        stats.setRejected(profileRepository.countByVerificationStatusAndIsDeletedFalse(VerificationStatus.REJECTED));
        stats.setBlocked(profileRepository.countByVerificationStatusAndIsDeletedFalse(VerificationStatus.BLOCKED));

        var mine = new com.hyperlocal.tantra.modules.business.dto.BusinessProfileStats.ReviewedByMe();
        mine.setApproved(profileRepository.countByVerifiedByAndVerificationStatusAndIsDeletedFalse(admin.getUserId(), VerificationStatus.APPROVED));
        mine.setRejected(profileRepository.countByVerifiedByAndVerificationStatusAndIsDeletedFalse(admin.getUserId(), VerificationStatus.REJECTED));
        mine.setBlocked(profileRepository.countByVerifiedByAndVerificationStatusAndIsDeletedFalse(admin.getUserId(), VerificationStatus.BLOCKED));
        stats.setReviewedByMe(mine);
        return stats;
    }

    /** Pending verification queue (default PENDING). Approved/rejected profiles drop out of here. */
    public Page<BusinessProfile> getQueue(VerificationStatus status, Pageable pageable) {
        VerificationStatus effective = status != null ? status : VerificationStatus.PENDING;
        return profileRepository.findByVerificationStatusAndIsDeletedFalse(effective, pageable);
    }

    /**
     * Review history — every profile the admin has already acted on (APPROVED or REJECTED), newest review
     * first. From here an admin can re-open an approved profile and reject it (take-down) if it turns out to
     * carry offensive/violating content. Carries verifiedBy / verifiedAt / rejectReason for the audit trail.
     */
    public Page<BusinessProfile> getHistory(Pageable pageable) {
        return profileRepository.findByVerificationStatusInAndIsDeletedFalse(
                List.of(VerificationStatus.APPROVED, VerificationStatus.REJECTED, VerificationStatus.BLOCKED), pageable);
    }

    @Transactional
    public BusinessProfileResponse approve(String profileId, String adminMobile) {
        User admin = currentUser(adminMobile);
        BusinessProfile profile = requireProfile(profileId);
        profile.setVerificationStatus(VerificationStatus.APPROVED);
        profile.setVerifiedBy(admin.getUserId());
        profile.setVerifiedAt(LocalDateTime.now());
        profile.setRejectReason(null);
        profile.setBlockReason(null);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        auditService.record("BUSINESS_PROFILE_APPROVED", "BUSINESS_PROFILE", profile.getProfileId(), admin.getUserId());
        notificationService.push(profile.getUserId(), "BUSINESS_PROFILE_APPROVED",
                LocalizedText.of("Business profile verified", "व्यवसाय प्रोफ़ाइल सत्यापित"),
                LocalizedText.of("Your business profile has been verified.", "आपकी व्यवसाय प्रोफ़ाइल सत्यापित हो गई है।"),
                "BUSINESS_PROFILE", profile.getProfileId());
        return respond(profile, MessageConstants.BUSINESS_PROFILE_APPROVED_EN, MessageConstants.BUSINESS_PROFILE_APPROVED_HI);
    }

    /**
     * Reject a profile — works both on a PENDING profile (first review) and on an already-APPROVED one
     * (take-down for offensive/violating content found later). Rejecting flips it out of public view
     * automatically (getVisible/getVerifiedProfiles only return APPROVED).
     */
    @Transactional
    public BusinessProfileResponse reject(String profileId, String reason, String adminMobile) {
        User admin = currentUser(adminMobile);
        BusinessProfile profile = requireProfile(profileId);
        boolean takeDown = profile.getVerificationStatus() == VerificationStatus.APPROVED;

        profile.setVerificationStatus(VerificationStatus.REJECTED);
        profile.setVerifiedBy(admin.getUserId());
        profile.setVerifiedAt(LocalDateTime.now());
        profile.setRejectReason(reason);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        // Distinct audit action so the history shows a post-approval take-down vs a first-time rejection.
        auditService.record(takeDown ? "BUSINESS_PROFILE_TAKEN_DOWN" : "BUSINESS_PROFILE_REJECTED",
                "BUSINESS_PROFILE", profile.getProfileId(), admin.getUserId());
        LocalizedText body = takeDown
                ? LocalizedText.of(reason != null ? reason : "Your verified business profile was removed due to a policy violation.",
                        reason != null ? reason : "नीति उल्लंघन के कारण आपकी सत्यापित व्यवसाय प्रोफ़ाइल हटा दी गई।")
                : LocalizedText.of(reason != null ? reason : "Your business profile was rejected.",
                        reason != null ? reason : "आपकी व्यवसाय प्रोफ़ाइल अस्वीकृत कर दी गई।");
        notificationService.push(profile.getUserId(), "BUSINESS_PROFILE_REJECTED",
                LocalizedText.of(takeDown ? "Business profile removed" : "Business profile rejected",
                        takeDown ? "व्यवसाय प्रोफ़ाइल हटाई गई" : "व्यवसाय प्रोफ़ाइल अस्वीकृत"),
                body, "BUSINESS_PROFILE", profile.getProfileId());
        return respond(profile, MessageConstants.BUSINESS_PROFILE_REJECTED_EN, MessageConstants.BUSINESS_PROFILE_REJECTED_HI);
    }

    /**
     * Permanently block a profile (offensive/policy-violating content). Unlike REJECTED, the owner cannot
     * edit or resubmit a BLOCKED profile. Auto-hidden from buyers; owner notified with the block reason.
     */
    @Transactional
    public BusinessProfileResponse block(String profileId, String reason, String adminMobile) {
        User admin = currentUser(adminMobile);
        BusinessProfile profile = requireProfile(profileId);
        profile.setVerificationStatus(VerificationStatus.BLOCKED);
        profile.setVerifiedBy(admin.getUserId());
        profile.setVerifiedAt(LocalDateTime.now());
        profile.setBlockReason(reason);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        auditService.record("BUSINESS_PROFILE_BLOCKED", "BUSINESS_PROFILE", profile.getProfileId(), admin.getUserId());
        notificationService.push(profile.getUserId(), "BUSINESS_PROFILE_BLOCKED",
                LocalizedText.of("Business profile blocked", "व्यवसाय प्रोफ़ाइल अवरुद्ध"),
                LocalizedText.of(
                        reason != null ? reason : "Your business profile was permanently blocked for violating our policies.",
                        reason != null ? reason : "नीति उल्लंघन के कारण आपकी व्यवसाय प्रोफ़ाइल स्थायी रूप से अवरुद्ध कर दी गई।"),
                "BUSINESS_PROFILE", profile.getProfileId());
        return respond(profile, MessageConstants.BUSINESS_PROFILE_BLOCKED_EN, MessageConstants.BUSINESS_PROFILE_BLOCKED_HI);
    }

    // ---------------- helpers ----------------

    private void apply(BusinessProfile profile, BusinessProfileRequest req) {
        profile.setProfileType(req.getProfileType());
        profile.setBusinessName(req.getBusinessName());
        profile.setAddress(req.getAddress());
        if (req.getIsVisible() != null) profile.setIsVisible(req.getIsVisible());
        if (req.getAttributes() != null) profile.setAttributes(req.getAttributes());
    }

    private User currentUser(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_USER_NOT_FOUND_EN, MessageConstants.LISTING_USER_NOT_FOUND_HI));
    }

    private BusinessProfile owned(String profileId, User user) {
        BusinessProfile profile = requireProfile(profileId);
        if (!user.getUserId().equals(profile.getUserId())) {
            throw new LocalizedException(
                    MessageConstants.BUSINESS_PROFILE_NOT_OWNER_EN, MessageConstants.BUSINESS_PROFILE_NOT_OWNER_HI);
        }
        return profile;
    }

    private BusinessProfile requireProfile(String profileId) {
        return profileRepository.findByProfileIdAndIsDeletedFalse(profileId)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.BUSINESS_PROFILE_NOT_FOUND_EN, MessageConstants.BUSINESS_PROFILE_NOT_FOUND_HI));
    }

    /** Public profile id: opaque, 10-char, type-prefixed — BP + 8 (e.g. "BP4T1NRK92"). */
    private String generateUniqueProfileId() {
        String id;
        do {
            id = IdGeneratorUtil.generateId("BP");
        } while (profileRepository.existsByProfileId(id));
        return id;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BusinessProfileResponse respond(BusinessProfile profile, String en, String hi) {
        return BusinessProfileResponse.ok(profile.getProfileId(), profile.getVerificationStatus(),
                reasonFor(profile), LocalizedText.of(en, hi));
    }

    /** The reason behind the profile's current status — blocked reason / rejected reason (else null). */
    private String reasonFor(BusinessProfile profile) {
        return switch (profile.getVerificationStatus()) {
            case BLOCKED -> profile.getBlockReason();
            case REJECTED -> profile.getRejectReason();
            default -> null;
        };
    }
}
