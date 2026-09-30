package com.example.devoopsclass;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RollbackRequestRepository extends JpaRepository<RollbackRequest, UUID> {
    List<RollbackRequest> findTop20ByOrderByRequestedAtDesc();

    List<RollbackRequest> findTop20ByStatusInOrderByRequestedAtDesc(List<String> statuses);
}
