package com.openflow.controller;

import com.openflow.model.Task;
import com.openflow.dto.TaskDto;
import com.openflow.service.TaskService;
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
 * Task management endpoints.
 * All task operations are available to both ADMIN and USER roles.
 * Users can create, move, modify, and delete tasks.
 */
@Tag(name = "Tasks", description = "Task management operations")
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class TaskController {
    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username).getId();
    }

    /**
     * Get all tasks for a board.
     * Available to ADMIN and USER.
     */
    @Operation(summary = "Get tasks by board", description = "Retrieve all tasks for a specific board. Requires READ access to the board.")
    @ApiResponse(responseCode = "200", description = "List of tasks retrieved successfully")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<TaskDto>> getTasks(
            @Parameter(description = "Board ID", required = true) @RequestParam Long boardId, 
            Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            List<TaskDto> tasks = taskService.getTasksByBoardIdDto(boardId, userId);
            return ResponseEntity.ok(tasks);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get a specific task by ID.
     * Available to ADMIN and USER.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TaskDto> getTask(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            TaskDto task = taskService.getTaskByIdDto(id, userId);
            return ResponseEntity.ok(task);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a new task.
     * Available to ADMIN and USER.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody TaskDto taskDto, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            TaskDto createdTask = taskService.createTaskDto(taskDto, userId);
            return ResponseEntity.ok(createdTask);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing task (including moving to different status).
     * Available to ADMIN and USER.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TaskDto> updateTask(@PathVariable Long id, @Valid @RequestBody TaskDto taskDto, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            TaskDto updatedTask = taskService.updateTaskDto(id, taskDto, userId);
            return ResponseEntity.ok(updatedTask);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a task.
     * Available to ADMIN and USER.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            taskService.deleteTask(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

