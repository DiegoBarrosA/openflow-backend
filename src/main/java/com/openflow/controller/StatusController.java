package com.openflow.controller;

import com.openflow.model.Status;
import com.openflow.dto.StatusDto;
import com.openflow.service.StatusService;
import com.openflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Status (column) management endpoints.
 * - GET endpoints: All authenticated users (ADMIN and USER)
 * - POST/PUT/DELETE endpoints: ADMIN only (column management)
 */
@RestController
@RequestMapping("/api/statuses")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class StatusController {
    @Autowired
    private StatusService statusService;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username).getId();
    }

    /**
     * Get all statuses for a board.
     * Available to all authenticated users.
     */
    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<StatusDto>> getStatusesByBoard(@PathVariable Long boardId, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<StatusDto> statuses = statusService.getStatusesByBoardIdDto(boardId, userId);
            return ResponseEntity.ok(statuses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get a specific status by ID.
     * Available to all authenticated users.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StatusDto> getStatus(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            StatusDto status = statusService.getStatusByIdDto(id, userId);
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a new status (column).
     * ADMIN only.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatusDto> createStatus(@Valid @RequestBody StatusDto statusDto, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            StatusDto createdStatus = statusService.createStatusDto(statusDto, userId);
            return ResponseEntity.ok(createdStatus);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing status (column).
     * ADMIN only.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatusDto> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDto statusDto, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            StatusDto updatedStatus = statusService.updateStatusDto(id, statusDto, userId);
            return ResponseEntity.ok(updatedStatus);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a status (column).
     * ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStatus(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            statusService.deleteStatus(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Reorder statuses for a board.
     * ADMIN only.
     */
    @PutMapping("/board/{boardId}/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StatusDto>> reorderStatuses(
            @PathVariable Long boardId,
            @RequestBody List<Long> statusIds,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<StatusDto> reordered = statusService.reorderStatuses(boardId, statusIds, userId);
            return ResponseEntity.ok(reordered);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

