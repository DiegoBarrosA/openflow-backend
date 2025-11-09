package com.openflow.service;

import com.openflow.model.Board;
import com.openflow.repository.BoardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardService {
    @Autowired
    private BoardRepository boardRepository;

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

