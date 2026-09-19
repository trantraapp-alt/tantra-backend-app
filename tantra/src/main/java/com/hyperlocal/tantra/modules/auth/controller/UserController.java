package com.hyperlocal.tantra.modules.auth.controller;

import com.hyperlocal.tantra.modules.auth.dto.SellerInfoDTO;
import com.hyperlocal.tantra.modules.auth.service.SellerInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public user-facing endpoints.
 * No auth required — listing detail screens are viewed by non-logged-in users too.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final SellerInfoService sellerInfoService;

    public UserController(SellerInfoService sellerInfoService) {
        this.sellerInfoService = sellerInfoService;
    }

    /**
     * Public seller info for the "Seller Information" card on listing detail screen.
     *
     * GET /api/v1/users/{userId}/seller-info
     *
     * userId comes from ListingCardDTO.userId returned by the listing detail endpoint.
     * No auth required — buyers (even unauthenticated) can view seller info.
     */
    @GetMapping("/{userId}/seller-info")
    public ResponseEntity<SellerInfoDTO> sellerInfo(@PathVariable String userId) {
        return ResponseEntity.ok(sellerInfoService.getSellerInfo(userId));
    }
}
