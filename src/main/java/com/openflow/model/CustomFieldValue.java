package com.openflow.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "custom_field_values", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "field_definition_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "field_definition_id", nullable = false)
    private Long fieldDefinitionId;

    @Column(name = "`VALUE`", length = 1000)
    private String value; // Stored as string, parsed based on field type

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

