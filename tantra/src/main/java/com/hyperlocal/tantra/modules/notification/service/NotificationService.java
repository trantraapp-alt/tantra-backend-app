package com.hyperlocal.tantra.modules.notification.service;

import com.hyperlocal.tantra.constants.MessageConstants;
import com.hyperlocal.tantra.exception.LocalizedException;
import com.hyperlocal.tantra.modules.auth.entity.User;
import com.hyperlocal.tantra.modules.auth.repository.UserRepository;
import com.hyperlocal.tantra.modules.forms.model.LocalizedText;
import com.hyperlocal.tantra.modules.notification.entity.Notification;
import com.hyperlocal.tantra.modules.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    /** Create an in-app notification for a user (by their userId). */
    public void push(String userId, String type, LocalizedText title, LocalizedText body,
                     String refType, String refId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRefType(refType);
        notification.setRefId(refId);
        notificationRepository.save(notification);
    }

    // ---- reads (controller, by the caller's mobile) ----

    public Page<Notification> list(String mobileNumber, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId(mobileNumber), pageable);
    }

    public long unreadCount(String mobileNumber) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId(mobileNumber));
    }

    @Transactional
    public void markRead(Long id, String mobileNumber) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId(mobileNumber))
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.NOTIFICATION_NOT_FOUND_EN, MessageConstants.NOTIFICATION_NOT_FOUND_HI));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    private String userId(String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new LocalizedException(
                        MessageConstants.LISTING_USER_NOT_FOUND_EN, MessageConstants.LISTING_USER_NOT_FOUND_HI));
        return user.getUserId();
    }
}
