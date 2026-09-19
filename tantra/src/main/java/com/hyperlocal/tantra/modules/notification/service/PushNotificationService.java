package com.hyperlocal.tantra.modules.notification.service;

/*
 * ──────────────────────────────────────────────────────────────────────────────
 * PRODUCTION FCM — uncomment when going live:
 *
 *   1. Add firebase-admin dependency in pom.xml (already commented there).
 *   2. Place your Firebase service-account JSON at the path set in
 *      app.fcm.service-account-path (default: classpath:firebase-service-account.json).
 *   3. Set app.fcm.enabled=true in application.properties.
 *   4. Uncomment the @PostConstruct init block and all // FCM sections below.
 * ──────────────────────────────────────────────────────────────────────────────
 */

// import com.google.auth.oauth2.GoogleCredentials;
// import com.google.firebase.FirebaseApp;
// import com.google.firebase.FirebaseOptions;
// import com.google.firebase.messaging.FirebaseMessaging;
// import com.google.firebase.messaging.FirebaseMessagingException;
// import com.google.firebase.messaging.Message;
// import com.google.firebase.messaging.MulticastMessage;
// import com.google.firebase.messaging.Notification;
// import java.io.InputStream;

import com.hyperlocal.tantra.modules.notification.entity.DeviceToken;
import com.hyperlocal.tantra.modules.notification.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Push notification delivery via Firebase Cloud Messaging (FCM).
 *
 * LOCAL MODE  (app.fcm.enabled=false):
 *   - All send calls are no-ops — logged at DEBUG level only.
 *
 * PRODUCTION MODE (app.fcm.enabled=true):
 *   - Initialise FirebaseApp once in @PostConstruct.
 *   - Multicast send to all device tokens registered for a userId.
 *   - Stale/invalid tokens (404 from FCM) are auto-deleted from DB.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    @Autowired private DeviceTokenRepository deviceTokenRepository;

    @Value("${app.fcm.enabled:false}")
    private boolean fcmEnabled;

    // @Value("${app.fcm.service-account-path:classpath:firebase-service-account.json}")
    // private String serviceAccountPath;

    // FCM — uncomment for production:
    // @PostConstruct
    // public void initFirebase() {
    //     if (!fcmEnabled) return;
    //     try {
    //         InputStream serviceAccount = getClass().getResourceAsStream("/firebase-service-account.json");
    //         FirebaseOptions options = FirebaseOptions.builder()
    //                 .setCredentials(GoogleCredentials.fromStream(serviceAccount))
    //                 .build();
    //         if (FirebaseApp.getApps().isEmpty()) {
    //             FirebaseApp.initializeApp(options);
    //             log.info("[FCM] FirebaseApp initialised");
    //         }
    //     } catch (Exception e) {
    //         log.error("[FCM] Failed to initialise Firebase: {}", e.getMessage(), e);
    //     }
    // }

    // ─────────────────────────────────────────────────────────────────────────
    // DEVICE TOKEN MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    /** Register or update an FCM token for a user. Upserts by token value. */
    @Transactional
    public void registerToken(String userId, String fcmToken, String platform) {
        Optional<DeviceToken> existing = deviceTokenRepository.findByFcmToken(fcmToken);
        DeviceToken dt = existing.orElse(new DeviceToken());
        dt.setUserId(userId);
        dt.setFcmToken(fcmToken);
        dt.setPlatform(platform != null ? platform.toUpperCase() : "ANDROID");
        dt.setUpdatedAt(LocalDateTime.now());
        deviceTokenRepository.save(dt);
        log.debug("[FCM] Token registered for userId={} platform={}", userId, dt.getPlatform());
    }

    /** Remove a specific token (called on logout). */
    @Transactional
    public void removeToken(String fcmToken) {
        deviceTokenRepository.deleteByFcmToken(fcmToken);
        log.debug("[FCM] Token removed");
    }

    /** Remove all tokens for a user (called on account deletion). */
    @Transactional
    public void removeAllTokens(String userId) {
        deviceTokenRepository.deleteByUserId(userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEND PUSH
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Send a push notification to all devices registered for a userId.
     *
     * @param userId   recipient
     * @param titleEn  notification title (English)
     * @param bodyEn   notification body (English)
     * @param refType  deep-link type, e.g. "SUBSCRIPTION" / "PAYMENT" (may be null)
     * @param refId    deep-link id, e.g. "SB3A9BKD01" (may be null)
     */
    public void sendToUser(String userId, String titleEn, String bodyEn,
                           String refType, String refId) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("[FCM] No device tokens for userId={} — skipping push", userId);
            return;
        }

        if (!fcmEnabled) {
            log.debug("[FCM][LOCAL] Would push to userId={} title='{}' body='{}' ref={}/{}",
                    userId, titleEn, bodyEn, refType, refId);
            return;
        }

        // FCM — uncomment this block for production:
        //
        // List<String> tokenStrings = tokens.stream()
        //         .map(DeviceToken::getFcmToken)
        //         .collect(java.util.stream.Collectors.toList());
        //
        // MulticastMessage message = MulticastMessage.builder()
        //         .setNotification(Notification.builder()
        //                 .setTitle(titleEn)
        //                 .setBody(bodyEn)
        //                 .build())
        //         .putData("refType", refType != null ? refType : "")
        //         .putData("refId",   refId   != null ? refId   : "")
        //         .addAllTokens(tokenStrings)
        //         .build();
        //
        // try {
        //     var response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
        //     log.info("[FCM] Sent to {} tokens, {} success, {} fail",
        //             tokenStrings.size(), response.getSuccessCount(), response.getFailureCount());
        //
        //     // Auto-clean stale tokens
        //     for (int i = 0; i < response.getResponses().size(); i++) {
        //         if (!response.getResponses().get(i).isSuccessful()) {
        //             String staleToken = tokenStrings.get(i);
        //             log.warn("[FCM] Removing stale token for userId={}", userId);
        //             deviceTokenRepository.deleteByFcmToken(staleToken);
        //         }
        //     }
        // } catch (FirebaseMessagingException e) {
        //     log.error("[FCM] Multicast send failed: {}", e.getMessage(), e);
        // }
    }
}
