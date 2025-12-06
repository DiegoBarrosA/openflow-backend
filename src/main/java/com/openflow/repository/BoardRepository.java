package com.openflow.repository;

import com.openflow.model.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByUserId(Long userId);
    
    List<Board> findByIsPublicTrue();
    
    Optional<Board> findByIdAndIsPublicTrue(Long id);
    
    List<Board> findByUserIdAndIsTemplateTrue(Long userId);
}

