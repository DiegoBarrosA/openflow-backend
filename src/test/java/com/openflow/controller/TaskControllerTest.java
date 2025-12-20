package com.openflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openflow.dto.TaskDto;
import com.openflow.model.Role;
import com.openflow.model.User;
import com.openflow.service.JwtService;
import com.openflow.service.TaskService;
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

import java.time.LocalDateTime;
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
 * Controller integration tests for TaskController.
 * Tests task CRUD operations with authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private com.openflow.config.AzureAdAuthenticationFilter azureAdAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private TaskDto testTaskDto;

    @BeforeEach
    void setUp() {
        reset(taskService, userService);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);

        testTaskDto = new TaskDto(
                1L,
                "Test Task",
                "Test Description",
                1L, // statusId
                1L, // boardId
                null, // assignedUserId
                null, // assignedUsername
                LocalDateTime.now()
        );
    }

    /**
     * Test GET /api/tasks?boardId=1 - get all tasks for a board.
     */
    @Test
    void testGetTasksByBoard_Success() throws Exception {
        // Arrange
        List<TaskDto> tasks = Arrays.asList(testTaskDto);
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(taskService.getTasksByBoardIdDto(1L, 1L)).thenReturn(tasks);

        // Act & Assert
        mockMvc.perform(get("/api/tasks")
                        .param("boardId", "1")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Test Task"));
    }

    /**
     * Test GET /api/tasks?boardId=999 - board not found or no access.
     */
    @Test
    void testGetTasksByBoard_NoAccess() throws Exception {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(taskService.getTasksByBoardIdDto(999L, 1L))
                .thenThrow(new RuntimeException("Board not found"));

        // Act & Assert
        mockMvc.perform(get("/api/tasks")
                        .param("boardId", "999")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test GET /api/tasks/{id} - get task by ID.
     */
    @Test
    void testGetTaskById_Success() throws Exception {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(taskService.getTaskByIdDto(1L, 1L)).thenReturn(testTaskDto);

        // Act & Assert
        mockMvc.perform(get("/api/tasks/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    /**
     * Test GET /api/tasks/{id} - task not found.
     */
    @Test
    void testGetTaskById_NotFound() throws Exception {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(taskService.getTaskByIdDto(999L, 1L))
                .thenThrow(new RuntimeException("Task not found"));

        // Act & Assert
        mockMvc.perform(get("/api/tasks/999")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER")))
                .andExpect(status().isNotFound());
    }

    /**
     * Test POST /api/tasks - create task.
     */
    @Test
    void testCreateTask_Success() throws Exception {
        // Arrange
        TaskDto newTask = new TaskDto();
        newTask.setTitle("New Task");
        newTask.setDescription("New Description");
        newTask.setBoardId(1L);
        newTask.setStatusId(1L);

        TaskDto createdTask = new TaskDto(
                2L, "New Task", "New Description", 1L, 1L, null, null, LocalDateTime.now()
        );

        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(taskService.createTaskDto(any(TaskDto.class), eq(1L))).thenReturn(createdTask);

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("New Task"));
    }

    /**
     * Test POST /api/tasks - create task without write access.
     */
    @Test
    void testCreateTask_NoWriteAccess() throws Exception {
        // Arrange
        TaskDto newTask = new TaskDto();
        newTask.setTitle("New Task");
        newTask.setBoardId(1L);
        newTask.setStatusId(1L);

        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(taskService.createTaskDto(any(TaskDto.class), eq(1L)))
                .thenThrow(new RuntimeException("WRITE access required"));

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test PUT /api/tasks/{id} - update task (move status).
     */
    @Test
    void testUpdateTask_MoveStatus() throws Exception {
        // Arrange
        TaskDto updateData = new TaskDto();
        updateData.setTitle("Test Task");
        updateData.setStatusId(2L); // Moving to different status

        TaskDto updatedTask = new TaskDto(
                1L, "Test Task", "Test Description", 2L, 1L, null, null, LocalDateTime.now()
        );

        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(taskService.updateTaskDto(eq(1L), any(TaskDto.class), eq(1L))).thenReturn(updatedTask);

        // Act & Assert
        mockMvc.perform(put("/api/tasks/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusId").value(2));
    }

    /**
     * Test PUT /api/tasks/{id} - update task fields.
     */
    @Test
    void testUpdateTask_UpdateFields() throws Exception {
        // Arrange
        TaskDto updateData = new TaskDto();
        updateData.setTitle("Updated Title");
        updateData.setDescription("Updated Description");
        updateData.setStatusId(1L);

        TaskDto updatedTask = new TaskDto(
                1L, "Updated Title", "Updated Description", 1L, 1L, null, null, LocalDateTime.now()
        );

        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(taskService.updateTaskDto(eq(1L), any(TaskDto.class), eq(1L))).thenReturn(updatedTask);

        // Act & Assert
        mockMvc.perform(put("/api/tasks/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"));
    }

    /**
     * Test PUT /api/tasks/{id} - assign user to task.
     */
    @Test
    void testUpdateTask_AssignUser() throws Exception {
        // Arrange
        TaskDto updateData = new TaskDto();
        updateData.setTitle("Test Task");
        updateData.setStatusId(1L);
        updateData.setAssignedUserId(2L);

        TaskDto updatedTask = new TaskDto(
                1L, "Test Task", "Test Description", 1L, 1L, 2L, "assignee", LocalDateTime.now()
        );

        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(taskService.updateTaskDto(eq(1L), any(TaskDto.class), eq(1L))).thenReturn(updatedTask);

        // Act & Assert
        mockMvc.perform(put("/api/tasks/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedUserId").value(2))
                .andExpect(jsonPath("$.assignedUsername").value("assignee"));
    }

    /**
     * Test DELETE /api/tasks/{id} - delete task.
     */
    @Test
    void testDeleteTask_Success() throws Exception {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        doNothing().when(taskService).deleteTask(1L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/tasks/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER")))
                .andExpect(status().isNoContent());
    }

    /**
     * Test DELETE /api/tasks/{id} - delete task without write access.
     */
    @Test
    void testDeleteTask_NoWriteAccess() throws Exception {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        doThrow(new RuntimeException("WRITE access required"))
                .when(taskService).deleteTask(1L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/tasks/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test DELETE /api/tasks/{id} - task not found.
     */
    @Test
    void testDeleteTask_NotFound() throws Exception {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        doThrow(new RuntimeException("Task not found"))
                .when(taskService).deleteTask(999L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/tasks/999")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test task operations as ADMIN.
     */
    @Test
    void testTaskOperations_AsAdmin() throws Exception {
        // Arrange
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ADMIN);

        when(userService.findByUsername("admin")).thenReturn(adminUser);
        when(taskService.getTasksByBoardIdDto(1L, 2L)).thenReturn(Arrays.asList(testTaskDto));

        // Act & Assert - Admin can also access tasks
        mockMvc.perform(get("/api/tasks")
                        .param("boardId", "1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

