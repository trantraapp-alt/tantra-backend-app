package com.hyperlocal.tantra.modules.deals.controller;

import com.hyperlocal.tantra.modules.deals.dto.DealGroupRequest;
import com.hyperlocal.tantra.modules.deals.entity.DealGroup;
import com.hyperlocal.tantra.modules.deals.service.DealService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin CRUD for deal groups.
 * All endpoints require ROLE_ADMIN (enforced by SecurityConfig /api/v1/admin/**).
 *
 *   GET    /api/v1/admin/deals
 *   GET    /api/v1/admin/deals/{id}
 *   POST   /api/v1/admin/deals
 *   PUT    /api/v1/admin/deals/{id}
 *   PATCH  /api/v1/admin/deals/{id}/toggle
 *   DELETE /api/v1/admin/deals/{id}
 */
@RestController
@RequestMapping("/api/v1/admin/deals")
public class AdminDealController {

    private static final Logger log = LoggerFactory.getLogger(AdminDealController.class);

    @Autowired private DealService dealService;

    @GetMapping
    public ResponseEntity<List<DealGroup>> listAll() {
        return ResponseEntity.ok(dealService.getAllGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DealGroup> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(dealService.getById(id));
    }

    @PostMapping
    public ResponseEntity<DealGroup> create(@RequestBody DealGroupRequest req, Authentication auth) {
        log.info("[ADMIN] {} creating deal group", auth.getName());
        return ResponseEntity.ok(dealService.create(req, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DealGroup> update(@PathVariable Integer id,
                                            @RequestBody DealGroupRequest req,
                                            Authentication auth) {
        log.info("[ADMIN] {} updating deal group id={}", auth.getName(), id);
        return ResponseEntity.ok(dealService.update(id, req, auth.getName()));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<DealGroup> toggle(@PathVariable Integer id, Authentication auth) {
        log.info("[ADMIN] {} toggling deal group id={}", auth.getName(), id);
        return ResponseEntity.ok(dealService.toggle(id, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id, Authentication auth) {
        log.info("[ADMIN] {} deleting deal group id={}", auth.getName(), id);
        dealService.delete(id, auth.getName());
        return ResponseEntity.ok(Map.of("message",
                Map.of("en", "Deal group deleted.", "hi", "डील ग्रुप हटा दिया गया।")));
    }
}
