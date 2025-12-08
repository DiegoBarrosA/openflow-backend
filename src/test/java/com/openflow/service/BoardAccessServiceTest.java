package com.openflow.service;

import com.openflow.dto.BoardAccessDto;
import com.openflow.model.AccessLevel;
import com.openflow.model.Board;
import com.openflow.model.BoardAccess;
import com.openflow.model.User;
import com.openflow.repository.BoardAccessRepository;
import com.openflow.repository.BoardRepository;
import com.openflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardAccessServiceTest {

    @Mock
    private BoardAccessRepository boardAccessRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChangeLogService changeLogService;

    @InjectMocks
    private BoardAccessService boardAccessService;

    private Board testBoard;
    private User boardOwner;
    private User testUser;
    private BoardAccess testAccess;

    @BeforeEach
    void setUp() {
        boardOwner = new User();
        boardOwner.setId(1L);
        boardOwner.setUsername("owner");

        testUser = new User();
        testUser.setId(2L);
        testUser.setUsername("testuser");

        testBoard = new Board();
        testBoard.setId(1L);
        testBoard.setUserId(1L);
        testBoard.setName("Test Board");

        testAccess = new BoardAccess();
        testAccess.setId(1L);
        testAccess.setBoardId(1L);
        testAccess.setUserId(2L);
        testAccess.setAccessLevel(AccessLevel.READ);
        testAccess.setGrantedBy(1L);
        testAccess.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testGetBoardAccesses() {
        // Arrange
        Long boardId = 1L;
        Long requesterId = 1L;
        List<BoardAccess> accesses = Arrays.asList(testAccess);

        when(boardRepository.findById(boardId)).thenReturn(Optional.of(testBoard));
        when(boardAccessRepository.findByBoardId(boardId)).thenReturn(accesses);
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(boardOwner));

        // Act
        List<BoardAccessDto> result = boardAccessService.getBoardAccesses(boardId, requesterId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(AccessLevel.READ, result.get(0).getAccessLevel());
        verify(boardAccessRepository).findByBoardId(boardId);
    }

    @Test
    void testGrantAccess() {
        // Arrange
        Long boardId = 1L;
        Long userId = 2L;
        Long grantedBy = 1L;
        AccessLevel level = AccessLevel.WRITE;

        when(boardRepository.findById(boardId)).thenReturn(Optional.of(testBoard));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(grantedBy)).thenReturn(Optional.of(boardOwner));
        when(boardAccessRepository.existsByBoardIdAndUserId(boardId, userId)).thenReturn(false);
        when(boardAccessRepository.save(any(BoardAccess.class))).thenReturn(testAccess);

        // Act
        BoardAccessDto result = boardAccessService.grantAccess(boardId, userId, level, grantedBy);

        // Assert
        assertNotNull(result);
        verify(boardAccessRepository).save(any(BoardAccess.class));
        verify(changeLogService).logFieldChange(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void testRevokeAccess() {
        // Arrange
        Long boardId = 1L;
        Long userId = 2L;
        Long requesterId = 1L;

        when(boardRepository.findById(boardId)).thenReturn(Optional.of(testBoard));
        when(boardAccessRepository.findByBoardIdAndUserId(boardId, userId)).thenReturn(Optional.of(testAccess));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        boardAccessService.revokeAccess(boardId, userId, requesterId);

        // Assert
        verify(boardAccessRepository).delete(any(BoardAccess.class));
        verify(changeLogService).logFieldChange(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void testHasAccess_Owner() {
        // Arrange
        Long boardId = 1L;
        Long userId = 1L; // Owner

        when(boardRepository.findById(boardId)).thenReturn(Optional.of(testBoard));

        // Act
        boolean result = boardAccessService.hasAccess(boardId, userId, AccessLevel.ADMIN);

        // Assert
        assertTrue(result); // Owner always has access
    }

    @Test
    void testHasAccess_WithExplicitAccess() {
        // Arrange
        Long boardId = 1L;
        Long userId = 2L;

        when(boardRepository.findById(boardId)).thenReturn(Optional.of(testBoard));
        when(boardAccessRepository.findByBoardIdAndUserId(boardId, userId)).thenReturn(Optional.of(testAccess));

        // Act
        boolean result = boardAccessService.hasAccess(boardId, userId, AccessLevel.READ);

        // Assert
        assertTrue(result);
    }
}

