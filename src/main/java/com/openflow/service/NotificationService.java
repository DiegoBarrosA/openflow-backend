package com.openflow.service;

import com.openflow.dto.AlertSubscriptionDto;
import com.openflow.dto.NotificationDto;
import com.openflow.model.AlertSubscription;
import com.openflow.model.Notification;
import com.openflow.model.User;
import com.openflow.repository.AlertSubscriptionRepository;
import com.openflow.repository.NotificationRepository;
import com.openflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AlertSubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private EmailService emailService;

    // ==================== Notification Methods ====================

    /**
     * Create and send notifications for an entity change.
     */
    @Transactional
    public void notifyEntityChange(String entityType, Long entityId, String notificationType, 
                                    String message, Long excludeUserId) {
        // Find all subscriptions for this entity
        List<AlertSubscription> subscriptions = subscriptionRepository
                .findByEntityTypeAndEntityId(entityType, entityId);

        for (AlertSubscription sub : subscriptions) {
            // Don't notify the user who made the change
            if (sub.getUserId().equals(excludeUserId)) {
                continue;
            }

            // Create in-app notification if enabled
            if (Boolean.TRUE.equals(sub.getInAppEnabled())) {
                createNotification(sub.getUserId(), notificationType, message, entityType, entityId);
            }

            // Send email notification if enabled
            if (Boolean.TRUE.equals(sub.getEmailEnabled())) {
                sendEmailNotification(sub.getUserId(), notificationType, message, entityType, entityId);
            }
        }
    }

    /**
     * Create an in-app notification.
     */
    public Notification createNotification(Long userId, String type, String message, 
                                            String referenceType, Long referenceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setIsRead(false);

        logger.info("Creating notification for user {}: {} - {}", userId, type, message);
        return notificationRepository.save(notification);
    }

    /**
     * Send an email notification.
     */
    private void sendEmailNotification(Long userId, String type, String message,
                                        String referenceType, Long referenceId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getEmail() != null) {
                emailService.sendNotificationEmail(user.getEmail(), type, message, referenceType, referenceId);
            }
        } catch (Exception e) {
            logger.error("Failed to send email notification to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Get notifications for a user.
     */
    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toNotificationDto)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notifications for a user.
     */
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toNotificationDto)
                .collect(Collectors.toList());
    }

    /**
     * Get unread count for a user.
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to notification");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    // ==================== Subscription Methods ====================

    /**
     * Subscribe to an entity.
     */
    @Transactional
    public AlertSubscriptionDto subscribe(Long userId, String entityType, Long entityId,
                                           Boolean emailEnabled, Boolean inAppEnabled) {
        // Check if already subscribed
        if (subscriptionRepository.existsByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId)) {
            // Update existing subscription
            AlertSubscription existing = subscriptionRepository
                    .findByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId)
                    .get();
            existing.setEmailEnabled(emailEnabled != null ? emailEnabled : existing.getEmailEnabled());
            existing.setInAppEnabled(inAppEnabled != null ? inAppEnabled : existing.getInAppEnabled());
            return toSubscriptionDto(subscriptionRepository.save(existing));
        }

        AlertSubscription subscription = new AlertSubscription();
        subscription.setUserId(userId);
        subscription.setEntityType(entityType);
        subscription.setEntityId(entityId);
        subscription.setEmailEnabled(emailEnabled != null ? emailEnabled : true);
        subscription.setInAppEnabled(inAppEnabled != null ? inAppEnabled : true);

        logger.info("User {} subscribed to {} #{}", userId, entityType, entityId);
        return toSubscriptionDto(subscriptionRepository.save(subscription));
    }

    /**
     * Unsubscribe from an entity.
     */
    @Transactional
    public void unsubscribe(Long userId, String entityType, Long entityId) {
        subscriptionRepository.deleteByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId);
        logger.info("User {} unsubscribed from {} #{}", userId, entityType, entityId);
    }

    /**
     * Check if user is subscribed to an entity.
     */
    public boolean isSubscribed(Long userId, String entityType, Long entityId) {
        return subscriptionRepository.existsByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId);
    }

    /**
     * Get subscription for an entity (if exists).
     */
    public AlertSubscriptionDto getSubscription(Long userId, String entityType, Long entityId) {
        return subscriptionRepository.findByUserIdAndEntityTypeAndEntityId(userId, entityType, entityId)
                .map(this::toSubscriptionDto)
                .orElse(null);
    }

    /**
     * Get all subscriptions for a user.
     */
    public List<AlertSubscriptionDto> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .stream()
                .map(this::toSubscriptionDto)
                .collect(Collectors.toList());
    }

    // ==================== Conversion Methods ====================

    private NotificationDto toNotificationDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setType(notification.getType());
        dto.setMessage(notification.getMessage());
        dto.setReferenceType(notification.getReferenceType());
        dto.setReferenceId(notification.getReferenceId());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }

    private AlertSubscriptionDto toSubscriptionDto(AlertSubscription subscription) {
        AlertSubscriptionDto dto = new AlertSubscriptionDto();
        dto.setId(subscription.getId());
        dto.setUserId(subscription.getUserId());
        dto.setEntityType(subscription.getEntityType());
        dto.setEntityId(subscription.getEntityId());
        dto.setEmailEnabled(subscription.getEmailEnabled());
        dto.setInAppEnabled(subscription.getInAppEnabled());
        dto.setIsSubscribed(true);
        return dto;
    }
}

