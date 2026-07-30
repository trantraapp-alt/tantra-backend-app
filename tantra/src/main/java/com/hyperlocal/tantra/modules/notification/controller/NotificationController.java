package com.hyperlocal.tantra.modules.notification.controller;

import com.hyperlocal.tantra.modules.listing.dto.PageResponse;
import com.hyperlocal.tantra.modules.notification.entity.Notification;
import com.hyperlocal.tantra.modules.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** In-app notifications for the authenticated user. */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    @Autowired private NotificationService service;

    @GetMapping
    public ResponseEntity<PageResponse<Notification>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {
        return ResponseEntity.ok(PageResponse.of(service.list(auth.getName(), pageable)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", service.unreadCount(auth.getName())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, Boolean>> markRead(@PathVariable Long id, Authentication auth) {
        service.markRead(id, auth.getName());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
