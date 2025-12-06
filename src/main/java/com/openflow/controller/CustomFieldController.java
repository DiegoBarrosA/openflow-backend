package com.openflow.controller;

import com.openflow.dto.CustomFieldDefinitionDto;
import com.openflow.dto.CustomFieldValueDto;
import com.openflow.model.User;
import com.openflow.repository.UserRepository;
import com.openflow.service.CustomFieldService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/custom-fields")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class CustomFieldController {

    @Autowired
    private CustomFieldService customFieldService;

    @Autowired
    private UserRepository userRepository;

    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    // ==================== Field Definition Endpoints ====================

    /**
     * Get all custom field definitions for a board.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/definitions/board/{boardId}")
    public ResponseEntity<List<CustomFieldDefinitionDto>> getFieldDefinitions(
            @PathVariable Long boardId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        List<CustomFieldDefinitionDto> definitions = customFieldService.getFieldDefinitions(boardId, userId);
        return ResponseEntity.ok(definitions);
    }

    /**
     * Create a new custom field definition (Admin only).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/definitions")
    public ResponseEntity<CustomFieldDefinitionDto> createFieldDefinition(
            @Valid @RequestBody CustomFieldDefinitionDto dto,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CustomFieldDefinitionDto created = customFieldService.createFieldDefinition(dto, userId);
        return ResponseEntity.ok(created);
    }

    /**
     * Update a custom field definition (Admin only).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/definitions/{id}")
    public ResponseEntity<CustomFieldDefinitionDto> updateFieldDefinition(
            @PathVariable Long id,
            @Valid @RequestBody CustomFieldDefinitionDto dto,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        CustomFieldDefinitionDto updated = customFieldService.updateFieldDefinition(id, dto, userId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a custom field definition (Admin only).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/definitions/{id}")
    public ResponseEntity<Void> deleteFieldDefinition(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        customFieldService.deleteFieldDefinition(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Field Value Endpoints ====================

    /**
     * Get all custom field values for a task.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/values/task/{taskId}")
    public ResponseEntity<List<CustomFieldValueDto>> getTaskFieldValues(@PathVariable Long taskId) {
        List<CustomFieldValueDto> values = customFieldService.getTaskFieldValues(taskId);
        return ResponseEntity.ok(values);
    }

    /**
     * Set a single custom field value for a task.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping("/values/task/{taskId}/field/{fieldDefinitionId}")
    public ResponseEntity<CustomFieldValueDto> setFieldValue(
            @PathVariable Long taskId,
            @PathVariable Long fieldDefinitionId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        String value = body.get("value");
        CustomFieldValueDto saved = customFieldService.setFieldValue(taskId, fieldDefinitionId, value, userId);
        return ResponseEntity.ok(saved);
    }

    /**
     * Set multiple custom field values for a task.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping("/values/task/{taskId}")
    public ResponseEntity<List<CustomFieldValueDto>> setFieldValues(
            @PathVariable Long taskId,
            @RequestBody Map<Long, String> fieldValues,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        List<CustomFieldValueDto> saved = customFieldService.setFieldValues(taskId, fieldValues, userId);
        return ResponseEntity.ok(saved);
    }
}

