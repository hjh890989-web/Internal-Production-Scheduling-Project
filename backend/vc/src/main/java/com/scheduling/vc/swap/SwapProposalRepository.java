package com.scheduling.vc.swap;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SwapProposalRepository extends JpaRepository<SwapProposal, UUID> {

    List<SwapProposal> findByStatus(SwapStatus status);

    List<SwapProposal> findByProposedBy(String proposedBy);
}
