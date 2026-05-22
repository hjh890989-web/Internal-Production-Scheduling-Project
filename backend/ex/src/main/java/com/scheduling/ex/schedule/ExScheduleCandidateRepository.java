package com.scheduling.ex.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExScheduleCandidateRepository extends JpaRepository<ExScheduleCandidate, UUID> {

    Optional<ExScheduleCandidate> findByVcRowId(UUID vcRowId);

    List<ExScheduleCandidate> findByScheduleId(UUID scheduleId);

    List<ExScheduleCandidate> findByHoseIdAndExtrusionDeadlineBetween(
        String hoseId, LocalDate from, LocalDate to);

    long countByStatus(CandidateStatus status);
}
