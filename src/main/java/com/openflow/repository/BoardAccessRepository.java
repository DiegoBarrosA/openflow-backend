package com.openflow.repository;

import com.openflow.model.BoardAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardAccessRepository extends JpaRepository<BoardAccess, Long> {
    List<BoardAccess> findByBoardId(Long boardId);
    List<BoardAccess> findByUserId(Long userId);
    Optional<BoardAccess> findByBoardIdAndUserId(Long boardId, Long userId);
    boolean existsByBoardIdAndUserId(Long boardId, Long userId);
    void deleteByBoardIdAndUserId(Long boardId, Long userId);
}

