package com.openflow.repository;

import com.openflow.model.CustomFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomFieldValueRepository extends JpaRepository<CustomFieldValue, Long> {
    List<CustomFieldValue> findByTaskId(Long taskId);
    
    Optional<CustomFieldValue> findByTaskIdAndFieldDefinitionId(Long taskId, Long fieldDefinitionId);
    
    void deleteByTaskId(Long taskId);
    
    void deleteByFieldDefinitionId(Long fieldDefinitionId);
}

