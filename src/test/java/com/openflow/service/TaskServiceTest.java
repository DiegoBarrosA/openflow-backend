package com.openflow.service;

import com.openflow.dto.TaskDto;
import com.openflow.model.Board;
import com.openflow.model.Status;
import com.openflow.model.Task;
import com.openflow.model.User;
import com.openflow.repository.TaskRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskService.
 * Covers task module test cases: TASK-01 to TASK-04.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private BoardService boardService;

    @Mock
    private StatusService statusService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChangeLogService changeLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CustomFieldService customFieldService;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;
    private Board testBoard;
    private Status testStatus;
    private Status newStatus;
    private User testUser;
    private Long ownerId = 1L;
    private Long boardId = 1L;
    private Long statusId = 1L;
    private Long newStatusId = 2L;

    @BeforeEach
    void setUp() {
        testBoard = new Board();
        testBoard.setId(boardId);
        testBoard.setName("Test Board");
        testBoard.setUserId(ownerId);

        testStatus = new Status();
        testStatus.setId(statusId);
        testStatus.setName("To Do");
        testStatus.setBoardId(boardId);

        newStatus = new Status();
        newStatus.setId(newStatusId);
        newStatus.setName("In Progress");
        newStatus.setBoardId(boardId);

        testTask = new Task();
        testTask.setId(1L);
        testTask.setTitle("Test Task");
        testTask.setDescription("Test Description");
        testTask.setBoardId(boardId);
        testTask.setStatusId(statusId);
        testTask.setCreatedAt(LocalDateTime.now());

        testUser = new User();
        testUser.setId(2L);
        testUser.setUsername("assignee");
    }

    /**
     * TASK-01: Test successful task creation.
     */
    @Test
    void testCreateTask() {
        // Arrange
        Task newTask = new Task();
        newTask.setTitle("New Task");
        newTask.setDescription("New Description");
        newTask.setBoardId(boardId);
        newTask.setStatusId(statusId);

        when(boardService.getBoardById(boardId, ownerId)).thenReturn(testBoard);
        when(boardService.getBoardAccessLevel(boardId, ownerId)).thenReturn("OWNER");
        when(statusService.getStatusById(statusId, ownerId)).thenReturn(testStatus);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(2L);
            return task;
        });
        doNothing().when(changeLogService).logCreate(anyString(), anyLong(), anyLong());
        doNothing().when(notificationService).notifyEntityChange(anyString(), anyLong(), anyString(), anyString(), anyLong());

        // Act
        Task result = taskService.createTask(newTask, ownerId);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("New Task", result.getTitle());
        verify(taskRepository).save(any(Task.class));
        verify(changeLogService).logCreate(eq(ChangeLogService.ENTITY_TASK), eq(2L), eq(ownerId));
        verify(notificationService).notifyEntityChange(eq("BOARD"), eq(boardId), eq("TASK_CREATED"), anyString(), eq(ownerId));
    }

    /**
     * TASK-01 (variant): Test create task with DTO.
     */
    @Test
    void testCreateTaskDto() {
        // Arrange
        TaskDto taskDto = new TaskDto();
        taskDto.setTitle("DTO Task");
        taskDto.setDescription("Created via DTO");
        taskDto.setBoardId(boardId);
        taskDto.setStatusId(statusId);

        when(boardService.getBoardById(boardId, ownerId)).thenReturn(testBoard);
        when(boardService.getBoardAccessLevel(boardId, ownerId)).thenReturn("OWNER");
        when(statusService.getStatusById(statusId, ownerId)).thenReturn(testStatus);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(3L);
            return task;
        });
        doNothing().when(changeLogService).logCreate(anyString(), anyLong(), anyLong());
        doNothing().when(notificationService).notifyEntityChange(anyString(), anyLong(), anyString(), anyString(), anyLong());

        // Act
        TaskDto result = taskService.createTaskDto(taskDto, ownerId);

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("DTO Task", result.getTitle());
    }

    /**
     * TASK-01 (variant): Test create task without WRITE access fails.
     */
    @Test
    void testCreateTask_NoWriteAccess() {
        // Arrange
        Task newTask = new Task();
        newTask.setTitle("New Task");
        newTask.setBoardId(boardId);
        newTask.setStatusId(statusId);

        when(boardService.getBoardById(boardId, ownerId)).thenReturn(testBoard);
        when(boardService.getBoardAccessLevel(boardId, ownerId)).thenReturn("READ");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.createTask(newTask, ownerId);
        });
        assertTrue(exception.getMessage().contains("WRITE access required"));
        verify(taskRepository, never()).save(any(Task.class));
    }

    /**
     * TASK-02: Test moving task to different status (column).
     */
    @Test
    void testUpdateTaskStatus_MoveTask() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(boardService.getBoardById(boardId, ownerId)).thenReturn(testBoard);
        when(boardService.getBoardAccessLevel(boardId, ownerId)).thenReturn("OWNER");
        when(statusService.getStatusById(newStatusId, ownerId)).thenReturn(newStatus);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(changeLogService).logMove(anyString(), anyLong(), anyLong(), anyString(), anyString());
        doNothing().when(notificationService).notifyEntityChange(anyString(), anyLong(), anyString(), anyString(), anyLong());

        Task updatedTask = new Task();
        updatedTask.setTitle("Test Task");
        updatedTask.setStatusId(newStatusId); // Moving to "In Progress"

        // Act
        Task result = taskService.updateTask(1L, updatedTask, ownerId);

        // Assert
        assertNotNull(result);
        verify(changeLogService).logMove(eq(ChangeLogService.ENTITY_TASK), eq(1L), eq(ownerId), 
            eq(String.valueOf(statusId)), eq(String.valueOf(newStatusId)));
        verify(notificationService).notifyEntityChange(eq("TASK"), eq(1L), eq("TASK_MOVED"), anyString(), eq(ownerId));
    }

    /**
     * TASK-03: Test updating task fields (title, description).
     */
    @Test
    void testUpdateTaskFields() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(boardService.getBoardById(boardId, ownerId)).thenReturn(testBoard);
        when(boardService.getBoardAccessLevel(boardId, ownerId)).thenReturn("OWNER");
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(changeLogService).logFieldChange(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString());
        doNothing().when(notificationService).notifyEntityChange(anyString(), anyLong(), anyString(), anyString(), anyLong());

        Task updatedTask = new Task();
        updatedTask.setTitle("Updated Title");
        updatedTask.setDescription("Updated Description");
        updatedTask.setStatusId(statusId); // Same status

        // Act
        Task result = taskService.updateTask(1L, updatedTask, ownerId);

        // Assert
        assertNotNull(result);
        verify(changeLogService).logFieldChange(eq(ChangeLogService.ENTITY_TASK), eq(1L), eq(ownerId), 
            eq("title"), eq("Test Task"), eq("Updated Title"));
        verify(notificationService).notifyEntityChange(eq("TASK"), eq(1L), eq("TASK_UPDATED"), anyString(), eq(ownerId));
    }

    /**
     * TASK-03 (variant): Test updating task with assigned user.
     */
    @Test
    void testUpdateTaskFields_WithAssignedUser() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(boardService.getBoardById(boardId, ownerId)).thenReturn(testBoard);
        when(boardService.getBoardAccessLevel(boardId, ownerId)).thenReturn("OWNER");
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(changeLogService).logFieldChange(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString());
        doNothing().when(notificationService).notifyEntityChange(anyString(), anyLong(), anyString(), anyString(), anyLong());

        Task updatedTask = new Task();
        updatedTask.setTitle("Test Task"); // Same title
        updatedTask.setAssignedUserId(2L);
        updatedTask.setStatusId(statusId);

        // Act
        Task result = taskService.updateTask(1L, updatedTask, ownerId);

        // Assert
        assertNotNull(result);
        verify(changeLogService).logFieldChange(eq(ChangeLogService.ENTITY_TASK), eq(1L), eq(ownerId), 
            eq("assignedUser"), eq("none"), eq("assignee"));
    }

    /**
     * TASK-04: Test successful task deletion.
     */
    @Test
    void testDeleteTask() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(boardService.getBoardById(boardId, ownerId)).thenReturn(testBoard);
        when(boardService.getBoardAccessLevel(boardId, ownerId)).thenReturn("OWNER");
        doNothing().when(changeLogService).logDelete(anyString(), anyLong(), anyLong());
        doNothing().when(notificationService).notifyEntityChange(anyString(), anyLong(), anyString(), anyString(), anyLong());
        doNothing().when(taskRepository).delete(any(Task.class));

        // Act
        taskService.deleteTask(1L, ownerId);

        // Assert
        verify(changeLogService).logDelete(eq(ChangeLogService.ENTITY_TASK), eq(1L), eq(ownerId));
        verify(notificationService).notifyEntityChange(eq("TASK"), eq(1L), eq("TASK_DELETED"), anyString(), eq(ownerId));
        verify(taskRepository).delete(testTask);
    }

    /**
     * TASK-04 (variant): Test delete task without WRITE access fails.
     */
    @Test
    void testDeleteTask_NoWriteAccess() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(boardService.getBoardById(boardId, ownerId)).thenReturn(testBoard);
        when(boardService.getBoardAccessLevel(boardId, ownerId)).thenReturn("READ");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.deleteTask(1L, ownerId);
        });
        assertTrue(exception.getMessage().contains("WRITE access required"));
        verify(taskRepository, never()).delete(any(Task.class));
    }

    /**
     * TASK-04 (variant): Test delete task not found.
     */
    @Test
    void testDeleteTask_NotFound() {
        // Arrange
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            taskService.deleteTask(999L, ownerId);
        });
        verify(taskRepository, never()).delete(any(Task.class));
    }

    /**
     * Test getTaskById - success.
     */
    @Test
    void testGetTaskById_Success() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(boardService.getBoardById(boardId, ownerId)).thenReturn(testBoard);

        // Act
        Task result = taskService.getTaskById(1L, ownerId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Task", result.getTitle());
    }

    /**
     * Test getTaskById - not found.
     */
    @Test
    void testGetTaskById_NotFound() {
        // Arrange
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            taskService.getTaskById(999L, ownerId);
        });
    }

    /**
     * Test getTasksByBoardId.
     */
    @Test
    void testGetTasksByBoardId() {
        // Arrange
        List<Task> tasks = Arrays.asList(testTask);
        when(boardService.getBoardById(boardId, ownerId)).thenReturn(testBoard);
        when(taskRepository.findByBoardId(boardId)).thenReturn(tasks);

        // Act
        List<Task> result = taskService.getTasksByBoardId(boardId, ownerId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Task", result.get(0).getTitle());
    }

    /**
     * Test getTasksByBoardIdDto - with assigned username.
     */
    @Test
    void testGetTasksByBoardIdDto_WithAssignedUser() {
        // Arrange
        testTask.setAssignedUserId(2L);
        List<Task> tasks = Arrays.asList(testTask);
        when(boardService.getBoardById(boardId, ownerId)).thenReturn(testBoard);
        when(taskRepository.findByBoardId(boardId)).thenReturn(tasks);
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

        // Act
        List<TaskDto> result = taskService.getTasksByBoardIdDto(boardId, ownerId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("assignee", result.get(0).getAssignedUsername());
    }

    /**
     * Test getTasksByBoardIdDtoPublic - for public boards.
     */
    @Test
    void testGetTasksByBoardIdDtoPublic() {
        // Arrange
        List<Task> tasks = Arrays.asList(testTask);
        when(taskRepository.findByBoardId(boardId)).thenReturn(tasks);

        // Act
        List<TaskDto> result = taskService.getTasksByBoardIdDtoPublic(boardId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        // No board access validation for public boards
        verify(boardService, never()).getBoardById(anyLong(), anyLong());
    }
}

