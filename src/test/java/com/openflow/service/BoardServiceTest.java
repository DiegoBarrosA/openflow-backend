package com.openflow.service;

import com.openflow.dto.BoardDto;
import com.openflow.model.AccessLevel;
import com.openflow.model.Board;
import com.openflow.repository.BoardRepository;
import com.openflow.repository.CustomFieldDefinitionRepository;
import com.openflow.repository.StatusRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BoardService.
 * Covers board module test cases: BOARD-01 to BOARD-05.
 */
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private CustomFieldDefinitionRepository customFieldDefinitionRepository;

    @Mock
    private ChangeLogService changeLogService;

    @Mock
    private BoardAccessService boardAccessService;

    @InjectMocks
    private BoardService boardService;

    private Board testBoard;
    private Board publicBoard;
    private Long ownerId = 1L;
    private Long otherUserId = 2L;

    @BeforeEach
    void setUp() {
        testBoard = new Board();
        testBoard.setId(1L);
        testBoard.setName("Test Board");
        testBoard.setDescription("Test Description");
        testBoard.setUserId(ownerId);
        testBoard.setIsPublic(false);
        testBoard.setIsTemplate(false);
        testBoard.setCreatedAt(LocalDateTime.now());

        publicBoard = new Board();
        publicBoard.setId(2L);
        publicBoard.setName("Public Board");
        publicBoard.setDescription("A public board");
        publicBoard.setUserId(ownerId);
        publicBoard.setIsPublic(true);
        publicBoard.setIsTemplate(false);
    }

    /**
     * BOARD-01: Test successful board creation.
     */
    @Test
    void testCreateBoard() {
        // Arrange
        Board newBoard = new Board();
        newBoard.setName("New Board");
        newBoard.setDescription("New Description");

        when(boardRepository.save(any(Board.class))).thenAnswer(invocation -> {
            Board board = invocation.getArgument(0);
            board.setId(3L);
            return board;
        });
        doNothing().when(changeLogService).logCreate(anyString(), anyLong(), anyLong());

        // Act
        Board result = boardService.createBoard(newBoard, ownerId);

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("New Board", result.getName());
        assertEquals(ownerId, result.getUserId());
        verify(boardRepository).save(any(Board.class));
        verify(changeLogService).logCreate(eq(ChangeLogService.ENTITY_BOARD), eq(3L), eq(ownerId));
    }

    /**
     * BOARD-01 (variant): Test creating board with DTO.
     */
    @Test
    void testCreateBoardDto() {
        // Arrange
        BoardDto boardDto = new BoardDto();
        boardDto.setName("DTO Board");
        boardDto.setDescription("Created via DTO");
        boardDto.setIsPublic(false);

        when(boardRepository.save(any(Board.class))).thenAnswer(invocation -> {
            Board board = invocation.getArgument(0);
            board.setId(4L);
            return board;
        });
        doNothing().when(changeLogService).logCreate(anyString(), anyLong(), anyLong());

        // Act
        BoardDto result = boardService.createBoardDto(boardDto, ownerId);

        // Assert
        assertNotNull(result);
        assertEquals(4L, result.getId());
        assertEquals("DTO Board", result.getName());
        assertEquals(ownerId, result.getUserId());
    }

    /**
     * BOARD-02: Test successful board update.
     */
    @Test
    void testUpdateBoard() {
        // Arrange
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardAccessService.hasAccess(anyLong(), anyLong(), any(AccessLevel.class))).thenReturn(true);
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        doNothing().when(changeLogService).logFieldChange(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString());

        Board updatedBoard = new Board();
        updatedBoard.setName("Updated Board Name");
        updatedBoard.setDescription("Updated Description");
        updatedBoard.setIsPublic(true);

        // Act
        Board result = boardService.updateBoard(1L, updatedBoard, ownerId);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Board Name", result.getName());
        verify(changeLogService).logFieldChange(eq(ChangeLogService.ENTITY_BOARD), eq(1L), eq(ownerId), 
            eq("name"), eq("Test Board"), eq("Updated Board Name"));
        verify(boardRepository).save(any(Board.class));
    }

    /**
     * BOARD-02 (variant): Test update board with DTO.
     */
    @Test
    void testUpdateBoardDto() {
        // Arrange
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardAccessService.hasAccess(anyLong(), anyLong(), any(AccessLevel.class))).thenReturn(true);
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        doNothing().when(changeLogService).logFieldChange(anyString(), anyLong(), anyLong(), anyString(), anyString(), anyString());

        BoardDto updateDto = new BoardDto();
        updateDto.setName("Updated via DTO");
        updateDto.setDescription("New description");

        // Act
        BoardDto result = boardService.updateBoardDto(1L, updateDto, ownerId);

        // Assert
        assertNotNull(result);
        verify(boardRepository).save(any(Board.class));
    }

    /**
     * BOARD-03: Test successful board deletion.
     */
    @Test
    void testDeleteBoard() {
        // Arrange
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardAccessService.hasAccess(anyLong(), anyLong(), any(AccessLevel.class))).thenReturn(true);
        doNothing().when(changeLogService).logDelete(anyString(), anyLong(), anyLong());
        doNothing().when(boardRepository).delete(any(Board.class));

        // Act
        boardService.deleteBoard(1L, ownerId);

        // Assert
        verify(changeLogService).logDelete(eq(ChangeLogService.ENTITY_BOARD), eq(1L), eq(ownerId));
        verify(boardRepository).delete(testBoard);
    }

    /**
     * BOARD-03 (variant): Test delete board not found.
     */
    @Test
    void testDeleteBoard_NotFound() {
        // Arrange
        when(boardRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            boardService.deleteBoard(999L, ownerId);
        });
        verify(boardRepository, never()).delete(any(Board.class));
    }

    /**
     * BOARD-04: Test get board access level - owner.
     */
    @Test
    void testGetBoardAccessLevel_Owner() {
        // Arrange
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));

        // Act
        String accessLevel = boardService.getBoardAccessLevel(1L, ownerId);

        // Assert
        assertEquals("OWNER", accessLevel);
    }

    /**
     * BOARD-04 (variant): Test get board access level - shared user.
     */
    @Test
    void testGetBoardAccessLevel_SharedUser() {
        // Arrange
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardAccessService.getUserAccessLevel(1L, otherUserId)).thenReturn("WRITE");

        // Act
        String accessLevel = boardService.getBoardAccessLevel(1L, otherUserId);

        // Assert
        assertEquals("WRITE", accessLevel);
    }

    /**
     * BOARD-04 (variant): Test isBoardOwner.
     */
    @Test
    void testIsBoardOwner() {
        // Arrange
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));

        // Act & Assert
        assertTrue(boardService.isBoardOwner(1L, ownerId));
        assertFalse(boardService.isBoardOwner(1L, otherUserId));
    }

    /**
     * BOARD-05: Test get all public boards.
     */
    @Test
    void testGetAllPublicBoards() {
        // Arrange
        List<Board> publicBoards = Arrays.asList(publicBoard);
        when(boardRepository.findByIsPublicTrue()).thenReturn(publicBoards);

        // Act
        List<BoardDto> result = boardService.getAllPublicBoardsDto();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Public Board", result.get(0).getName());
        assertTrue(result.get(0).getIsPublic());
        verify(boardRepository).findByIsPublicTrue();
    }

    /**
     * BOARD-05 (variant): Test get public board by ID.
     */
    @Test
    void testGetPublicBoardById() {
        // Arrange
        when(boardRepository.findByIdAndIsPublicTrue(2L)).thenReturn(Optional.of(publicBoard));

        // Act
        BoardDto result = boardService.getPublicBoardByIdDto(2L);

        // Assert
        assertNotNull(result);
        assertEquals("Public Board", result.getName());
        assertTrue(result.getIsPublic());
    }

    /**
     * BOARD-05 (variant): Test get public board by ID - not found.
     */
    @Test
    void testGetPublicBoardById_NotFound() {
        // Arrange
        when(boardRepository.findByIdAndIsPublicTrue(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            boardService.getPublicBoardByIdDto(999L);
        });
    }

    /**
     * Test getBoardById - owner access.
     */
    @Test
    void testGetBoardById_OwnerAccess() {
        // Arrange
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));

        // Act
        Board result = boardService.getBoardById(1L, ownerId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Board", result.getName());
    }

    /**
     * Test getBoardById - shared user access.
     */
    @Test
    void testGetBoardById_SharedUserAccess() {
        // Arrange
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardAccessService.hasAccess(1L, otherUserId, AccessLevel.READ)).thenReturn(true);

        // Act
        Board result = boardService.getBoardById(1L, otherUserId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Board", result.getName());
    }

    /**
     * Test getBoardById - unauthorized access.
     */
    @Test
    void testGetBoardById_UnauthorizedAccess() {
        // Arrange
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(boardAccessService.hasAccess(1L, otherUserId, AccessLevel.READ)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            boardService.getBoardById(1L, otherUserId);
        });
    }

    /**
     * Test getAllBoardsByUserId.
     */
    @Test
    void testGetAllBoardsByUserId() {
        // Arrange
        List<Board> userBoards = Arrays.asList(testBoard);
        when(boardAccessService.getUserAccessibleBoards(ownerId)).thenReturn(userBoards);

        // Act
        List<Board> result = boardService.getAllBoardsByUserId(ownerId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Board", result.get(0).getName());
    }
}

