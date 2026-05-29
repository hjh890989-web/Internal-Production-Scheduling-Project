package com.scheduling.master.line;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineProductCompatibilityRepository
    extends JpaRepository<LineProductCompatibility, LineProductCompatibility.PK> {

    List<LineProductCompatibility> findByHoseId(String hoseId);

    List<LineProductCompatibility> findByHoseIdAndFordOnlyTrue(String hoseId);

    List<LineProductCompatibility> findByLineId(String lineId);

    void deleteByLineId(String lineId);
}
