package com.openflow.service;

import com.openflow.dto.CommentDto;
import com.openflow.model.Comment;
import com.openflow.model.Role;
import com.openflow.model.User;
import com.openflow.repository.CommentRepository;
import com.openflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskService taskService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChangeLogService changeLogService;

    @InjectMocks
    private CommentService commentService;

    private User testUser;
    private Comment testComment;
    private CommentDto testCommentDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        testComment = new Comment();
        testComment.setId(1L);
        testComment.setTaskId(1L);
        testComment.setUserId(1L);
        testComment.setContent("Test comment");
        testComment.setCreatedAt(LocalDateTime.now());
        testComment.setUpdatedAt(LocalDateTime.now());

        testCommentDto = new CommentDto();
        testCommentDto.setTaskId(1L);
        testCommentDto.setContent("Test comment");
    }

    @Test
    void testGetCommentsByTaskId() {
        // Arrange
        Long taskId = 1L;
        Long userId = 1L;
        List<Comment> comments = Arrays.asList(testComment);

        when(taskService.getTaskById(taskId, userId)).thenReturn(null); // Mock validation
        when(commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)).thenReturn(comments);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        List<CommentDto> result = commentService.getCommentsByTaskId(taskId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test comment", result.get(0).getContent());
        verify(taskService).getTaskById(taskId, userId);
        verify(commentRepository).findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    @Test
    void testCreateComment() {
        // Arrange
        Long userId = 1L;
        when(taskService.getTaskById(any(), any())).thenReturn(null); // Mock validation
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        CommentDto result = commentService.createComment(testCommentDto, userId);

        // Assert
        assertNotNull(result);
        assertEquals("Test comment", result.getContent());
        verify(commentRepository).save(any(Comment.class));
        verify(changeLogService).logCreate(anyString(), anyLong(), anyLong());
    }

    @Test
    void testUpdateComment_AsAuthor() {
        // Arrange
        Long commentId = 1L;
        Long userId = 1L;
        testCommentDto.setContent("Updated comment");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
        when(taskService.getTaskById(any(), any())).thenReturn(null); // Mock validation
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // Act
        CommentDto result = commentService.updateComment(commentId, testCommentDto, userId);

        // Assert
        assertNotNull(result);
        verify(commentRepository).save(any(Comment.class));
        verify(changeLogService).logFieldChange(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void testDeleteComment_AsAuthor() {
        // Arrange
        Long commentId = 1L;
        Long userId = 1L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(testComment));
        when(taskService.getTaskById(any(), any())).thenReturn(null); // Mock validation
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        commentService.deleteComment(commentId, userId);

        // Assert
        verify(commentRepository).delete(any(Comment.class));
        verify(changeLogService).logDelete(anyString(), anyLong(), anyLong());
    }
}

