package com.hyperlocal.tantra.modules.promo.controller;

import com.hyperlocal.tantra.modules.promo.dto.PromoCardRequest;
import com.hyperlocal.tantra.modules.promo.entity.PromoCard;
import com.hyperlocal.tantra.modules.promo.service.PromoCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin CRUD for promotional/feature cards shown on the home feed.
 * All endpoints require ROLE_ADMIN (enforced by SecurityConfig /api/v1/admin/**).
 *
 * Usage:
 *   POST   /api/v1/admin/promo-cards          — create a new card
 *   GET    /api/v1/admin/promo-cards          — list all (active + inactive)
 *   GET    /api/v1/admin/promo-cards/{id}     — get single card
 *   PUT    /api/v1/admin/promo-cards/{id}     — full update
 *   PATCH  /api/v1/admin/promo-cards/{id}/toggle  — flip isActive
 *   DELETE /api/v1/admin/promo-cards/{id}     — hard delete
 */
@RestController
@RequestMapping("/api/v1/admin/promo-cards")
public class AdminPromoCardController {

    private static final Logger log = LoggerFactory.getLogger(AdminPromoCardController.class);

    @Autowired private PromoCardService promoCardService;

    /** List all cards (admin view — includes inactive). */
    @GetMapping
    public ResponseEntity<List<PromoCard>> listAll() {
        return ResponseEntity.ok(promoCardService.getAllCards());
    }

    /** Get single card by DB id. */
    @GetMapping("/{id}")
    public ResponseEntity<PromoCard> getById(@PathVariable Long id) {
        return ResponseEntity.ok(promoCardService.getById(id));
    }

    /**
     * Create a promo card.
     * Body example:
     * {
     *   "title": {"en":"Kharif Season Sale","hi":"खरीफ सीजन सेल"},
     *   "subtitle": {"en":"Best seeds at best prices","hi":"बेहतरीन दाम पर बेहतरीन बीज"},
     *   "imageUrl": "http://localhost:8080/files/promo1.jpg",
     *   "bgColor": "#FFF3E0",
     *   "textColor": "#212121",
     *   "ctaLabel": {"en":"Shop Seeds","hi":"बीज देखें"},
     *   "ctaType": "CATEGORY",
     *   "ctaValue": "3",
     *   "displayOrder": 1,
     *   "targetDistrict": null,
     *   "isActive": true,
     *   "validFrom": "2026-06-01T00:00:00",
     *   "validTo": "2026-09-30T23:59:59"
     * }
     */
    @PostMapping
    public ResponseEntity<PromoCard> create(
            @RequestBody PromoCardRequest request,
            Authentication auth) {
        log.info("[ADMIN] {} creating promo card", auth.getName());
        return ResponseEntity.ok(promoCardService.create(request, auth.getName()));
    }

    /** Update card content, targeting, or schedule. */
    @PutMapping("/{id}")
    public ResponseEntity<PromoCard> update(
            @PathVariable Long id,
            @RequestBody PromoCardRequest request,
            Authentication auth) {
        log.info("[ADMIN] {} updating promo card id={}", auth.getName(), id);
        return ResponseEntity.ok(promoCardService.update(id, request, auth.getName()));
    }

    /** Toggle card active/inactive without full update. */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<PromoCard> toggle(
            @PathVariable Long id,
            Authentication auth) {
        log.info("[ADMIN] {} toggling promo card id={}", auth.getName(), id);
        return ResponseEntity.ok(promoCardService.toggle(id, auth.getName()));
    }

    /** Permanently delete a card. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            Authentication auth) {
        log.info("[ADMIN] {} deleting promo card id={}", auth.getName(), id);
        promoCardService.delete(id, auth.getName());
        return ResponseEntity.ok(Map.of(
                "message", Map.of("en", "Promo card deleted.", "hi", "प्रमो कार्ड हटा दिया गया।")));
    }
}
