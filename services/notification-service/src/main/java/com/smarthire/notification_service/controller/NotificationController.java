package com.smarthire.notification_service.controller;

import com.smarthire.notification_service.entity.Notification;
import com.smarthire.notification_service.service.NotificationService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── GET /api/notifications — all notifications ────────────────────────
    @GetMapping
    public ResponseEntity<List<Notification>> getAll(Authentication auth) {
        return ResponseEntity.ok(notificationService.getNotifications(extractUserId(auth)));
    }

    // ── GET /api/notifications/unread ─────────────────────────────────────
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnread(Authentication auth) {
        return ResponseEntity.ok(notificationService.getUnread(extractUserId(auth)));
    }

    // ── GET /api/notifications/unread/count ───────────────────────────────
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> countUnread(Authentication auth) {
        long count = notificationService.countUnread(extractUserId(auth));
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ── PATCH /api/notifications/{id}/read ────────────────────────────────
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    // ── PATCH /api/notifications/read-all ────────────────────────────────
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        notificationService.markAllAsRead(extractUserId(auth));
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }
}
