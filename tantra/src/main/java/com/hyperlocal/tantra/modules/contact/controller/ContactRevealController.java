package com.hyperlocal.tantra.modules.contact.controller;

import com.hyperlocal.tantra.modules.contact.dto.ContactRevealResponse;
import com.hyperlocal.tantra.modules.contact.service.ContactRevealService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Contact reveal endpoint — authenticated buyers only.
 *
 * POST /api/v1/contacts/reveal/{listingId}
 *
 * Returns:
 *   action = REVEALED_FULL    → mobileNumber + altMobileNumber + whatsappUrl
 *   action = WHATSAPP_ONLY    → whatsappUrl only (seller hid direct number)
 */
@RestController
@RequestMapping("/api/v1/contacts")
public class ContactRevealController {

    @Autowired private ContactRevealService contactRevealService;

    /**
     * Reveal contact details for a listing.
     * Buyer identity resolved from JWT — no body required.
     */
    @PostMapping("/reveal/{listingId}")
    public ResponseEntity<ContactRevealResponse> reveal(
            @PathVariable String listingId,
            Authentication auth,
            HttpServletRequest request) {

        String ipAddress = resolveClientIp(request);
        ContactRevealResponse response = contactRevealService.reveal(
                auth.getName(), listingId, ipAddress);

        return ResponseEntity.ok(response);
    }

    /** Extracts real client IP, accounting for reverse proxies. */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
