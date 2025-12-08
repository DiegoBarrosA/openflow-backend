package com.openflow.dto;

import com.openflow.model.AccessLevel;
import java.time.LocalDateTime;

public class BoardAccessDto {
    private Long id;
    private Long boardId;
    private Long userId;
    private String username;
    private AccessLevel accessLevel;
    private Long grantedBy;
    private String grantedByUsername;
    private LocalDateTime createdAt;

    public BoardAccessDto() {}

    public BoardAccessDto(Long id, Long boardId, Long userId, String username, AccessLevel accessLevel,
                         Long grantedBy, String grantedByUsername, LocalDateTime createdAt) {
        this.id = id;
        this.boardId = boardId;
        this.userId = userId;
        this.username = username;
        this.accessLevel = accessLevel;
        this.grantedBy = grantedBy;
        this.grantedByUsername = grantedByUsername;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBoardId() { return boardId; }
    public void setBoardId(Long boardId) { this.boardId = boardId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public AccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(AccessLevel accessLevel) { this.accessLevel = accessLevel; }

    public Long getGrantedBy() { return grantedBy; }
    public void setGrantedBy(Long grantedBy) { this.grantedBy = grantedBy; }

    public String getGrantedByUsername() { return grantedByUsername; }
    public void setGrantedByUsername(String grantedByUsername) { this.grantedByUsername = grantedByUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

