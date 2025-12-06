package com.openflow.controller;

import com.openflow.dto.BoardDto;
import com.openflow.dto.StatusDto;
import com.openflow.dto.TaskDto;
import com.openflow.service.BoardService;
import com.openflow.service.StatusService;
import com.openflow.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public endpoints for anonymous access to public boards.
 * No authentication required for these endpoints.
 */
@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class PublicBoardController {
    
    @Autowired
    private BoardService boardService;
    
    @Autowired
    private StatusService statusService;
    
    @Autowired
    private TaskService taskService;

    /**
     * Get all public boards (no authentication required).
     */
    @GetMapping("/boards")
    public ResponseEntity<List<BoardDto>> getPublicBoards() {
        List<BoardDto> boards = boardService.getAllPublicBoardsDto();
        return ResponseEntity.ok(boards);
    }

    /**
     * Get a specific public board by ID (no authentication required).
     */
    @GetMapping("/boards/{id}")
    public ResponseEntity<BoardDto> getPublicBoard(@PathVariable Long id) {
        try {
            BoardDto board = boardService.getPublicBoardByIdDto(id);
            return ResponseEntity.ok(board);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get statuses for a public board (no authentication required).
     */
    @GetMapping("/boards/{boardId}/statuses")
    public ResponseEntity<List<StatusDto>> getPublicBoardStatuses(@PathVariable Long boardId) {
        try {
            // Verify board is public
            boardService.getPublicBoardById(boardId);
            List<StatusDto> statuses = statusService.getStatusesByBoardIdDtoPublic(boardId);
            return ResponseEntity.ok(statuses);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get tasks for a public board (no authentication required).
     */
    @GetMapping("/boards/{boardId}/tasks")
    public ResponseEntity<List<TaskDto>> getPublicBoardTasks(@PathVariable Long boardId) {
        try {
            // Verify board is public
            boardService.getPublicBoardById(boardId);
            List<TaskDto> tasks = taskService.getTasksByBoardIdDtoPublic(boardId);
            return ResponseEntity.ok(tasks);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

