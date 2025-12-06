package com.openflow.service;

import com.openflow.dto.TaskDto;
import com.openflow.model.Task;
import com.openflow.model.User;
import com.openflow.repository.TaskRepository;
import com.openflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BoardService boardService;

    @Autowired
    private StatusService statusService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private ChangeLogService changeLogService;

    @Autowired
    @Lazy
    private NotificationService notificationService;

    @Autowired
    @Lazy
    private CustomFieldService customFieldService;

    private TaskDto toDto(Task task) {
        String assignedUsername = null;
        if (task.getAssignedUserId() != null) {
            assignedUsername = userRepository.findById(task.getAssignedUserId())
                    .map(User::getUsername)
                    .orElse(null);
        }
        return new TaskDto(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatusId(),
            task.getBoardId(),
            task.getAssignedUserId(),
            assignedUsername,
            task.getCreatedAt()
        );
    }

    private Task toEntity(TaskDto dto) {
        Task task = new Task();
        task.setId(dto.getId());
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatusId(dto.getStatusId());
        task.setBoardId(dto.getBoardId());
        task.setAssignedUserId(dto.getAssignedUserId());
        task.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now());
        return task;
    }

    public List<TaskDto> getTasksByBoardIdDto(Long boardId, Long userId) {
        return getTasksByBoardId(boardId, userId).stream().map(this::toDto).toList();
    }

    public TaskDto getTaskByIdDto(Long id, Long userId) {
        return toDto(getTaskById(id, userId));
    }

    public TaskDto createTaskDto(TaskDto taskDto, Long userId) {
        Task task = toEntity(taskDto);
        Task created = createTask(task, userId);
        
        // Save custom field values if provided
        Map<Long, String> customFieldValues = taskDto.getCustomFieldValues();
        if (customFieldValues != null && !customFieldValues.isEmpty()) {
            customFieldService.setFieldValues(created.getId(), customFieldValues, userId);
        }
        
        return toDto(created);
    }

    public TaskDto updateTaskDto(Long id, TaskDto taskDto, Long userId) {
        Task updated = updateTask(id, toEntity(taskDto), userId);
        return toDto(updated);
    }

    public List<Task> getTasksByBoardId(Long boardId, Long userId) {
        boardService.getBoardById(boardId, userId); // Validate board access
        return taskRepository.findByBoardId(boardId);
    }

    public Task getTaskById(Long id, Long userId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        boardService.getBoardById(task.getBoardId(), userId); // Validate board access
        return task;
    }

    public Task createTask(Task task, Long userId) {
        boardService.getBoardById(task.getBoardId(), userId); // Validate board access
        statusService.getStatusById(task.getStatusId(), userId); // Validate status exists and belongs to board
        Task saved = taskRepository.save(task);
        
        // Log creation
        changeLogService.logCreate(ChangeLogService.ENTITY_TASK, saved.getId(), userId);
        
        // Send notifications to board subscribers
        notificationService.notifyEntityChange(
            "BOARD", task.getBoardId(),
            "TASK_CREATED",
            "New task created: " + task.getTitle(),
            userId
        );
        
        return saved;
    }

    public Task updateTask(Long id, Task updatedTask, Long userId) {
        Task existingTask = getTaskById(id, userId);
        boolean wasUpdated = false;
        boolean wasMoved = false;
        
        // Log field changes
        if (!existingTask.getTitle().equals(updatedTask.getTitle())) {
            changeLogService.logFieldChange(ChangeLogService.ENTITY_TASK, id, userId,
                "title", existingTask.getTitle(), updatedTask.getTitle());
            wasUpdated = true;
        }
        existingTask.setTitle(updatedTask.getTitle());
        
        if (updatedTask.getDescription() != null && 
            !String.valueOf(updatedTask.getDescription()).equals(String.valueOf(existingTask.getDescription()))) {
            changeLogService.logFieldChange(ChangeLogService.ENTITY_TASK, id, userId,
                "description", existingTask.getDescription(), updatedTask.getDescription());
            existingTask.setDescription(updatedTask.getDescription());
            wasUpdated = true;
        }
        
        if (updatedTask.getStatusId() != null && !updatedTask.getStatusId().equals(existingTask.getStatusId())) {
            statusService.getStatusById(updatedTask.getStatusId(), userId); // Validate status
            changeLogService.logMove(ChangeLogService.ENTITY_TASK, id, userId,
                String.valueOf(existingTask.getStatusId()), String.valueOf(updatedTask.getStatusId()));
            existingTask.setStatusId(updatedTask.getStatusId());
            wasMoved = true;
        }
        
        // Handle assignedUserId change
        Long newAssignedUserId = updatedTask.getAssignedUserId();
        Long existingAssignedUserId = existingTask.getAssignedUserId();
        
        if ((newAssignedUserId == null && existingAssignedUserId != null) ||
            (newAssignedUserId != null && !newAssignedUserId.equals(existingAssignedUserId))) {
            
            String oldUsername = existingAssignedUserId != null 
                ? userRepository.findById(existingAssignedUserId).map(User::getUsername).orElse("none")
                : "none";
            String newUsername = newAssignedUserId != null 
                ? userRepository.findById(newAssignedUserId).map(User::getUsername).orElse("none")
                : "none";
            
            changeLogService.logFieldChange(ChangeLogService.ENTITY_TASK, id, userId,
                "assignedUser", oldUsername, newUsername);
            existingTask.setAssignedUserId(newAssignedUserId);
            wasUpdated = true;
        }
        
        Task saved = taskRepository.save(existingTask);
        
        // Send notifications
        if (wasMoved) {
            notificationService.notifyEntityChange(
                "TASK", id,
                "TASK_MOVED",
                "Task moved: " + existingTask.getTitle(),
                userId
            );
        } else if (wasUpdated) {
            notificationService.notifyEntityChange(
                "TASK", id,
                "TASK_UPDATED",
                "Task updated: " + existingTask.getTitle(),
                userId
            );
        }
        
        return saved;
    }

    public void deleteTask(Long id, Long userId) {
        Task task = getTaskById(id, userId);
        
        // Log deletion before deleting
        changeLogService.logDelete(ChangeLogService.ENTITY_TASK, id, userId);
        
        // Send notifications to task subscribers
        notificationService.notifyEntityChange(
            "TASK", id,
            "TASK_DELETED",
            "Task deleted: " + task.getTitle(),
            userId
        );
        
        taskRepository.delete(task);
    }

    /**
     * Get tasks for a public board (no user validation).
     */
    public List<TaskDto> getTasksByBoardIdDtoPublic(Long boardId) {
        return taskRepository.findByBoardId(boardId)
                .stream()
                .map(this::toDto)
                .toList();
    }
}
