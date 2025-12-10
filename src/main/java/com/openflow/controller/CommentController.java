package com.openflow.controller;

import com.openflow.dto.CommentDto;
import com.openflow.model.User;
import com.openflow.service.CommentService;
import com.openflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Comment management endpoints.
 * All comment operations are available to both ADMIN and USER roles.
 * Users can create, edit, and delete their own comments.
 * Admins can delete any comment.
 */
@Tag(name = "Comments", description = "Task comment management operations")
@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Authentication is required");
        }
        String username = authentication.getName();
        if (username == null) {
            throw new RuntimeException("Username not found in authentication");
        }
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }
        return user.getId();
    }

    /**
     * Get all comments for a task.
     * Available to ADMIN and USER.
     */
    @Operation(summary = "Get task comments", description = "Retrieve all comments for a specific task. Requires access to the task's board.")
    @ApiResponse(responseCode = "200", description = "List of comments retrieved successfully")
    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<CommentDto>> getCommentsByTask(
            @Parameter(description = "Task ID", required = true) @PathVariable Long taskId, 
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<CommentDto> comments = commentService.getCommentsByTaskId(taskId, userId);
            return ResponseEntity.ok(comments);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create a new comment.
     * Available to ADMIN and USER.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<CommentDto> createComment(@Valid @RequestBody CommentDto commentDto, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            CommentDto createdComment = commentService.createComment(commentDto, userId);
            return ResponseEntity.ok(createdComment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing comment.
     * Available to ADMIN and USER (only own comments).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<CommentDto> updateComment(@PathVariable Long id, @Valid @RequestBody CommentDto commentDto, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            CommentDto updatedComment = commentService.updateComment(id, commentDto, userId);
            return ResponseEntity.ok(updatedComment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a comment.
     * Available to ADMIN and USER (only own comments, or admin can delete any).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            commentService.deleteComment(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

