package com.scheduling.vc.capacity_overflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CapacityOverflowRequestRepository extends JpaRepository<CapacityOverflowRequest, UUID> {

    List<CapacityOverflowRequest> findByStatusOrderByPriorityRankAscRequestedAtAsc(CapacityOverflowRequest.Status status);

    List<CapacityOverflowRequest> findByHoseIdAndStatus(String hoseId, CapacityOverflowRequest.Status status);
}
