package com.openflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openflow.dto.CustomFieldDefinitionDto;
import com.openflow.dto.CustomFieldValueDto;
import com.openflow.model.CustomFieldDefinition;
import com.openflow.model.CustomFieldValue;
import com.openflow.repository.CustomFieldDefinitionRepository;
import com.openflow.repository.CustomFieldValueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomFieldService {
    private static final Logger logger = LoggerFactory.getLogger(CustomFieldService.class);
    
    @Autowired
    private CustomFieldDefinitionRepository definitionRepository;
    
    @Autowired
    private CustomFieldValueRepository valueRepository;
    
    @Autowired
    private BoardService boardService;
    
    @Autowired
    private ChangeLogService changeLogService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== Field Definition Methods ====================

    /**
     * Get all custom field definitions for a board.
     */
    public List<CustomFieldDefinitionDto> getFieldDefinitions(Long boardId, Long userId) {
        boardService.getBoardById(boardId, userId); // Validate access
        return definitionRepository.findByBoardIdOrderByDisplayOrderAsc(boardId)
                .stream()
                .map(this::toDefinitionDto)
                .collect(Collectors.toList());
    }

    /**
     * Create a new custom field definition.
     */
    @Transactional
    public CustomFieldDefinitionDto createFieldDefinition(CustomFieldDefinitionDto dto, Long userId) {
        boardService.getBoardById(dto.getBoardId(), userId); // Validate access
        
        // Check if we're trying to add a showInCard field and there are already 3
        if (dto.getShowInCard() != null && dto.getShowInCard()) {
            long currentShowInCardCount = definitionRepository.findByBoardIdOrderByDisplayOrderAsc(dto.getBoardId())
                    .stream()
                    .filter(d -> d.getShowInCard() != null && d.getShowInCard())
                    .count();
            if (currentShowInCardCount >= 3) {
                throw new RuntimeException("Maximum of 3 fields can be shown in card");
            }
        }
        
        CustomFieldDefinition definition = new CustomFieldDefinition();
        definition.setBoardId(dto.getBoardId());
        definition.setName(dto.getName());
        definition.setFieldType(dto.getFieldType());
        definition.setOptions(serializeOptions(dto.getOptions()));
        definition.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        definition.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : false);
        definition.setShowInCard(dto.getShowInCard() != null ? dto.getShowInCard() : false);
        
        CustomFieldDefinition saved = definitionRepository.save(definition);
        
        changeLogService.logCreate("CUSTOM_FIELD", saved.getId(), userId);
        
        return toDefinitionDto(saved);
    }

    /**
     * Update a custom field definition.
     */
    @Transactional
    public CustomFieldDefinitionDto updateFieldDefinition(Long id, CustomFieldDefinitionDto dto, Long userId) {
        CustomFieldDefinition existing = definitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom field not found"));
        
        boardService.getBoardById(existing.getBoardId(), userId); // Validate access
        
        // Check if we're trying to enable showInCard and there are already 3 (excluding current)
        Boolean newShowInCard = dto.getShowInCard() != null ? dto.getShowInCard() : existing.getShowInCard();
        Boolean currentShowInCard = existing.getShowInCard() != null ? existing.getShowInCard() : false;
        
        if (newShowInCard && !currentShowInCard) {
            long currentShowInCardCount = definitionRepository.findByBoardIdOrderByDisplayOrderAsc(existing.getBoardId())
                    .stream()
                    .filter(d -> d.getShowInCard() != null && d.getShowInCard() && !d.getId().equals(id))
                    .count();
            if (currentShowInCardCount >= 3) {
                throw new RuntimeException("Maximum of 3 fields can be shown in card");
            }
        }
        
        if (!existing.getName().equals(dto.getName())) {
            changeLogService.logFieldChange("CUSTOM_FIELD", id, userId, 
                "name", existing.getName(), dto.getName());
        }
        
        existing.setName(dto.getName());
        existing.setFieldType(dto.getFieldType());
        existing.setOptions(serializeOptions(dto.getOptions()));
        existing.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : existing.getDisplayOrder());
        existing.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : existing.getIsRequired());
        existing.setShowInCard(newShowInCard);
        
        return toDefinitionDto(definitionRepository.save(existing));
    }

    /**
     * Delete a custom field definition and all its values.
     */
    @Transactional
    public void deleteFieldDefinition(Long id, Long userId) {
        CustomFieldDefinition definition = definitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Custom field not found"));
        
        boardService.getBoardById(definition.getBoardId(), userId); // Validate access
        
        // Delete all values for this field
        valueRepository.deleteByFieldDefinitionId(id);
        
        changeLogService.logDelete("CUSTOM_FIELD", id, userId);
        
        definitionRepository.delete(definition);
    }

    // ==================== Field Value Methods ====================

    /**
     * Get all custom field values for a task.
     */
    public List<CustomFieldValueDto> getTaskFieldValues(Long taskId) {
        List<CustomFieldValue> values = valueRepository.findByTaskId(taskId);
        
        // Get all field definitions for the task's board
        if (values.isEmpty()) {
            return List.of();
        }
        
        // Enrich with field definition info
        Map<Long, CustomFieldDefinition> definitionMap = values.stream()
                .map(CustomFieldValue::getFieldDefinitionId)
                .distinct()
                .map(defId -> definitionRepository.findById(defId).orElse(null))
                .filter(def -> def != null)
                .collect(Collectors.toMap(CustomFieldDefinition::getId, def -> def));
        
        return values.stream()
                .map(value -> toValueDto(value, definitionMap.get(value.getFieldDefinitionId())))
                .collect(Collectors.toList());
    }

    /**
     * Get visible custom field values for a task (fields with showInCard=true).
     */
    public List<CustomFieldValueDto> getTaskVisibleFieldValues(Long taskId, Long boardId) {
        // Get definitions that should be shown in card
        List<CustomFieldDefinition> visibleDefinitions = definitionRepository.findByBoardIdOrderByDisplayOrderAsc(boardId)
                .stream()
                .filter(d -> d.getShowInCard() != null && d.getShowInCard())
                .limit(3)
                .collect(Collectors.toList());
        
        if (visibleDefinitions.isEmpty()) {
            return List.of();
        }
        
        // Get values for these definitions
        List<CustomFieldValue> values = valueRepository.findByTaskId(taskId);
        Map<Long, String> valueMap = values.stream()
                .collect(Collectors.toMap(CustomFieldValue::getFieldDefinitionId, CustomFieldValue::getValue, (a, b) -> a));
        
        // Create DTOs with values (or empty if no value set)
        return visibleDefinitions.stream()
                .map(def -> {
                    CustomFieldValueDto dto = new CustomFieldValueDto();
                    dto.setFieldDefinitionId(def.getId());
                    dto.setFieldName(def.getName());
                    dto.setFieldType(def.getFieldType());
                    dto.setValue(valueMap.getOrDefault(def.getId(), ""));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Set a custom field value for a task.
     */
    @Transactional
    public CustomFieldValueDto setFieldValue(Long taskId, Long fieldDefinitionId, String value, Long userId) {
        CustomFieldDefinition definition = definitionRepository.findById(fieldDefinitionId)
                .orElseThrow(() -> new RuntimeException("Custom field definition not found"));
        
        // Find existing value or create new
        CustomFieldValue fieldValue = valueRepository
                .findByTaskIdAndFieldDefinitionId(taskId, fieldDefinitionId)
                .orElseGet(() -> {
                    CustomFieldValue newValue = new CustomFieldValue();
                    newValue.setTaskId(taskId);
                    newValue.setFieldDefinitionId(fieldDefinitionId);
                    return newValue;
                });
        
        String oldValue = fieldValue.getValue();
        fieldValue.setValue(value);
        
        CustomFieldValue saved = valueRepository.save(fieldValue);
        
        // Log the change
        if (oldValue == null || !oldValue.equals(value)) {
            changeLogService.logFieldChange(ChangeLogService.ENTITY_TASK, taskId, userId,
                "custom:" + definition.getName(), oldValue, value);
        }
        
        return toValueDto(saved, definition);
    }

    /**
     * Set multiple custom field values for a task.
     */
    @Transactional
    public List<CustomFieldValueDto> setFieldValues(Long taskId, Map<Long, String> fieldValues, Long userId) {
        return fieldValues.entrySet().stream()
                .map(entry -> setFieldValue(taskId, entry.getKey(), entry.getValue(), userId))
                .collect(Collectors.toList());
    }

    // ==================== Conversion Methods ====================

    private CustomFieldDefinitionDto toDefinitionDto(CustomFieldDefinition definition) {
        CustomFieldDefinitionDto dto = new CustomFieldDefinitionDto();
        dto.setId(definition.getId());
        dto.setBoardId(definition.getBoardId());
        dto.setName(definition.getName());
        dto.setFieldType(definition.getFieldType());
        dto.setOptions(deserializeOptions(definition.getOptions()));
        dto.setDisplayOrder(definition.getDisplayOrder());
        dto.setIsRequired(definition.getIsRequired());
        dto.setShowInCard(definition.getShowInCard());
        return dto;
    }

    private CustomFieldValueDto toValueDto(CustomFieldValue value, CustomFieldDefinition definition) {
        CustomFieldValueDto dto = new CustomFieldValueDto();
        dto.setId(value.getId());
        dto.setTaskId(value.getTaskId());
        dto.setFieldDefinitionId(value.getFieldDefinitionId());
        dto.setValue(value.getValue());
        if (definition != null) {
            dto.setFieldName(definition.getName());
            dto.setFieldType(definition.getFieldType());
        }
        return dto;
    }

    private String serializeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize options", e);
            return null;
        }
    }

    private List<String> deserializeOptions(String options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(options, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize options", e);
            return List.of();
        }
    }
}

