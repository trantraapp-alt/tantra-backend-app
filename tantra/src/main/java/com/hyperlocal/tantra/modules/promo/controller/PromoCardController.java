package com.hyperlocal.tantra.modules.promo.controller;

import com.hyperlocal.tantra.modules.promo.entity.PromoCard;
import com.hyperlocal.tantra.modules.promo.service.PromoCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public read-only access to active promo cards.
 * Used by the home feed and dedicated banner/carousel screens.
 */
@RestController
@RequestMapping("/api/v1/promo-cards")
public class PromoCardController {

    @Autowired private PromoCardService promoCardService;

    /**
     * Returns currently active promo cards.
     * Use {@code cardType=CAROUSEL} to fetch only the home-screen banner slides.
     *
     * @param district optional — pass user's district for localized cards
     * @param cardType optional — CAROUSEL / MINI / SCHEME; omit for all types
     */
    @GetMapping
    public ResponseEntity<List<PromoCard>> getActive(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String cardType) {
        return ResponseEntity.ok(promoCardService.getActiveCards(district, cardType != null ? cardType.toUpperCase() : null));
    }
}
