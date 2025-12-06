package com.openflow.controller;

import com.openflow.model.Board;
import com.openflow.dto.BoardDto;
import com.openflow.service.BoardService;
import com.openflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Board management endpoints.
 * - GET endpoints: All authenticated users (ADMIN and USER)
 * - POST/PUT/DELETE endpoints: ADMIN only
 */
@RestController
@RequestMapping("/api/boards")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class BoardController {
    @Autowired
    private BoardService boardService;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username).getId();
    }

    /**
     * Get all boards for the current user.
     * Available to all authenticated users.
     */
    @GetMapping
    public ResponseEntity<List<BoardDto>> getAllBoards(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<BoardDto> boards = boardService.getAllBoardsByUserIdDto(userId);
        return ResponseEntity.ok(boards);
    }

    /**
     * Get a specific board by ID.
     * Available to all authenticated users.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardDto> getBoard(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            BoardDto board = boardService.getBoardByIdDto(id, userId);
            return ResponseEntity.ok(board);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a new board.
     * ADMIN only.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BoardDto> createBoard(@Valid @RequestBody BoardDto boardDto, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            BoardDto createdBoard = boardService.createBoardDto(boardDto, userId);
            return ResponseEntity.ok(createdBoard);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing board.
     * ADMIN only.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BoardDto> updateBoard(@PathVariable Long id, @Valid @RequestBody BoardDto boardDto, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            BoardDto updatedBoard = boardService.updateBoardDto(id, boardDto, userId);
            return ResponseEntity.ok(updatedBoard);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a board.
     * ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            boardService.deleteBoard(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all templates for the current user.
     * ADMIN only.
     */
    @GetMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BoardDto>> getTemplates(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<BoardDto> templates = boardService.getTemplatesDto(userId);
        return ResponseEntity.ok(templates);
    }

    /**
     * Create a board from a template.
     * ADMIN only.
     */
    @PostMapping("/from-template/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BoardDto> createBoardFromTemplate(
            @PathVariable Long templateId,
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            String newBoardName = body.get("name");
            if (newBoardName == null || newBoardName.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            BoardDto newBoard = boardService.createBoardFromTemplate(templateId, newBoardName, userId);
            return ResponseEntity.ok(newBoard);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

