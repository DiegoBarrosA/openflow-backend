package com.openflow.controller;

import com.openflow.model.Status;
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
    public ResponseEntity<List<Status>> getStatusesByBoard(@PathVariable Long boardId, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<Status> statuses = statusService.getStatusesByBoardId(boardId, userId);
            return ResponseEntity.ok(statuses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Status> getStatus(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            Status status = statusService.getStatusById(id, userId);
            return ResponseEntity.ok(status);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Status> createStatus(@Valid @RequestBody Status status, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            Status createdStatus = statusService.createStatus(status, userId);
            return ResponseEntity.ok(createdStatus);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Status> updateStatus(@PathVariable Long id, @Valid @RequestBody Status status, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            Status updatedStatus = statusService.updateStatus(id, status, userId);
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

