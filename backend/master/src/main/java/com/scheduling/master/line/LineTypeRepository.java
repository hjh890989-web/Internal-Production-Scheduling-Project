package com.scheduling.master.line;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineTypeRepository extends JpaRepository<LineType, String> {

    List<LineType> findByActiveTrueOrderByPriorityAsc();
}
