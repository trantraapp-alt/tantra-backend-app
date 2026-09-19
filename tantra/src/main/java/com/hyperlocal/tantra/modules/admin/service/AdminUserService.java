package com.hyperlocal.tantra.modules.admin.service;

import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.address.entity.UserAddress;
import com.hyperlocal.tantra.modules.address.repository.UserAddressRepository;
import com.hyperlocal.tantra.modules.admin.dto.AdminBlockRequest;
import com.hyperlocal.tantra.modules.admin.dto.AdminGrantSubscriptionRequest;
import com.hyperlocal.tantra.modules.admin.dto.AdminUserDetailDTO;
import com.hyperlocal.tantra.modules.admin.dto.AdminUserListDTO;
import com.hyperlocal.tantra.modules.audit.service.AuditService;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.business.entity.BusinessProfile;
import com.hyperlocal.tantra.modules.business.repository.BusinessProfileRepository;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import com.hyperlocal.tantra.modules.listing.model.ListingStatus;
import com.hyperlocal.tantra.modules.listing.repository.ListingRepository;
import com.hyperlocal.tantra.modules.subscription.entity.SubscriptionPlan;
import com.hyperlocal.tantra.modules.subscription.entity.UserSubscription;
import com.hyperlocal.tantra.modules.subscription.repository.SubscriptionPlanRepository;
import com.hyperlocal.tantra.modules.subscription.repository.UserSubscriptionRepository;
import com.hyperlocal.tantra.utils.IdGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    @Autowired private UserRepository             userRepository;
    @Autowired private ListingRepository          listingRepository;
    @Autowired private UserAddressRepository      addressRepository;
    @Autowired private BusinessProfileRepository  businessProfileRepository;
    @Autowired private UserSubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionPlanRepository planRepository;
    @Autowired private AuditService               auditService;

    // ── 1. User List ──────────────────────────────────────────────────────────

    public Page<AdminUserListDTO> listUsers(
            String search,
            String filterUserId,
            Boolean isBlocked,
            Boolean hasSub,
            String fromDate,
            String toDate,
            Pageable pageable) {

        String searchLike = (search != null && !search.isBlank()) ? "%" + search.trim() + "%" : null;

        Page<User> users = userRepository.adminSearch(
                searchLike, filterUserId, isBlocked, fromDate, toDate, hasSub, pageable);

        return users.map(this::toListDTO);
    }

    private AdminUserListDTO toListDTO(User u) {
        // Subscription badge
        String subBadge = "FREE";
        java.util.Optional<UserSubscription> activeSub =
                subscriptionRepository.findActiveByUserId(u.getUserId(), LocalDateTime.now());
        if (activeSub.isPresent()) {
            java.util.Optional<SubscriptionPlan> plan =
                    planRepository.findById(activeSub.get().getPlanId());
            subBadge = plan.map(SubscriptionPlan::getPlanKey).orElse("PREMIUM");
        }

        // Business profile badge
        String bpBadge = "NONE";
        List<BusinessProfile> bps =
                businessProfileRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(u.getUserId());
        if (!bps.isEmpty()) {
            bpBadge = bps.get(0).getVerificationStatus().name();
        }

        return AdminUserListDTO.builder()
                .userId(u.getUserId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .mobileNumber(u.getMobileNumber())
                .appUsageRole(u.getAppUsageRole())
                .status(Boolean.TRUE.equals(u.getIsBlocked()) ? "BLOCKED" : "ACTIVE")
                .joinedAt(u.getCreatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .subscriptionBadge(subBadge)
                .businessProfileBadge(bpBadge)
                .build();
    }

    // ── 2. User Detail ────────────────────────────────────────────────────────

    public AdminUserDetailDTO getUserDetail(String userId) {
        User u = findUser(userId);

        // Activity summary
        List<Listing> allListings = listingRepository.findByUserIdOrderByCreatedAtDesc(u.getUserId());
        long total  = allListings.size();
        long active = allListings.stream().filter(l -> l.getStatus() == ListingStatus.ACTIVE).count();
        long sold   = allListings.stream().filter(l -> l.getStatus() == ListingStatus.SOLD).count();
        LocalDateTime lastListingAt = allListings.isEmpty() ? null : allListings.get(0).getCreatedAt();

        AdminUserDetailDTO.ActivitySummary activity = AdminUserDetailDTO.ActivitySummary.builder()
                .totalListings(total)
                .activeListings(active)
                .soldListings(sold)
                .lastListingAt(lastListingAt)
                .build();

        // Subscription
        AdminUserDetailDTO.SubscriptionInfo subInfo = null;
        List<UserSubscription> subs = subscriptionRepository.findByUserIdOrderByCreatedAtDesc(u.getUserId());
        if (!subs.isEmpty()) {
            UserSubscription latest = subs.get(0);
            String planName = planRepository.findById(latest.getPlanId())
                    .map(SubscriptionPlan::getPlanKey).orElse("UNKNOWN");
            subInfo = AdminUserDetailDTO.SubscriptionInfo.builder()
                    .subscriptionId(latest.getSubscriptionId())
                    .planName(planName)
                    .planKey(planName)
                    .startedAt(latest.getStartedAt())
                    .expiresAt(latest.getExpiresAt())
                    .status(latest.getStatus())
                    .grantedBy(latest.getGrantedBy())
                    .build();
        }

        // Business profile
        AdminUserDetailDTO.BusinessProfileInfo bpInfo = null;
        List<BusinessProfile> bps =
                businessProfileRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(u.getUserId());
        if (!bps.isEmpty()) {
            BusinessProfile bp = bps.get(0);
            bpInfo = AdminUserDetailDTO.BusinessProfileInfo.builder()
                    .profileId(bp.getProfileId())
                    .businessName(bp.getBusinessName())
                    .profileType(bp.getProfileType())
                    .verificationStatus(bp.getVerificationStatus().name())
                    .build();
        }

        // Addresses
        List<UserAddress> addrs = addressRepository.findByUserIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(u.getUserId());
        List<AdminUserDetailDTO.AddressSummary> addrList = addrs.stream()
                .map(a -> AdminUserDetailDTO.AddressSummary.builder()
                        .addressId(a.getAddressId())
                        .label(a.getLabel())
                        .fullAddress(a.getFullAddress())
                        .district(a.getDistrict())
                        .state(a.getState())
                        .isDefault(a.getIsDefault())
                        .build())
                .collect(Collectors.toList());

        return AdminUserDetailDTO.builder()
                .userId(u.getUserId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .mobileNumber(u.getMobileNumber())
                .preferredLanguage(u.getPreferredLanguage())
                .appUsageRole(u.getAppUsageRole())
                .joinedAt(u.getCreatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .status(Boolean.TRUE.equals(u.getIsBlocked()) ? "BLOCKED" : "ACTIVE")
                .blockedReason(u.getBlockedReason())
                .blockedAt(u.getBlockedAt())
                .activity(activity)
                .subscription(subInfo)
                .businessProfile(bpInfo)
                .addresses(addrList)
                .build();
    }

    // ── 3. Block User ─────────────────────────────────────────────────────────

    @Transactional
    public void blockUser(String adminUserId, String targetUserId, AdminBlockRequest req) {
        User user = findUser(targetUserId);

        if (Boolean.TRUE.equals(user.getIsBlocked())) {
            throw new LocalizedException("User is already blocked.", "उपयोगकर्ता पहले से ब्लॉक है।");
        }

        user.setIsBlocked(true);
        user.setBlockedAt(LocalDateTime.now());
        user.setBlockedReason(req.getReason());
        userRepository.save(user);

        // Hide all active listings
        List<Listing> listings = listingRepository.findByUserIdAndIsDeletedFalse(user.getUserId());
        for (Listing l : listings) {
            if (l.getStatus() == ListingStatus.ACTIVE) {
                l.setStatus(ListingStatus.INACTIVE);
                listingRepository.save(l);
            }
        }

        auditService.record("USER_BLOCKED", "USER", targetUserId, adminUserId,
                Map.of("reason", req.getReason() != null ? req.getReason() : "",
                        "notes", req.getNotes() != null ? req.getNotes() : ""));

        log.info("[ADMIN] User {} blocked by admin {} — reason: {}", targetUserId, adminUserId, req.getReason());
    }

    // ── 4. Unblock User ───────────────────────────────────────────────────────

    @Transactional
    public void unblockUser(String adminUserId, String targetUserId) {
        User user = findUser(targetUserId);

        if (!Boolean.TRUE.equals(user.getIsBlocked())) {
            throw new LocalizedException("User is not blocked.", "उपयोगकर्ता ब्लॉक नहीं है।");
        }

        user.setIsBlocked(false);
        user.setBlockedAt(null);
        user.setBlockedReason(null);
        userRepository.save(user);

        auditService.record("USER_UNBLOCKED", "USER", targetUserId, adminUserId, null);

        log.info("[ADMIN] User {} unblocked by admin {}", targetUserId, adminUserId);
    }

    // ── 5. Grant Subscription ─────────────────────────────────────────────────

    @Transactional
    public void grantSubscription(String adminUserId, String targetUserId, AdminGrantSubscriptionRequest req) {
        User user = findUser(targetUserId);

        SubscriptionPlan plan = planRepository.findById(req.getPlanId())
                .orElseThrow(() -> new LocalizedException("Plan not found.", "प्लान नहीं मिला।"));

        // Cancel any existing active subscription
        subscriptionRepository.findActiveByUserId(user.getUserId(), LocalDateTime.now())
                .ifPresent(existing -> {
                    existing.setStatus("CANCELLED");
                    subscriptionRepository.save(existing);
                });

        // Create new subscription
        UserSubscription sub = new UserSubscription();
        sub.setSubscriptionId(IdGeneratorUtil.generateId("SB"));
        sub.setUserId(user.getUserId());
        sub.setPlanId(plan.getId());
        sub.setStatus("ACTIVE");
        sub.setStartedAt(LocalDateTime.now());
        sub.setExpiresAt(LocalDateTime.now().plusDays(req.getDurationDays()));
        sub.setGrantedBy(adminUserId);
        sub.setNotes(req.getNotes());
        sub.setAutoRenew(false);
        subscriptionRepository.save(sub);

        auditService.record("SUBSCRIPTION_GRANTED", "USER", targetUserId, adminUserId,
                Map.of("planId", plan.getId(), "durationDays", req.getDurationDays()));

        log.info("[ADMIN] Subscription {} granted to user {} by admin {}", plan.getPlanKey(), targetUserId, adminUserId);
    }

    // ── 6. Revoke Subscription ────────────────────────────────────────────────

    @Transactional
    public void revokeSubscription(String adminUserId, String targetUserId) {
        User user = findUser(targetUserId);

        UserSubscription sub = subscriptionRepository.findActiveByUserId(user.getUserId(), LocalDateTime.now())
                .orElseThrow(() -> new LocalizedException(
                        "No active subscription found.", "कोई सक्रिय सब्सक्रिप्शन नहीं मिला।"));

        sub.setStatus("CANCELLED");
        subscriptionRepository.save(sub);

        auditService.record("SUBSCRIPTION_REVOKED", "USER", targetUserId, adminUserId, null);

        log.info("[ADMIN] Subscription revoked for user {} by admin {}", targetUserId, adminUserId);
    }

    // ── 7. User's Listings (admin view — all statuses) ────────────────────────

    public List<Listing> getUserListings(String userId) {
        findUser(userId); // validates user exists
        return listingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ── 8. Force Delete Listing ───────────────────────────────────────────────

    @Transactional
    public void forceDeleteListing(String adminUserId, String listingId) {
        Listing listing = listingRepository.findByListingId(listingId)
                .orElseThrow(() -> new LocalizedException("Listing not found.", "लिस्टिंग नहीं मिली।"));

        listing.setIsDeleted(true);
        listing.setStatus(ListingStatus.DELETED);
        listingRepository.save(listing);

        auditService.record("LISTING_FORCE_DELETED", "LISTING", listingId, adminUserId,
                Map.of("sellerUserId", listing.getUserId()));

        log.info("[ADMIN] Listing {} force-deleted by admin {}", listingId, adminUserId);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private User findUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new LocalizedException("User not found.", "उपयोगकर्ता नहीं मिला।"));
    }
}
