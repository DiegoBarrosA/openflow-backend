package com.openflow.service;

import com.openflow.dto.CommentDto;
import com.openflow.model.Comment;
import com.openflow.model.Role;
import com.openflow.model.User;
import com.openflow.repository.CommentRepository;
import com.openflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private ChangeLogService changeLogService;

    @Autowired
    private S3Service s3Service;


    private CommentDto toDto(Comment comment) {
        User user = userRepository.findById(comment.getUserId()).orElse(null);
        String username = user != null ? user.getUsername() : "Unknown";
        String profilePictureUrl = null;
        
        // Get profile picture URL if available
        if (user != null && user.getProfilePictureKey() != null && s3Service.isEnabled()) {
            try {
                profilePictureUrl = s3Service.getPresignedUrl(user.getProfilePictureKey());
            } catch (Exception e) {
                // Ignore - profile picture not available
            }
        }
        
        return new CommentDto(
            comment.getId(),
            comment.getTaskId(),
            comment.getUserId(),
            username,
            profilePictureUrl,
            comment.getContent(),
            comment.getCreatedAt(),
            comment.getUpdatedAt()
        );
    }

    private Comment toEntity(CommentDto dto) {
        Comment comment = new Comment();
        comment.setId(dto.getId());
        comment.setTaskId(dto.getTaskId());
        comment.setUserId(dto.getUserId());
        comment.setContent(dto.getContent());
        return comment;
    }

    /**
     * Get all comments for a task.
     * Validates that the user has access to the task's board.
     */
    public List<CommentDto> getCommentsByTaskId(Long taskId, Long userId) {
        // Validate user has access to the task's board
        taskService.getTaskById(taskId, userId);
        
        List<Comment> comments = commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        return comments.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Create a new comment.
     * Validates that the user has access to the task's board.
     */
    public CommentDto createComment(CommentDto dto, Long userId) {
        // Validate user has access to the task's board
        taskService.getTaskById(dto.getTaskId(), userId);
        
        Comment comment = toEntity(dto);
        comment.setUserId(userId);
        Comment saved = commentRepository.save(comment);
        
        // Log comment creation
        changeLogService.logCreate(ChangeLogService.ENTITY_TASK, dto.getTaskId(), userId);
        
        return toDto(saved);
    }

    /**
     * Update a comment.
     * Only the comment author or admin can update.
     */
    public CommentDto updateComment(Long id, CommentDto dto, Long userId) {
        Comment existingComment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        // Validate user has access to the task's board
        taskService.getTaskById(existingComment.getTaskId(), userId);
        
        // Check if user is the author or admin
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isAuthor = existingComment.getUserId().equals(userId);
        boolean isAdmin = user.getRole() == Role.ADMIN;
        
        if (!isAuthor && !isAdmin) {
            throw new RuntimeException("Unauthorized: Only comment author or admin can update");
        }
        
        existingComment.setContent(dto.getContent());
        Comment saved = commentRepository.save(existingComment);
        
        // Log comment update
        changeLogService.logFieldChange(ChangeLogService.ENTITY_TASK, existingComment.getTaskId(), userId,
            "comment", "updated", dto.getContent());
        
        return toDto(saved);
    }

    /**
     * Delete a comment.
     * Only the comment author or admin can delete.
     */
    public void deleteComment(Long id, Long userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        // Validate user has access to the task's board
        taskService.getTaskById(comment.getTaskId(), userId);
        
        // Check if user is the author or admin
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean isAuthor = comment.getUserId().equals(userId);
        boolean isAdmin = user.getRole() == Role.ADMIN;
        
        if (!isAuthor && !isAdmin) {
            throw new RuntimeException("Unauthorized: Only comment author or admin can delete");
        }
        
        // Log comment deletion
        changeLogService.logDelete(ChangeLogService.ENTITY_TASK, comment.getTaskId(), userId);
        
        commentRepository.delete(comment);
    }
}

