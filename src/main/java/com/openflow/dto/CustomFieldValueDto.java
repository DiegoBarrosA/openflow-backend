package com.openflow.dto;

import com.openflow.model.FieldType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldValueDto {
    private Long id;
    private Long taskId;
    private Long fieldDefinitionId;
    private String fieldName;      // From definition
    private FieldType fieldType;   // From definition
    private String value;
}

