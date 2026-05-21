package com.scheduling.master.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {

    @Query("""
        SELECT h FROM Holiday h
        WHERE EXTRACT(YEAR FROM h.holidayDate) = :year
        ORDER BY h.holidayDate
        """)
    List<Holiday> findByYear(@Param("year") int year);

    @Query("SELECT h.holidayDate FROM Holiday h")
    List<LocalDate> findAllHolidayDates();
}
