package com.openflow.controller;

import com.openflow.dto.BoardAccessDto;
import com.openflow.model.AccessLevel;
import com.openflow.service.BoardAccessService;
import com.openflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Board access management endpoints.
 * All endpoints require board owner or ADMIN access to the board.
 */
@RestController
@RequestMapping("/api/boards/{boardId}/access")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class BoardAccessController {
    @Autowired
    private BoardAccessService boardAccessService;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username).getId();
    }

    /**
     * Get all users with access to a board.
     * Available to board owner or users with ADMIN access.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<BoardAccessDto>> getBoardAccess(@PathVariable Long boardId, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<BoardAccessDto> accesses = boardAccessService.getBoardAccesses(boardId, userId);
            return ResponseEntity.ok(accesses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Grant access to a user.
     * Available to board owner or users with ADMIN access.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<BoardAccessDto> grantAccess(
            @PathVariable Long boardId,
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            Long targetUserId = Long.valueOf(request.get("userId").toString());
            AccessLevel level = AccessLevel.valueOf(request.get("accessLevel").toString());
            
            BoardAccessDto access = boardAccessService.grantAccess(boardId, targetUserId, level, userId);
            return ResponseEntity.ok(access);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update access level for a user.
     * Available to board owner or users with ADMIN access.
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<BoardAccessDto> updateAccess(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            @Valid @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            Long requesterId = getCurrentUserId(authentication);
            AccessLevel level = AccessLevel.valueOf(request.get("accessLevel").toString());
            
            BoardAccessDto access = boardAccessService.updateAccessLevel(boardId, userId, level, requesterId);
            return ResponseEntity.ok(access);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Revoke access from a user.
     * Available to board owner or users with ADMIN access.
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> revokeAccess(
            @PathVariable Long boardId,
            @PathVariable Long userId,
            Authentication authentication) {
        try {
            Long requesterId = getCurrentUserId(authentication);
            boardAccessService.revokeAccess(boardId, userId, requesterId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

