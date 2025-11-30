package com.openflow.controller;

import com.openflow.model.Status;
import com.openflow.dto.StatusDto;
import com.openflow.service.StatusService;
import com.openflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    public ResponseEntity<StatusDto> createStatus(@Valid @RequestBody StatusDto statusDto, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            StatusDto createdStatus = statusService.createStatusDto(statusDto, userId);
            return ResponseEntity.ok(createdStatus);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<StatusDto> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDto statusDto, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            StatusDto updatedStatus = statusService.updateStatusDto(id, statusDto, userId);
            return ResponseEntity.ok(updatedStatus);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatus(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            statusService.deleteStatus(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

