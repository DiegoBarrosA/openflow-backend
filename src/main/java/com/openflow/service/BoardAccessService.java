package com.openflow.service;

import com.openflow.dto.BoardAccessDto;
import com.openflow.model.AccessLevel;
import com.openflow.model.Board;
import com.openflow.model.BoardAccess;
import com.openflow.model.User;
import com.openflow.repository.BoardAccessRepository;
import com.openflow.repository.BoardRepository;
import com.openflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BoardAccessService {
    @Autowired
    private BoardAccessRepository boardAccessRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private ChangeLogService changeLogService;

    private BoardAccessDto toDto(BoardAccess access) {
        String username = userRepository.findById(access.getUserId())
                .map(User::getUsername)
                .orElse("Unknown");
        String grantedByUsername = userRepository.findById(access.getGrantedBy())
                .map(User::getUsername)
                .orElse("Unknown");
        return new BoardAccessDto(
            access.getId(),
            access.getBoardId(),
            access.getUserId(),
            username,
            access.getAccessLevel(),
            access.getGrantedBy(),
            grantedByUsername,
            access.getCreatedAt()
        );
    }

    /**
     * Get all users with access to a board (excluding the owner).
     */
    public List<BoardAccessDto> getBoardAccesses(Long boardId, Long requesterId) {
        // Verify requester is board owner or has ADMIN access
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        
        if (!board.getUserId().equals(requesterId) && 
            !hasAccess(boardId, requesterId, AccessLevel.ADMIN)) {
            throw new RuntimeException("Unauthorized: Only board owner or users with ADMIN access can view access list");
        }

        List<BoardAccess> accesses = boardAccessRepository.findByBoardId(boardId);
        return accesses.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Grant access to a user.
     * Only board owner or users with ADMIN access can grant access.
     */
    @Transactional
    public BoardAccessDto grantAccess(Long boardId, Long userId, AccessLevel level, Long grantedBy) {
        // Verify board exists
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify grantedBy is board owner or has ADMIN access
        if (!board.getUserId().equals(grantedBy) && 
            !hasAccess(boardId, grantedBy, AccessLevel.ADMIN)) {
            throw new RuntimeException("Unauthorized: Only board owner or users with ADMIN access can grant access");
        }
        
        // Prevent granting access to board owner
        if (board.getUserId().equals(userId)) {
            throw new RuntimeException("Cannot grant access to board owner");
        }
        
        // Check if access already exists
        if (boardAccessRepository.existsByBoardIdAndUserId(boardId, userId)) {
            throw new RuntimeException("User already has access to this board");
        }
        
        BoardAccess access = new BoardAccess();
        access.setBoardId(boardId);
        access.setUserId(userId);
        access.setAccessLevel(level);
        access.setGrantedBy(grantedBy);
        
        BoardAccess saved = boardAccessRepository.save(access);
        
        // Log access grant
        changeLogService.logFieldChange(ChangeLogService.ENTITY_BOARD, boardId, grantedBy,
            "access", "granted", level.name() + " to " + user.getUsername());
        
        return toDto(saved);
    }

    /**
     * Revoke access from a user.
     * Only board owner or users with ADMIN access can revoke access.
     */
    @Transactional
    public void revokeAccess(Long boardId, Long userId, Long requesterId) {
        // Verify board exists
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        
        // Verify requester is board owner or has ADMIN access
        if (!board.getUserId().equals(requesterId) && 
            !hasAccess(boardId, requesterId, AccessLevel.ADMIN)) {
            throw new RuntimeException("Unauthorized: Only board owner or users with ADMIN access can revoke access");
        }
        
        // Prevent revoking access from board owner
        if (board.getUserId().equals(userId)) {
            throw new RuntimeException("Cannot revoke access from board owner");
        }
        
        BoardAccess access = boardAccessRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new RuntimeException("Access not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Log access revocation
        changeLogService.logFieldChange(ChangeLogService.ENTITY_BOARD, boardId, requesterId,
            "access", "revoked", access.getAccessLevel().name() + " from " + user.getUsername());
        
        boardAccessRepository.delete(access);
    }

    /**
     * Update access level for a user.
     * Only board owner or users with ADMIN access can update access levels.
     */
    @Transactional
    public BoardAccessDto updateAccessLevel(Long boardId, Long userId, AccessLevel level, Long requesterId) {
        // Verify board exists
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("Board not found"));
        
        // Verify requester is board owner or has ADMIN access
        if (!board.getUserId().equals(requesterId) && 
            !hasAccess(boardId, requesterId, AccessLevel.ADMIN)) {
            throw new RuntimeException("Unauthorized: Only board owner or users with ADMIN access can update access levels");
        }
        
        BoardAccess access = boardAccessRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new RuntimeException("Access not found"));
        
        AccessLevel oldLevel = access.getAccessLevel();
        access.setAccessLevel(level);
        BoardAccess saved = boardAccessRepository.save(access);
        
        // Log access level change
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        changeLogService.logFieldChange(ChangeLogService.ENTITY_BOARD, boardId, requesterId,
            "access", oldLevel.name(), level.name() + " for " + user.getUsername());
        
        return toDto(saved);
    }

    /**
     * Check if a user has the required access level to a board.
     * Returns true if:
     * - User is the board owner (always has full access)
     * - User has explicit access at the required level or higher
     */
    public boolean hasAccess(Long boardId, Long userId, AccessLevel requiredLevel) {
        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null) return false;
        
        // Board owner always has full access
        if (board.getUserId().equals(userId)) {
            return true;
        }
        
        // Check explicit access
        BoardAccess access = boardAccessRepository.findByBoardIdAndUserId(boardId, userId)
                .orElse(null);
        
        if (access == null) return false;
        
        // Check if access level meets requirement
        return access.getAccessLevel().ordinal() >= requiredLevel.ordinal();
    }

    /**
     * Get all boards a user can access (owned + shared).
     */
    public List<Board> getUserAccessibleBoards(Long userId) {
        // Get owned boards
        List<Board> ownedBoards = boardRepository.findByUserId(userId);
        
        // Get shared boards (where user has explicit access)
        List<BoardAccess> accesses = boardAccessRepository.findByUserId(userId);
        List<Board> sharedBoards = accesses.stream()
                .map(access -> boardRepository.findById(access.getBoardId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());
        
        // Combine and remove duplicates
        List<Board> allBoards = new java.util.ArrayList<>(ownedBoards);
        for (Board shared : sharedBoards) {
            if (!allBoards.stream().anyMatch(b -> b.getId().equals(shared.getId()))) {
                allBoards.add(shared);
            }
        }
        
        return allBoards;
    }

    /**
     * Get user's access level to a board (without requiring admin permissions).
     * Returns: "ADMIN", "WRITE", "READ", or null if no access.
     */
    public String getUserAccessLevel(Long boardId, Long userId) {
        BoardAccess access = boardAccessRepository.findByBoardIdAndUserId(boardId, userId)
                .orElse(null);
        
        if (access == null) return null;
        return access.getAccessLevel().name();
    }
}

