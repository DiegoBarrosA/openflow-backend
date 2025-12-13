package com.openflow.repository;

import com.openflow.model.CustomFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomFieldValueRepository extends JpaRepository<CustomFieldValue, Long> {
    List<CustomFieldValue> findByTaskId(Long taskId);
    
    Optional<CustomFieldValue> findByTaskIdAndFieldDefinitionId(Long taskId, Long fieldDefinitionId);
    
    @Modifying
    @Query("DELETE FROM CustomFieldValue v WHERE v.taskId = :taskId")
    void deleteByTaskId(Long taskId);
    
    @Modifying
    @Query("DELETE FROM CustomFieldValue v WHERE v.fieldDefinitionId = :fieldDefinitionId")
    void deleteByFieldDefinitionId(Long fieldDefinitionId);
}

