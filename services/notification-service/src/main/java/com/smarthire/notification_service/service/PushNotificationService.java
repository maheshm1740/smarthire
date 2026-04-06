package com.smarthire.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Push notification service.
 * Currently logs push notifications — wire Firebase FCM or any push provider here.
 * To enable real push: add firebase-admin dependency + FCM token storage per user.
 */
@Slf4j
@Service
public class PushNotificationService {

    public boolean sendPush(UUID userId, String title, String message, String referenceId) {
        // TODO: integrate Firebase FCM
        // FirebaseMessaging.getInstance().send(Message.builder()
        //     .setToken(getUserFcmToken(userId))
        //     .setNotification(Notification.builder().setTitle(title).setBody(message).build())
        //     .putData("referenceId", referenceId)
        //     .build());

        log.info("[PUSH] userId={} title='{}' message='{}'", userId, title, message);
        // Return true to indicate push was processed (log-only for now)
        return true;
    }
}
