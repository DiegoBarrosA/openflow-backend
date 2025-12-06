package com.openflow.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String type; // TASK_CREATED, TASK_UPDATED, TASK_DELETED, BOARD_UPDATED, etc.

    @Column(length = 500)
    private String message;

    @Column(name = "reference_type", length = 20)
    private String referenceType; // TASK, BOARD, STATUS

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
    }

    // Notification types
    public static final String TYPE_TASK_CREATED = "TASK_CREATED";
    public static final String TYPE_TASK_UPDATED = "TASK_UPDATED";
    public static final String TYPE_TASK_DELETED = "TASK_DELETED";
    public static final String TYPE_TASK_MOVED = "TASK_MOVED";
    public static final String TYPE_BOARD_UPDATED = "BOARD_UPDATED";
    public static final String TYPE_STATUS_CREATED = "STATUS_CREATED";
    public static final String TYPE_STATUS_UPDATED = "STATUS_UPDATED";
    public static final String TYPE_STATUS_DELETED = "STATUS_DELETED";
}

