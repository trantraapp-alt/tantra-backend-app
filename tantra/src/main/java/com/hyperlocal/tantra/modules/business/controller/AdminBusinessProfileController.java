package com.hyperlocal.tantra.modules.business.controller;

import com.hyperlocal.tantra.modules.business.dto.BusinessProfileResponse;
import com.hyperlocal.tantra.modules.business.dto.BusinessProfileStats;
import com.hyperlocal.tantra.modules.business.entity.BusinessProfile;
import com.hyperlocal.tantra.modules.business.model.VerificationStatus;
import com.hyperlocal.tantra.modules.business.service.BusinessProfileService;
import com.hyperlocal.tantra.modules.listing.dto.PageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin verification dashboard for business profiles. Locked to ROLE_ADMIN via
 * /api/v1/admin/** in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/admin/business-profiles")
public class AdminBusinessProfileController {

    @Autowired private BusinessProfileService service;

    /** Approval Tracker — status counts for the admin dashboard (global + this admin's own tally). */
    @GetMapping("/stats")
    public ResponseEntity<BusinessProfileStats> stats(Authentication auth) {
        return ResponseEntity.ok(service.getStats(auth.getName()));
    }

    /** Verification queue (default PENDING), paginated. Approved/rejected profiles leave this queue. */
    @GetMapping
    public ResponseEntity<PageResponse<BusinessProfile>> queue(
            @RequestParam(required = false) VerificationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(service.getQueue(status, pageable)));
    }

    /**
     * Approval / review history — profiles already acted on (APPROVED or REJECTED), newest review first.
     * From here an admin can re-open an approved profile and reject it (take-down) if it carries offensive content.
     */
    @GetMapping("/history")
    public ResponseEntity<PageResponse<BusinessProfile>> history(
            @PageableDefault(size = 20, sort = "verifiedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(service.getHistory(pageable)));
    }

    @PostMapping("/{profileId}/approve")
    public ResponseEntity<BusinessProfileResponse> approve(@PathVariable String profileId, Authentication auth) {
        return ResponseEntity.ok(service.approve(profileId, auth.getName()));
    }

    @PostMapping("/{profileId}/reject")
    public ResponseEntity<BusinessProfileResponse> reject(
            @PathVariable String profileId, @RequestBody(required = false) Map<String, String> body, Authentication auth) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(service.reject(profileId, reason, auth.getName()));
    }

    /** Permanently block a profile (offensive/policy violation) — owner cannot resubmit. */
    @PostMapping("/{profileId}/block")
    public ResponseEntity<BusinessProfileResponse> block(
            @PathVariable String profileId, @RequestBody(required = false) Map<String, String> body, Authentication auth) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(service.block(profileId, reason, auth.getName()));
    }
}
