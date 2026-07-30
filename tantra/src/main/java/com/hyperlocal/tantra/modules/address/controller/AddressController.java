package com.hyperlocal.tantra.modules.address.controller;

import com.hyperlocal.tantra.modules.address.dto.AddressRequest;
import com.hyperlocal.tantra.modules.address.dto.AddressResponse;
import com.hyperlocal.tantra.modules.address.entity.UserAddress;
import com.hyperlocal.tantra.modules.address.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Address book (Profile ▸ Address). Authenticated; owner resolved from the JWT.
 * Locked to authenticated via /api/v1/addresses/** in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {

    @Autowired private AddressService service;

    @GetMapping
    public ResponseEntity<List<UserAddress>> list(Authentication auth) {
        return ResponseEntity.ok(service.list(auth.getName()));
    }

    @GetMapping("/default")
    public ResponseEntity<UserAddress> getDefault(Authentication auth) {
        return ResponseEntity.ok(service.getDefault(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@RequestBody AddressRequest request, Authentication auth) {
        return ResponseEntity.ok(service.create(request, auth.getName()));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> update(
            @PathVariable String addressId, @RequestBody AddressRequest request, Authentication auth) {
        return ResponseEntity.ok(service.update(addressId, request, auth.getName()));
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<AddressResponse> setDefault(@PathVariable String addressId, Authentication auth) {
        return ResponseEntity.ok(service.setDefault(addressId, auth.getName()));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<AddressResponse> delete(@PathVariable String addressId, Authentication auth) {
        return ResponseEntity.ok(service.delete(addressId, auth.getName()));
    }
}
