package com.scheduling.master.shift;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, String> {

    List<Shift> findAllByOrderBySortOrderAsc();
}
