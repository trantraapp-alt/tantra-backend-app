package com.hyperlocal.tantra.modules.admin.controller;

import com.hyperlocal.tantra.modules.admin.dto.AdminBlockRequest;
import com.hyperlocal.tantra.modules.admin.dto.AdminGrantSubscriptionRequest;
import com.hyperlocal.tantra.modules.admin.dto.AdminUserDetailDTO;
import com.hyperlocal.tantra.modules.admin.dto.AdminUserListDTO;
import com.hyperlocal.tantra.modules.admin.service.AdminUserService;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.listing.entity.Listing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin-only user management endpoints.
 * All routes under /api/v1/admin/** are restricted to ROLE_ADMIN via SecurityConfig.
 *
 *  GET    /api/v1/admin/users                         → list users with filters
 *  GET    /api/v1/admin/users/{userId}                → user full detail
 *  PUT    /api/v1/admin/users/{userId}/block          → block user
 *  PUT    /api/v1/admin/users/{userId}/unblock        → unblock user
 *  POST   /api/v1/admin/users/{userId}/subscription   → grant subscription
 *  DELETE /api/v1/admin/users/{userId}/subscription   → revoke subscription
 *  GET    /api/v1/admin/users/{userId}/listings       → all listings (admin view)
 *  DELETE /api/v1/admin/listings/{listingId}          → force delete any listing
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminUserController {

    @Autowired private AdminUserService adminUserService;
    @Autowired private UserRepository   userRepository;

    // ── User List ─────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserListDTO>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Boolean isBlocked,
            @RequestParam(required = false) Boolean hasSub,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
                adminUserService.listUsers(search, userId, isBlocked, hasSub, fromDate, toDate, pageable));
    }

    // ── User Detail ───────────────────────────────────────────────────────────

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDetailDTO> getUserDetail(@PathVariable String userId) {
        return ResponseEntity.ok(adminUserService.getUserDetail(userId));
    }

    // ── Block ─────────────────────────────────────────────────────────────────

    @PutMapping("/users/{userId}/block")
    public ResponseEntity<Map<String, String>> blockUser(
            @PathVariable String userId,
            @RequestBody AdminBlockRequest req,
            Authentication auth) {

        adminUserService.blockUser(resolveAdminUserId(auth), userId, req);
        return ResponseEntity.ok(Map.of("message", "User blocked successfully."));
    }

    // ── Unblock ───────────────────────────────────────────────────────────────

    @PutMapping("/users/{userId}/unblock")
    public ResponseEntity<Map<String, String>> unblockUser(
            @PathVariable String userId,
            Authentication auth) {

        adminUserService.unblockUser(resolveAdminUserId(auth), userId);
        return ResponseEntity.ok(Map.of("message", "User unblocked successfully."));
    }

    // ── Grant Subscription ────────────────────────────────────────────────────

    @PostMapping("/users/{userId}/subscription")
    public ResponseEntity<Map<String, String>> grantSubscription(
            @PathVariable String userId,
            @RequestBody AdminGrantSubscriptionRequest req,
            Authentication auth) {

        adminUserService.grantSubscription(resolveAdminUserId(auth), userId, req);
        return ResponseEntity.ok(Map.of("message", "Subscription granted successfully."));
    }

    // ── Revoke Subscription ───────────────────────────────────────────────────

    @DeleteMapping("/users/{userId}/subscription")
    public ResponseEntity<Map<String, String>> revokeSubscription(
            @PathVariable String userId,
            Authentication auth) {

        adminUserService.revokeSubscription(resolveAdminUserId(auth), userId);
        return ResponseEntity.ok(Map.of("message", "Subscription revoked successfully."));
    }

    // ── User's Listings (admin view — all statuses) ───────────────────────────

    @GetMapping("/users/{userId}/listings")
    public ResponseEntity<List<Listing>> getUserListings(@PathVariable String userId) {
        return ResponseEntity.ok(adminUserService.getUserListings(userId));
    }

    // ── Force Delete Listing ──────────────────────────────────────────────────

    @DeleteMapping("/listings/{listingId}")
    public ResponseEntity<Map<String, String>> forceDeleteListing(
            @PathVariable String listingId,
            Authentication auth) {

        adminUserService.forceDeleteListing(resolveAdminUserId(auth), listingId);
        return ResponseEntity.ok(Map.of("message", "Listing deleted successfully."));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Resolves admin's userId from their JWT mobile number. */
    private String resolveAdminUserId(Authentication auth) {
        return userRepository.findByMobileNumber(auth.getName())
                .map(User::getUserId)
                .orElse("UNKNOWN_ADMIN");
    }
}
