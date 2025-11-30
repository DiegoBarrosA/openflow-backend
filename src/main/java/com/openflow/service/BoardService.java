package com.openflow.service;

import com.openflow.dto.BoardDto;
import com.openflow.model.Board;
import com.openflow.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardService {
    @Autowired
    private BoardRepository boardRepository;

    private BoardDto toDto(Board board) {
        return new BoardDto(
            board.getId(),
            board.getName(),
            board.getDescription(),
            board.getUserId()
        );
    }

    private Board toEntity(BoardDto dto) {
        Board board = new Board();
        board.setId(dto.getId());
        board.setName(dto.getName());
        board.setDescription(dto.getDescription());
        board.setUserId(dto.getUserId());
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
        return boardRepository.save(board);
    }

    public Board updateBoard(Long id, Board updatedBoard, Long userId) {
        Board existingBoard = getBoardById(id, userId);
        existingBoard.setName(updatedBoard.getName());
        existingBoard.setDescription(updatedBoard.getDescription());
        return boardRepository.save(existingBoard);
    }

    public void deleteBoard(Long id, Long userId) {
        Board board = getBoardById(id, userId);
        boardRepository.delete(board);
    }
}
