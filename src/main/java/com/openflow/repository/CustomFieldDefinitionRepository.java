package com.openflow.repository;

import com.openflow.model.CustomFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, Long> {
    List<CustomFieldDefinition> findByBoardIdOrderByDisplayOrderAsc(Long boardId);
    
    void deleteByBoardId(Long boardId);
}

