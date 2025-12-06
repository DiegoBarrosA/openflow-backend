package com.openflow.service;

import com.openflow.dto.TaskDto;
import com.openflow.model.Task;
import com.openflow.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BoardService boardService;

    @Autowired
    private StatusService statusService;

    private TaskDto toDto(Task task) {
        return new TaskDto(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatusId(),
            task.getBoardId(),
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
        return taskRepository.save(task);
    }

    public Task updateTask(Long id, Task updatedTask, Long userId) {
        Task existingTask = getTaskById(id, userId);
        existingTask.setTitle(updatedTask.getTitle());
        if (updatedTask.getDescription() != null) {
            existingTask.setDescription(updatedTask.getDescription());
        }
        if (updatedTask.getStatusId() != null) {
            statusService.getStatusById(updatedTask.getStatusId(), userId); // Validate status
            existingTask.setStatusId(updatedTask.getStatusId());
        }
        return taskRepository.save(existingTask);
    }

    public void deleteTask(Long id, Long userId) {
        Task task = getTaskById(id, userId);
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
