package com.openflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "custom_field_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @NotBlank
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 20)
    private FieldType fieldType;

    @Column(columnDefinition = "CLOB")
    private String options; // JSON array for dropdown options: ["Option 1", "Option 2"]

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "show_in_card")
    private Boolean showInCard = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }
}

