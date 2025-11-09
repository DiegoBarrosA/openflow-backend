package com.openflow.repository;

import com.openflow.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {
    List<Status> findByBoardIdOrderByOrderAsc(Long boardId);
    void deleteByBoardId(Long boardId);
}

