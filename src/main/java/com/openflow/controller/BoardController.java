package com.openflow.controller;

import com.openflow.model.Board;
import com.openflow.service.BoardService;
import com.openflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<Board>> getAllBoards(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(boardService.getAllBoardsByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Board> getBoard(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            Board board = boardService.getBoardById(id, userId);
            return ResponseEntity.ok(board);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Board> createBoard(@Valid @RequestBody Board board, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            Board createdBoard = boardService.createBoard(board, userId);
            return ResponseEntity.ok(createdBoard);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Board> updateBoard(@PathVariable Long id, @Valid @RequestBody Board board, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            Board updatedBoard = boardService.updateBoard(id, board, userId);
            return ResponseEntity.ok(updatedBoard);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            boardService.deleteBoard(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

