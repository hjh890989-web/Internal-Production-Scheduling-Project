package com.scheduling.master.setting;

import com.scheduling.master.api.HoseSettingGroupSummary;
import com.scheduling.master.api.SettingGroupLookup;
import com.scheduling.master.api.SettingGroupSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * {@link SettingGroupLookup} 구현 — TK-09-1-1.
 *
 * <p>Phase 2+ Caffeine 캐시 + LISTEN/NOTIFY 도입 — Sprint 3 단순 pass-through.
 */
@Component
@Profile("with-infra")
class SettingGroupLookupImpl implements SettingGroupLookup {

    private final SettingGroupRepository groupRepo;
    private final ProductSettingGroupRepository mappingRepo;

    SettingGroupLookupImpl(SettingGroupRepository groupRepo,
                            ProductSettingGroupRepository mappingRepo) {
        this.groupRepo = groupRepo;
        this.mappingRepo = mappingRepo;
    }

    @Override
    public List<SettingGroupSummary> findAllGroups() {
        return groupRepo.findAllByOrderByGroupNumberAsc().stream()
            .map(g -> new SettingGroupSummary(g.getGroupNumber(), g.getGroupName(), g.getDescription()))
            .toList();
    }

    @Override
    public List<HoseSettingGroupSummary> findGroupsForHose(String hoseId) {
        return mappingRepo.findByHoseId(hoseId).stream()
            .sorted(Comparator.<ProductSettingGroup>comparingInt(m -> m.isPrimaryGroup() ? 0 : 1)
                .thenComparingInt(ProductSettingGroup::getGroupNumber))
            .map(m -> new HoseSettingGroupSummary(m.getHoseId(), m.getGroupNumber(), m.isPrimaryGroup()))
            .toList();
    }

    @Override
    public Optional<HoseSettingGroupSummary> findPrimaryGroup(String hoseId) {
        return findGroupsForHose(hoseId).stream().findFirst();
    }

    @Override
    public List<HoseSettingGroupSummary> findHosesInGroup(short groupNumber) {
        return mappingRepo.findByGroupNumber(groupNumber).stream()
            .map(m -> new HoseSettingGroupSummary(m.getHoseId(), m.getGroupNumber(), m.isPrimaryGroup()))
            .toList();
    }
}
