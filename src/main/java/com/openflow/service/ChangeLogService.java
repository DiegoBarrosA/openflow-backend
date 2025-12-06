package com.openflow.service;

import com.openflow.dto.ChangeLogDto;
import com.openflow.model.ChangeLog;
import com.openflow.model.User;
import com.openflow.repository.ChangeLogRepository;
import com.openflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChangeLogService {
    private static final Logger logger = LoggerFactory.getLogger(ChangeLogService.class);

    @Autowired
    private ChangeLogRepository changeLogRepository;

    @Autowired
    private UserRepository userRepository;

    // Entity type constants
    public static final String ENTITY_TASK = "TASK";
    public static final String ENTITY_BOARD = "BOARD";
    public static final String ENTITY_STATUS = "STATUS";

    // Action constants
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_MOVE = "MOVE";

    /**
     * Log a create action
     */
    public ChangeLog logCreate(String entityType, Long entityId, Long userId) {
        ChangeLog log = ChangeLog.create(entityType, entityId, userId, ACTION_CREATE);
        logger.info("Logging CREATE: {} #{} by user {}", entityType, entityId, userId);
        return changeLogRepository.save(log);
    }

    /**
     * Log a delete action
     */
    public ChangeLog logDelete(String entityType, Long entityId, Long userId) {
        ChangeLog log = ChangeLog.create(entityType, entityId, userId, ACTION_DELETE);
        logger.info("Logging DELETE: {} #{} by user {}", entityType, entityId, userId);
        return changeLogRepository.save(log);
    }

    /**
     * Log a field change
     */
    public ChangeLog logFieldChange(String entityType, Long entityId, Long userId,
                                     String fieldName, Object oldValue, Object newValue) {
        String oldStr = oldValue != null ? String.valueOf(oldValue) : null;
        String newStr = newValue != null ? String.valueOf(newValue) : null;
        
        // Only log if values actually changed
        if ((oldStr == null && newStr == null) || (oldStr != null && oldStr.equals(newStr))) {
            return null;
        }
        
        ChangeLog log = ChangeLog.fieldChange(entityType, entityId, userId, fieldName, oldStr, newStr);
        logger.info("Logging FIELD CHANGE: {} #{} field '{}' changed from '{}' to '{}' by user {}", 
                   entityType, entityId, fieldName, oldStr, newStr, userId);
        return changeLogRepository.save(log);
    }

    /**
     * Log a move action (e.g., task moved between statuses)
     */
    public ChangeLog logMove(String entityType, Long entityId, Long userId, 
                              String fromLocation, String toLocation) {
        ChangeLog log = ChangeLog.create(entityType, entityId, userId, ACTION_MOVE);
        log.setFieldName("location");
        log.setOldValue(fromLocation);
        log.setNewValue(toLocation);
        logger.info("Logging MOVE: {} #{} from '{}' to '{}' by user {}", 
                   entityType, entityId, fromLocation, toLocation, userId);
        return changeLogRepository.save(log);
    }

    /**
     * Get change history for an entity
     */
    public List<ChangeLogDto> getEntityHistory(String entityType, Long entityId) {
        List<ChangeLog> logs = changeLogRepository.findTop50ByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            entityType, entityId);
        return enrichWithUsernames(logs);
    }

    /**
     * Get change history for a task
     */
    public List<ChangeLogDto> getTaskHistory(Long taskId) {
        return getEntityHistory(ENTITY_TASK, taskId);
    }

    /**
     * Get change history for a board
     */
    public List<ChangeLogDto> getBoardHistory(Long boardId) {
        return getEntityHistory(ENTITY_BOARD, boardId);
    }

    /**
     * Get user's recent activity
     */
    public List<ChangeLogDto> getUserActivity(Long userId) {
        List<ChangeLog> logs = changeLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return enrichWithUsernames(logs);
    }

    /**
     * Convert to DTO and enrich with usernames
     */
    private List<ChangeLogDto> enrichWithUsernames(List<ChangeLog> logs) {
        // Collect all unique user IDs
        List<Long> userIds = logs.stream()
            .map(ChangeLog::getUserId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());

        // Batch fetch users
        Map<Long, String> usernames = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, User::getUsername));

        // Convert to DTOs with usernames
        return logs.stream()
            .map(log -> {
                ChangeLogDto dto = new ChangeLogDto();
                dto.setId(log.getId());
                dto.setEntityType(log.getEntityType());
                dto.setEntityId(log.getEntityId());
                dto.setUserId(log.getUserId());
                dto.setUsername(log.getUserId() != null ? usernames.get(log.getUserId()) : "System");
                dto.setAction(log.getAction());
                dto.setFieldName(log.getFieldName());
                dto.setOldValue(log.getOldValue());
                dto.setNewValue(log.getNewValue());
                dto.setCreatedAt(log.getCreatedAt());
                return dto;
            })
            .collect(Collectors.toList());
    }
}

