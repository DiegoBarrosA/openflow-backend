package com.openflow.repository;

import com.openflow.model.ChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {
    List<ChangeLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
    
    List<ChangeLog> findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(String entityType, List<Long> entityIds);
    
    List<ChangeLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<ChangeLog> findTop50ByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
}

