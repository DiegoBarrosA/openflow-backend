package com.openflow.controller;

import com.openflow.dto.AlertSubscriptionDto;
import com.openflow.model.User;
import com.openflow.repository.UserRepository;
import com.openflow.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AlertSubscriptionController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    /**
     * Get all subscriptions for the current user.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public ResponseEntity<List<AlertSubscriptionDto>> getUserSubscriptions(Authentication authentication) {
        Long userId = getUserId(authentication);
        List<AlertSubscriptionDto> subscriptions = notificationService.getUserSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Check if user is subscribed to an entity.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<Map<String, Object>> checkSubscription(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        boolean isSubscribed = notificationService.isSubscribed(userId, entityType.toUpperCase(), entityId);
        AlertSubscriptionDto subscription = notificationService.getSubscription(userId, entityType.toUpperCase(), entityId);
        
        return ResponseEntity.ok(Map.of(
            "isSubscribed", isSubscribed,
            "subscription", subscription != null ? subscription : Map.of()
        ));
    }

    /**
     * Subscribe to an entity.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/{entityType}/{entityId}")
    public ResponseEntity<AlertSubscriptionDto> subscribe(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestBody(required = false) Map<String, Boolean> options,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        Boolean emailEnabled = options != null ? options.get("emailEnabled") : true;
        Boolean inAppEnabled = options != null ? options.get("inAppEnabled") : true;
        
        AlertSubscriptionDto subscription = notificationService.subscribe(
            userId, entityType.toUpperCase(), entityId, emailEnabled, inAppEnabled);
        return ResponseEntity.ok(subscription);
    }

    /**
     * Unsubscribe from an entity.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @DeleteMapping("/{entityType}/{entityId}")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        notificationService.unsubscribe(userId, entityType.toUpperCase(), entityId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update subscription preferences.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping("/{entityType}/{entityId}")
    public ResponseEntity<AlertSubscriptionDto> updateSubscription(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestBody Map<String, Boolean> options,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        Boolean emailEnabled = options.get("emailEnabled");
        Boolean inAppEnabled = options.get("inAppEnabled");
        
        AlertSubscriptionDto subscription = notificationService.subscribe(
            userId, entityType.toUpperCase(), entityId, emailEnabled, inAppEnabled);
        return ResponseEntity.ok(subscription);
    }
}

