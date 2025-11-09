package com.openflow.service;

import com.openflow.model.Task;
import com.openflow.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BoardService boardService;

    @Autowired
    private StatusService statusService;

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
}

