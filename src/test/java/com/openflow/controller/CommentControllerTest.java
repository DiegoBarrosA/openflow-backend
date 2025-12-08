package com.openflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openflow.dto.CommentDto;
import com.openflow.model.Role;
import com.openflow.model.User;
import com.openflow.repository.UserRepository;
import com.openflow.service.CommentService;
import com.openflow.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @MockBean
    private com.openflow.service.UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private CommentDto testCommentDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);

        testCommentDto = new CommentDto();
        testCommentDto.setId(1L);
        testCommentDto.setTaskId(1L);
        testCommentDto.setUserId(1L);
        testCommentDto.setContent("Test comment");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetCommentsByTask() throws Exception {
        // Arrange
        Long taskId = 1L;
        List<CommentDto> comments = Arrays.asList(testCommentDto);
        when(userService.findByUsername(any())).thenReturn(testUser);
        when(commentService.getCommentsByTaskId(taskId, 1L)).thenReturn(comments);

        // Act & Assert
        mockMvc.perform(get("/api/comments/task/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Test comment"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testCreateComment() throws Exception {
        // Arrange
        CommentDto newComment = new CommentDto();
        newComment.setTaskId(1L);
        newComment.setContent("New comment");

        when(userService.findByUsername(any())).thenReturn(testUser);
        when(commentService.createComment(any(CommentDto.class), anyLong())).thenReturn(testCommentDto);

        // Act & Assert
        mockMvc.perform(post("/api/comments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Test comment"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testDeleteComment() throws Exception {
        // Arrange
        Long commentId = 1L;
        when(userService.findByUsername(any())).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(delete("/api/comments/{id}", commentId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}

