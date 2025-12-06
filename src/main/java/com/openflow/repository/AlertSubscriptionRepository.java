package com.openflow.repository;

import com.openflow.model.AlertSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertSubscriptionRepository extends JpaRepository<AlertSubscription, Long> {
    List<AlertSubscription> findByEntityTypeAndEntityId(String entityType, Long entityId);
    
    List<AlertSubscription> findByUserId(Long userId);
    
    Optional<AlertSubscription> findByUserIdAndEntityTypeAndEntityId(Long userId, String entityType, Long entityId);
    
    boolean existsByUserIdAndEntityTypeAndEntityId(Long userId, String entityType, Long entityId);
    
    void deleteByUserIdAndEntityTypeAndEntityId(Long userId, String entityType, Long entityId);
    
    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);
}

