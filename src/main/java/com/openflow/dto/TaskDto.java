package com.openflow.dto;

import java.time.LocalDateTime;

public class TaskDto {
    private Long id;
    private String title;
    private String description;
    private Long statusId;
    private Long boardId;
    private LocalDateTime createdAt;

    public TaskDto() {}

    public TaskDto(Long id, String title, String description, Long statusId, Long boardId, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.statusId = statusId;
        this.boardId = boardId;
        this.createdAt = createdAt;
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
}
