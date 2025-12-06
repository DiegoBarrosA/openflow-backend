package com.openflow.dto;

import com.openflow.model.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldDefinitionDto {
    private Long id;
    
    @NotNull(message = "Board ID is required")
    private Long boardId;
    
    @NotBlank(message = "Field name is required")
    @Size(min = 1, max = 100, message = "Field name must be between 1 and 100 characters")
    private String name;
    
    @NotNull(message = "Field type is required")
    private FieldType fieldType;
    
    private List<String> options; // For dropdown fields
    
    private Integer displayOrder;
    
    private Boolean isRequired;
    
    private Boolean showInCard;
}

