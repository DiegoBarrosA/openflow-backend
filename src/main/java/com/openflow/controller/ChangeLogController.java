package com.openflow.controller;

import com.openflow.dto.ChangeLogDto;
import com.openflow.service.ChangeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class ChangeLogController {

    @Autowired
    private ChangeLogService changeLogService;

    /**
     * Get change history for a specific task
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<List<ChangeLogDto>> getTaskHistory(@PathVariable Long taskId) {
        List<ChangeLogDto> history = changeLogService.getTaskHistory(taskId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get change history for a specific board
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/boards/{boardId}")
    public ResponseEntity<List<ChangeLogDto>> getBoardHistory(@PathVariable Long boardId) {
        List<ChangeLogDto> history = changeLogService.getBoardHistory(boardId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get change history for any entity type
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<ChangeLogDto>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        List<ChangeLogDto> history = changeLogService.getEntityHistory(entityType.toUpperCase(), entityId);
        return ResponseEntity.ok(history);
    }
}

