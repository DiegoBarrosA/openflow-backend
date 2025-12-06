package com.openflow.dto;

public class BoardDto {
    private Long id;
    private String name;
    private String description;
    private Long userId;
    private Boolean isPublic;

    public BoardDto() {}

    public BoardDto(Long id, String name, String description, Long userId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.isPublic = false;
    }

    public BoardDto(Long id, String name, String description, Long userId, Boolean isPublic) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.isPublic = isPublic;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
}
