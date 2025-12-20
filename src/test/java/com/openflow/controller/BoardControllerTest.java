package com.openflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openflow.dto.BoardDto;
import com.openflow.model.Role;
import com.openflow.model.User;
import com.openflow.service.BoardService;
import com.openflow.service.JwtService;
import com.openflow.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller integration tests for BoardController.
 * Tests board CRUD operations with role-based access control.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private com.openflow.config.AzureAdAuthenticationFilter azureAdAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User regularUser;
    private BoardDto testBoardDto;

    @BeforeEach
    void setUp() {
        reset(boardService, userService);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Role.ADMIN);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("user");
        regularUser.setEmail("user@example.com");
        regularUser.setRole(Role.USER);

        testBoardDto = new BoardDto(1L, "Test Board", "Description", 1L, false, false);
    }

    /**
     * Test GET /api/boards - get all boards for authenticated user.
     */
    @Test
    void testGetAllBoards_Authenticated() throws Exception {
        // Arrange
        List<BoardDto> boards = Arrays.asList(testBoardDto);
        when(userService.findByUsername("admin")).thenReturn(adminUser);
        when(boardService.getAllBoardsByUserIdDto(1L)).thenReturn(boards);

        // Act & Assert
        mockMvc.perform(get("/api/boards")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Test Board"));
    }

    /**
     * Test GET /api/boards/{id} - get board by ID.
     */
    @Test
    void testGetBoardById_Success() throws Exception {
        // Arrange
        when(userService.findByUsername("admin")).thenReturn(adminUser);
        when(boardService.getBoardByIdDto(1L, 1L)).thenReturn(testBoardDto);

        // Act & Assert
        mockMvc.perform(get("/api/boards/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Board"));
    }

    /**
     * Test GET /api/boards/{id} - board not found.
     */
    @Test
    void testGetBoardById_NotFound() throws Exception {
        // Arrange
        when(userService.findByUsername("admin")).thenReturn(adminUser);
        when(boardService.getBoardByIdDto(999L, 1L)).thenThrow(new RuntimeException("Board not found"));

        // Act & Assert
        mockMvc.perform(get("/api/boards/999")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    /**
     * Test POST /api/boards - create board as ADMIN.
     */
    @Test
    void testCreateBoard_AsAdmin() throws Exception {
        // Arrange
        BoardDto newBoard = new BoardDto();
        newBoard.setName("New Board");
        newBoard.setDescription("New Description");

        BoardDto createdBoard = new BoardDto(2L, "New Board", "New Description", 1L, false, false);

        when(userService.findByUsername("admin")).thenReturn(adminUser);
        when(boardService.createBoardDto(any(BoardDto.class), eq(1L))).thenReturn(createdBoard);

        // Act & Assert
        mockMvc.perform(post("/api/boards")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBoard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("New Board"));
    }

    /**
     * Test POST /api/boards - create board as regular USER (forbidden).
     */
    @Test
    void testCreateBoard_AsUser_Forbidden() throws Exception {
        // Arrange
        BoardDto newBoard = new BoardDto();
        newBoard.setName("New Board");

        when(userService.findByUsername("user")).thenReturn(regularUser);

        // Act & Assert
        mockMvc.perform(post("/api/boards")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBoard)))
                .andExpect(status().isForbidden());
    }

    /**
     * Test PUT /api/boards/{id} - update board as ADMIN.
     */
    @Test
    void testUpdateBoard_AsAdmin() throws Exception {
        // Arrange
        BoardDto updateData = new BoardDto();
        updateData.setName("Updated Board");
        updateData.setDescription("Updated Description");

        BoardDto updatedBoard = new BoardDto(1L, "Updated Board", "Updated Description", 1L, false, false);

        when(userService.findByUsername("admin")).thenReturn(adminUser);
        when(boardService.updateBoardDto(eq(1L), any(BoardDto.class), eq(1L))).thenReturn(updatedBoard);

        // Act & Assert
        mockMvc.perform(put("/api/boards/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Board"));
    }

    /**
     * Test PUT /api/boards/{id} - update board as regular USER (forbidden).
     */
    @Test
    void testUpdateBoard_AsUser_Forbidden() throws Exception {
        // Arrange
        BoardDto updateData = new BoardDto();
        updateData.setName("Updated Board");

        // Act & Assert
        mockMvc.perform(put("/api/boards/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isForbidden());
    }

    /**
     * Test DELETE /api/boards/{id} - delete board as ADMIN.
     */
    @Test
    void testDeleteBoard_AsAdmin() throws Exception {
        // Arrange
        when(userService.findByUsername("admin")).thenReturn(adminUser);
        doNothing().when(boardService).deleteBoard(1L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/boards/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    /**
     * Test DELETE /api/boards/{id} - delete board as regular USER (forbidden).
     */
    @Test
    void testDeleteBoard_AsUser_Forbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/boards/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    /**
     * Test DELETE /api/boards/{id} - delete non-existent board.
     */
    @Test
    void testDeleteBoard_NotFound() throws Exception {
        // Arrange
        when(userService.findByUsername("admin")).thenReturn(adminUser);
        doThrow(new RuntimeException("Board not found")).when(boardService).deleteBoard(999L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/boards/999")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test GET /api/boards/templates - get templates as ADMIN.
     */
    @Test
    void testGetTemplates_AsAdmin() throws Exception {
        // Arrange
        BoardDto templateDto = new BoardDto(3L, "Template", "Template Desc", 1L, false, true);
        List<BoardDto> templates = Arrays.asList(templateDto);

        when(userService.findByUsername("admin")).thenReturn(adminUser);
        when(boardService.getTemplatesDto(1L)).thenReturn(templates);

        // Act & Assert
        mockMvc.perform(get("/api/boards/templates")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].isTemplate").value(true));
    }

    /**
     * Test GET /api/boards/templates - get templates as USER (forbidden).
     */
    @Test
    void testGetTemplates_AsUser_Forbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/boards/templates")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }
}

