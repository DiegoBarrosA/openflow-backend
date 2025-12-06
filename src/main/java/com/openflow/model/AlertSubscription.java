package com.openflow.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_subscriptions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entity_type", "entity_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType; // TASK, BOARD

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    private Boolean inAppEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (emailEnabled == null) emailEnabled = true;
        if (inAppEnabled == null) inAppEnabled = true;
    }

    // Entity types
    public static final String ENTITY_TASK = "TASK";
    public static final String ENTITY_BOARD = "BOARD";
}

