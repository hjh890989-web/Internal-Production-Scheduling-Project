package com.scheduling.vc.capacity_overflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CapacityOverflowRequestRepository extends JpaRepository<CapacityOverflowRequest, UUID> {

    List<CapacityOverflowRequest> findByStatusOrderByPriorityRankAscRequestedAtAsc(CapacityOverflowRequest.Status status);

    List<CapacityOverflowRequest> findByHoseIdAndStatus(String hoseId, CapacityOverflowRequest.Status status);

    /** Sprint 9 EP-V12-Auto-Expire — PENDING + requested_at < threshold 조회 (auto-reject 대상). */
    List<CapacityOverflowRequest> findByStatusAndRequestedAtBefore(
        CapacityOverflowRequest.Status status, Instant threshold);
}
