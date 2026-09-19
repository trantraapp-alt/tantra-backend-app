package com.hyperlocal.tantra.modules.carousel.controller;

import com.hyperlocal.tantra.modules.carousel.dto.CarouselItemRequest;
import com.hyperlocal.tantra.modules.carousel.entity.CarouselItem;
import com.hyperlocal.tantra.modules.carousel.service.CarouselItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin CRUD for the DB-driven carousel.
 * All endpoints require ROLE_ADMIN (enforced by SecurityConfig /api/v1/admin/**).
 *
 *   GET    /api/v1/admin/carousel          — list all items (incl. inactive)
 *   GET    /api/v1/admin/carousel/{id}     — single item
 *   POST   /api/v1/admin/carousel          — create
 *   PUT    /api/v1/admin/carousel/{id}     — full update
 *   PATCH  /api/v1/admin/carousel/{id}/toggle — flip isActive
 *   DELETE /api/v1/admin/carousel/{id}     — hard delete
 */
@RestController
@RequestMapping("/api/v1/admin/carousel")
public class AdminCarouselController {

    private static final Logger log = LoggerFactory.getLogger(AdminCarouselController.class);

    @Autowired private CarouselItemService service;

    @GetMapping
    public ResponseEntity<List<CarouselItem>> listAll() {
        return ResponseEntity.ok(service.getAllItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarouselItem> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<CarouselItem> create(
            @RequestBody CarouselItemRequest request,
            Authentication auth) {
        log.info("[ADMIN] {} creating carousel item", auth.getName());
        return ResponseEntity.ok(service.create(request, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarouselItem> update(
            @PathVariable Integer id,
            @RequestBody CarouselItemRequest request,
            Authentication auth) {
        log.info("[ADMIN] {} updating carousel item id={}", auth.getName(), id);
        return ResponseEntity.ok(service.update(id, request, auth.getName()));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<CarouselItem> toggle(
            @PathVariable Integer id,
            Authentication auth) {
        log.info("[ADMIN] {} toggling carousel item id={}", auth.getName(), id);
        return ResponseEntity.ok(service.toggle(id, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Integer id,
            Authentication auth) {
        log.info("[ADMIN] {} deleting carousel item id={}", auth.getName(), id);
        service.delete(id, auth.getName());
        return ResponseEntity.ok(Map.of(
                "message", Map.of("en", "Carousel item deleted.", "hi", "कैरोसेल आइटम हटा दिया गया।")));
    }
}
