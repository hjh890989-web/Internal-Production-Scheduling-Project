package com.scheduling.master.line;

import com.scheduling.master.api.LineRoutingLookup;
import com.scheduling.master.api.LineTypeSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link LineRoutingLookup} 구현 — TK-14-1-1.
 */
@Component
@Profile("with-infra")
class LineRoutingLookupImpl implements LineRoutingLookup {

    private final LineTypeRepository lineRepo;
    private final LineProductCompatibilityRepository compatRepo;

    LineRoutingLookupImpl(LineTypeRepository lineRepo,
                          LineProductCompatibilityRepository compatRepo) {
        this.lineRepo = lineRepo;
        this.compatRepo = compatRepo;
    }

    @Override
    public List<LineTypeSummary> findAllActive() {
        return lineRepo.findByActiveTrueOrderByPriorityAsc().stream()
            .map(l -> new LineTypeSummary(l.getLineId(), l.getLineType(),
                l.getPriority(), l.isActive(), l.getDescription()))
            .toList();
    }

    @Override
    public boolean isFordOnly(String hoseId) {
        return !compatRepo.findByHoseIdAndFordOnlyTrue(hoseId).isEmpty();
    }

    @Override
    public List<String> findCompatibleLineIds(String hoseId) {
        return compatRepo.findByHoseId(hoseId).stream()
            .map(LineProductCompatibility::getLineId).toList();
    }
}
