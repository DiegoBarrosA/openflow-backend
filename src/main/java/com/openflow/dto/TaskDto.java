package com.openflow.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class TaskDto {
    private Long id;
    private String title;
    private String description;
    private Long statusId;
    private Long boardId;
    private Long assignedUserId;
    private String assignedUsername;
    private LocalDateTime createdAt;
    private Map<String, String> customFieldValues; // fieldDefinitionId (as string) -> value

    public TaskDto() {}

    public TaskDto(Long id, String title, String description, Long statusId, Long boardId, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.statusId = statusId;
        this.boardId = boardId;
        this.createdAt = createdAt;
    }

    public TaskDto(Long id, String title, String description, Long statusId, Long boardId, Long assignedUserId, String assignedUsername, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.statusId = statusId;
        this.boardId = boardId;
        this.assignedUserId = assignedUserId;
        this.assignedUsername = assignedUsername;
        this.createdAt = createdAt;
    }

    public TaskDto(Long id, String title, String description, Long statusId, Long boardId, LocalDateTime createdAt, Map<String, String> customFieldValues) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.statusId = statusId;
        this.boardId = boardId;
        this.createdAt = createdAt;
        this.customFieldValues = customFieldValues;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getStatusId() { return statusId; }
    public void setStatusId(Long statusId) { this.statusId = statusId; }

    public Long getBoardId() { return boardId; }
    public void setBoardId(Long boardId) { this.boardId = boardId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(Long assignedUserId) { this.assignedUserId = assignedUserId; }

    public String getAssignedUsername() { return assignedUsername; }
    public void setAssignedUsername(String assignedUsername) { this.assignedUsername = assignedUsername; }

    public Map<String, String> getCustomFieldValues() { return customFieldValues; }
    public void setCustomFieldValues(Map<String, String> customFieldValues) { this.customFieldValues = customFieldValues; }
}
