package com.openflow.controller;

import com.openflow.model.Attachment;
import com.openflow.service.AttachmentService;
import com.openflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Attachment management endpoints.
 * Allows users to upload, download, and delete file attachments for tasks.
 */
@Tag(name = "Attachments", description = "File attachment management for tasks")
@RestController
@RequestMapping("/api/attachments")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AttachmentController {

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username).getId();
    }

    /**
     * Check if S3 storage is enabled.
     */
    @Operation(summary = "Check S3 status", description = "Check if file storage is enabled")
    @ApiResponse(responseCode = "200", description = "S3 status retrieved")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        return ResponseEntity.ok(Map.of("enabled", attachmentService.isS3Enabled()));
    }

    /**
     * Get all attachments for a task.
     */
    @Operation(summary = "Get attachments for task", description = "Retrieve all attachments for a specific task")
    @ApiResponse(responseCode = "200", description = "List of attachments retrieved")
    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Attachment>> getAttachmentsByTask(
            @Parameter(description = "Task ID") @PathVariable Long taskId) {
        List<Attachment> attachments = attachmentService.getAttachmentsByTaskId(taskId);
        return ResponseEntity.ok(attachments);
    }

    /**
     * Upload a file attachment to a task.
     */
    @Operation(summary = "Upload attachment", description = "Upload a file attachment to a task")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid file or S3 not enabled"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadAttachment(
            @Parameter(description = "Task ID") @PathVariable Long taskId,
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            
            Long userId = getCurrentUserId(authentication);
            Attachment attachment = attachmentService.uploadAttachment(taskId, userId, file);
            return ResponseEntity.ok(attachment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file"));
        }
    }

    /**
     * Get a download URL for an attachment.
     */
    @Operation(summary = "Get download URL", description = "Get a presigned URL to download an attachment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Download URL generated"),
        @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    @GetMapping("/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> getDownloadUrl(
            @Parameter(description = "Attachment ID") @PathVariable Long attachmentId) {
        try {
            String url = attachmentService.getDownloadUrl(attachmentId);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete an attachment.
     */
    @Operation(summary = "Delete attachment", description = "Delete an attachment from a task")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Attachment deleted"),
        @ApiResponse(responseCode = "404", description = "Attachment not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteAttachment(
            @Parameter(description = "Attachment ID") @PathVariable Long attachmentId) {
        try {
            attachmentService.deleteAttachment(attachmentId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

