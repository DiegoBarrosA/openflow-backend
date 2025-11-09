package com.openflow.service;

import com.openflow.model.Status;
import com.openflow.repository.BoardRepository;
import com.openflow.repository.StatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
        
        return statusRepository.save(status);
    }

    public Status updateStatus(Long id, Status updatedStatus, Long userId) {
        Status existingStatus = getStatusById(id, userId);
        existingStatus.setName(updatedStatus.getName());
        if (updatedStatus.getColor() != null) {
            existingStatus.setColor(updatedStatus.getColor());
        }
        if (updatedStatus.getOrder() != null) {
            existingStatus.setOrder(updatedStatus.getOrder());
        }
        return statusRepository.save(existingStatus);
    }

    public void deleteStatus(Long id, Long userId) {
        Status status = getStatusById(id, userId);
        statusRepository.delete(status);
    }
}

