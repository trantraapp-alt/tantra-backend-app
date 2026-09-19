package com.hyperlocal.tantra.modules.notification.controller;

import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.notification.service.PushNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Registers and removes FCM device tokens for the authenticated user.
 * Called by the mobile app on every launch (register) and on logout (remove).
 */
@RestController
@RequestMapping("/api/v1/notifications/device-token")
public class DeviceTokenController {

    @Autowired private PushNotificationService pushService;
    @Autowired private UserRepository userRepository;

    /**
     * POST /api/v1/notifications/device-token
     * Body: { "fcmToken": "...", "platform": "ANDROID" }
     * Register or refresh the FCM token for this device.
     */
    @PostMapping
    public ResponseEntity<?> register(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {

        String userId = resolveUserId(userDetails.getUsername());
        String fcmToken = body.get("fcmToken");
        String platform = body.getOrDefault("platform", "ANDROID");

        if (fcmToken == null || fcmToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "fcmToken is required"));
        }

        pushService.registerToken(userId, fcmToken, platform);
        return ResponseEntity.ok(Map.of(
                "message", Map.of("en", "Device token registered.", "hi", "डिवाइस टोकन पंजीकृत हुआ।")));
    }

    /**
     * DELETE /api/v1/notifications/device-token
     * Body: { "fcmToken": "..." }
     * Remove the token on logout so this device stops receiving pushes.
     */
    @DeleteMapping
    public ResponseEntity<?> remove(@RequestBody Map<String, String> body) {
        String fcmToken = body.get("fcmToken");
        if (fcmToken != null && !fcmToken.isBlank()) {
            pushService.removeToken(fcmToken);
        }
        return ResponseEntity.ok(Map.of(
                "message", Map.of("en", "Device token removed.", "hi", "डिवाइस टोकन हटाया गया।")));
    }

    private String resolveUserId(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber)
                .map(User::getUserId)
                .orElseThrow();
    }
}
