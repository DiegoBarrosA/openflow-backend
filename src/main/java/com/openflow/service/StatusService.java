package com.openflow.service;

import com.openflow.dto.StatusDto;
import com.openflow.model.Status;
import com.openflow.repository.BoardRepository;
import com.openflow.repository.StatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatusService {
    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardService boardService;

    @Autowired
    @Lazy
    private ChangeLogService changeLogService;

    private StatusDto toDto(Status status) {
        return new StatusDto(
            status.getId(),
            status.getName(),
            status.getColor(),
            status.getBoardId(),
            status.getOrder()
        );
    }

    private Status toEntity(StatusDto dto) {
        Status status = new Status();
        status.setId(dto.getId());
        status.setName(dto.getName());
        status.setColor(dto.getColor());
        status.setBoardId(dto.getBoardId());
        status.setOrder(dto.getOrder());
        return status;
    }

    public List<StatusDto> getStatusesByBoardIdDto(Long boardId, Long userId) {
        return getStatusesByBoardId(boardId, userId).stream().map(this::toDto).toList();
    }

    public StatusDto getStatusByIdDto(Long id, Long userId) {
        return toDto(getStatusById(id, userId));
    }

    public StatusDto createStatusDto(StatusDto statusDto, Long userId) {
        Status status = toEntity(statusDto);
        Status created = createStatus(status, userId);
        return toDto(created);
    }

    public StatusDto updateStatusDto(Long id, StatusDto statusDto, Long userId) {
        Status updated = updateStatus(id, toEntity(statusDto), userId);
        return toDto(updated);
    }

    public List<Status> getStatusesByBoardId(Long boardId, Long userId) {
        boardService.getBoardById(boardId, userId); // Validate board access
        return statusRepository.findByBoardIdOrderByOrderAsc(boardId);
    }

    public Status getStatusById(Long id, Long userId) {
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Status not found"));
        boardService.getBoardById(status.getBoardId(), userId); // Validate board access
        return status;
    }

    public Status createStatus(Status status, Long userId) {
        boardService.getBoardById(status.getBoardId(), userId); // Validate board access
        
        // Set order if not provided
        if (status.getOrder() == null) {
            List<Status> existingStatuses = statusRepository.findByBoardIdOrderByOrderAsc(status.getBoardId());
            status.setOrder(existingStatuses.size());
        }
        
        Status saved = statusRepository.save(status);
        
        // Log creation
        changeLogService.logCreate(ChangeLogService.ENTITY_STATUS, saved.getId(), userId);
        
        return saved;
    }

    public Status updateStatus(Long id, Status updatedStatus, Long userId) {
        Status existingStatus = getStatusById(id, userId);
        
        // Log field changes
        if (!existingStatus.getName().equals(updatedStatus.getName())) {
            changeLogService.logFieldChange(ChangeLogService.ENTITY_STATUS, id, userId,
                "name", existingStatus.getName(), updatedStatus.getName());
        }
        existingStatus.setName(updatedStatus.getName());
        
        if (updatedStatus.getColor() != null && !updatedStatus.getColor().equals(existingStatus.getColor())) {
            changeLogService.logFieldChange(ChangeLogService.ENTITY_STATUS, id, userId,
                "color", existingStatus.getColor(), updatedStatus.getColor());
            existingStatus.setColor(updatedStatus.getColor());
        }
        
        if (updatedStatus.getOrder() != null && !updatedStatus.getOrder().equals(existingStatus.getOrder())) {
            changeLogService.logFieldChange(ChangeLogService.ENTITY_STATUS, id, userId,
                "order", String.valueOf(existingStatus.getOrder()), String.valueOf(updatedStatus.getOrder()));
            existingStatus.setOrder(updatedStatus.getOrder());
        }
        
        return statusRepository.save(existingStatus);
    }

    public void deleteStatus(Long id, Long userId) {
        Status status = getStatusById(id, userId);
        
        // Log deletion before deleting
        changeLogService.logDelete(ChangeLogService.ENTITY_STATUS, id, userId);
        
        statusRepository.delete(status);
    }

    /**
     * Reorder statuses for a board.
     * @param boardId The board ID
     * @param statusIds Ordered list of status IDs in their new order
     * @param userId The user ID for authorization
     * @return Updated list of statuses
     */
    public List<StatusDto> reorderStatuses(Long boardId, List<Long> statusIds, Long userId) {
        boardService.getBoardById(boardId, userId); // Validate board access
        
        List<Status> statuses = statusRepository.findByBoardIdOrderByOrderAsc(boardId);
        
        // Update order for each status
        for (int i = 0; i < statusIds.size(); i++) {
            Long statusId = statusIds.get(i);
            for (Status status : statuses) {
                if (status.getId().equals(statusId)) {
                    if (!status.getOrder().equals(i)) {
                        changeLogService.logFieldChange(ChangeLogService.ENTITY_STATUS, statusId, userId,
                            "order", String.valueOf(status.getOrder()), String.valueOf(i));
                        status.setOrder(i);
                        statusRepository.save(status);
                    }
                    break;
                }
            }
        }
        
        return getStatusesByBoardIdDto(boardId, userId);
    }

    /**
     * Get statuses for a public board (no user validation).
     */
    public List<StatusDto> getStatusesByBoardIdDtoPublic(Long boardId) {
        return statusRepository.findByBoardIdOrderByOrderAsc(boardId)
                .stream()
                .map(this::toDto)
                .toList();
    }
}