package com.openflow.service;

import com.openflow.dto.BoardDto;
import com.openflow.model.Board;
import com.openflow.model.CustomFieldDefinition;
import com.openflow.model.Status;
import com.openflow.repository.BoardRepository;
import com.openflow.repository.CustomFieldDefinitionRepository;
import com.openflow.repository.StatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardService {
    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private CustomFieldDefinitionRepository customFieldDefinitionRepository;

    @Autowired
    @Lazy
    private ChangeLogService changeLogService;

    private BoardDto toDto(Board board) {
        return new BoardDto(
            board.getId(),
            board.getName(),
            board.getDescription(),
            board.getUserId(),
            board.getIsPublic(),
            board.getIsTemplate()
        );
    }

    private Board toEntity(BoardDto dto) {
        Board board = new Board();
        board.setId(dto.getId());
        board.setName(dto.getName());
        board.setDescription(dto.getDescription());
        board.setUserId(dto.getUserId());
        board.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false);
        board.setIsTemplate(dto.getIsTemplate() != null ? dto.getIsTemplate() : false);
        return board;
    }

    public List<BoardDto> getAllBoardsByUserIdDto(Long userId) {
        return getAllBoardsByUserId(userId).stream().map(this::toDto).toList();
    }

    public BoardDto getBoardByIdDto(Long id, Long userId) {
        return toDto(getBoardById(id, userId));
    }

    public BoardDto createBoardDto(BoardDto boardDto, Long userId) {
        Board board = toEntity(boardDto);
        Board created = createBoard(board, userId);
        return toDto(created);
    }

    public BoardDto updateBoardDto(Long id, BoardDto boardDto, Long userId) {
        Board updated = updateBoard(id, toEntity(boardDto), userId);
        return toDto(updated);
    }

    public List<Board> getAllBoardsByUserId(Long userId) {
        return boardRepository.findByUserId(userId);
    }

    public Board getBoardById(Long id, Long userId) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        if (!board.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to board");
        }
        return board;
    }

    public Board createBoard(Board board, Long userId) {
        board.setUserId(userId);
        Board saved = boardRepository.save(board);
        
        // Log creation
        changeLogService.logCreate(ChangeLogService.ENTITY_BOARD, saved.getId(), userId);
        
        return saved;
    }

    public Board updateBoard(Long id, Board updatedBoard, Long userId) {
        Board existingBoard = getBoardById(id, userId);
        
        // Log field changes
        if (!existingBoard.getName().equals(updatedBoard.getName())) {
            changeLogService.logFieldChange(ChangeLogService.ENTITY_BOARD, id, userId,
                "name", existingBoard.getName(), updatedBoard.getName());
        }
        existingBoard.setName(updatedBoard.getName());
        
        if (updatedBoard.getDescription() != null && 
            !String.valueOf(updatedBoard.getDescription()).equals(String.valueOf(existingBoard.getDescription()))) {
            changeLogService.logFieldChange(ChangeLogService.ENTITY_BOARD, id, userId,
                "description", existingBoard.getDescription(), updatedBoard.getDescription());
        }
        existingBoard.setDescription(updatedBoard.getDescription());
        
        Boolean newIsPublic = updatedBoard.getIsPublic() != null ? updatedBoard.getIsPublic() : existingBoard.getIsPublic();
        if (!newIsPublic.equals(existingBoard.getIsPublic())) {
            changeLogService.logFieldChange(ChangeLogService.ENTITY_BOARD, id, userId,
                "isPublic", String.valueOf(existingBoard.getIsPublic()), String.valueOf(newIsPublic));
        }
        existingBoard.setIsPublic(newIsPublic);
        
        Boolean newIsTemplate = updatedBoard.getIsTemplate() != null ? updatedBoard.getIsTemplate() : existingBoard.getIsTemplate();
        if (newIsTemplate == null) newIsTemplate = false;
        if (!newIsTemplate.equals(existingBoard.getIsTemplate())) {
            changeLogService.logFieldChange(ChangeLogService.ENTITY_BOARD, id, userId,
                "isTemplate", String.valueOf(existingBoard.getIsTemplate()), String.valueOf(newIsTemplate));
        }
        existingBoard.setIsTemplate(newIsTemplate);
        
        return boardRepository.save(existingBoard);
    }

    public void deleteBoard(Long id, Long userId) {
        Board board = getBoardById(id, userId);
        
        // Log deletion before deleting
        changeLogService.logDelete(ChangeLogService.ENTITY_BOARD, id, userId);
        
        boardRepository.delete(board);
    }

    // Public board methods for anonymous access
    public List<BoardDto> getAllPublicBoardsDto() {
        return boardRepository.findByIsPublicTrue().stream().map(this::toDto).toList();
    }

    public BoardDto getPublicBoardByIdDto(Long id) {
        Board board = boardRepository.findByIdAndIsPublicTrue(id)
                .orElseThrow(() -> new RuntimeException("Public board not found"));
        return toDto(board);
    }

    public Board getPublicBoardById(Long id) {
        return boardRepository.findByIdAndIsPublicTrue(id)
                .orElseThrow(() -> new RuntimeException("Public board not found"));
    }

    // Template methods
    public List<BoardDto> getTemplatesDto(Long userId) {
        return boardRepository.findByUserIdAndIsTemplateTrue(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Create a new board from a template.
     * Copies statuses and custom field definitions from the template.
     */
    @Transactional
    public BoardDto createBoardFromTemplate(Long templateId, String newBoardName, Long userId) {
        Board template = getBoardById(templateId, userId);
        
        if (template.getIsTemplate() == null || !template.getIsTemplate()) {
            throw new RuntimeException("Source board is not a template");
        }
        
        // Create new board
        Board newBoard = new Board();
        newBoard.setName(newBoardName);
        newBoard.setDescription(template.getDescription());
        newBoard.setUserId(userId);
        newBoard.setIsPublic(false);
        newBoard.setIsTemplate(false);
        
        Board savedBoard = boardRepository.save(newBoard);
        
        // Log creation
        changeLogService.logCreate(ChangeLogService.ENTITY_BOARD, savedBoard.getId(), userId);
        
        // Copy statuses
        List<Status> templateStatuses = statusRepository.findByBoardIdOrderByOrderAsc(templateId);
        for (Status status : templateStatuses) {
            Status newStatus = new Status();
            newStatus.setName(status.getName());
            newStatus.setColor(status.getColor());
            newStatus.setBoardId(savedBoard.getId());
            newStatus.setOrder(status.getOrder());
            statusRepository.save(newStatus);
        }
        
        // Copy custom field definitions
        List<CustomFieldDefinition> templateFields = customFieldDefinitionRepository.findByBoardIdOrderByDisplayOrderAsc(templateId);
        for (CustomFieldDefinition field : templateFields) {
            CustomFieldDefinition newField = new CustomFieldDefinition();
            newField.setName(field.getName());
            newField.setFieldType(field.getFieldType());
            newField.setOptions(field.getOptions());
            newField.setDisplayOrder(field.getDisplayOrder());
            newField.setIsRequired(field.getIsRequired());
            newField.setBoardId(savedBoard.getId());
            customFieldDefinitionRepository.save(newField);
        }
        
        return toDto(savedBoard);
    }
}
