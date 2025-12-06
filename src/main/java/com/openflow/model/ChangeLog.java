package com.openflow.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "change_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType; // TASK, BOARD, STATUS

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 20)
    private String action; // CREATE, UPDATE, DELETE, MOVE

    @Column(name = "field_name", length = 100)
    private String fieldName; // Which field changed

    @Column(name = "old_value", length = 1000)
    private String oldValue;

    @Column(name = "new_value", length = 1000)
    private String newValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Builder-style static factory methods for convenience
    public static ChangeLog create(String entityType, Long entityId, Long userId, String action) {
        ChangeLog log = new ChangeLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setUserId(userId);
        log.setAction(action);
        return log;
    }

    public static ChangeLog fieldChange(String entityType, Long entityId, Long userId, 
                                         String fieldName, String oldValue, String newValue) {
        ChangeLog log = create(entityType, entityId, userId, "UPDATE");
        log.setFieldName(fieldName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        return log;
    }
}

