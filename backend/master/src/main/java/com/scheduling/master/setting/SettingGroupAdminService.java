package com.scheduling.master.setting;

import com.scheduling.audit.aop.Auditable;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sprint 21 ST-CRUD-2 — SETTING_GROUP 관리 Service (IT_OPS 전용).
 *
 * <p>{@code @Auditable} BR-X02 — 모든 mutation audit_log 기록.
 * setting_group_id 범위 1~8 강제 (BR-V12·BR-V13 cross-reference).
 * product_setting_group 연결 row 존재 시 비활성화 안내 메시지 포함 (보존).
 *
 * @see BR-X02
 * @see BR-V12
 * @see BR-V13
 */
@Service
public class SettingGroupAdminService {

    private static final Logger log = LoggerFactory.getLogger(SettingGroupAdminService.class);

    static final short GROUP_MIN = 1;
    static final short GROUP_MAX = 8;

    private final SettingGroupRepository repository;
    private final ProductSettingGroupRepository productSettingGroupRepository;

    public SettingGroupAdminService(SettingGroupRepository repository,
                                    ProductSettingGroupRepository productSettingGroupRepository) {
        this.repository = repository;
        this.productSettingGroupRepository = productSettingGroupRepository;
    }

    /** read — 모든 role 허용, groupNumber 오름차순. */
    public List<SettingGroup> list() {
        return repository.findAllByOrderByGroupNumberAsc();
    }

    /**
     * POST — 신규 그룹 생성 (IT_OPS only).
     * setting_group_id 범위 1~8 위반 시 {@link IllegalArgumentException} (→ 400).
     *
     * @see BR-V12
     */
    @Auditable("ST-CRUD-2 SETTING_GROUP 추가 (IT_OPS)")
    @Transactional
    public SettingGroup create(short groupNumber, String groupName, String description,
                               boolean active, String actor) {
        validateGroupNumber(groupNumber);
        if (repository.existsById(groupNumber)) {
            throw new EntityExistsException("setting_group_id 중복: " + groupNumber);
        }
        SettingGroup g = new SettingGroup(groupNumber, groupName, description, active, actor);
        log.info("ST-CRUD-2 setting_group create — groupNumber={} actor={}", groupNumber, actor);
        return repository.save(g);
    }

    /**
     * PUT — display_name(groupName) 및 active 수정 (IT_OPS only).
     * setting_group_id 범위 1~8 위반 시 400.
     */
    @Auditable("ST-CRUD-2 SETTING_GROUP 수정 (IT_OPS)")
    @Transactional
    public SettingGroup update(short groupNumber, String groupName, boolean active, String actor) {
        validateGroupNumber(groupNumber);
        SettingGroup g = repository.findById(groupNumber)
            .orElseThrow(() -> new EntityNotFoundException("setting_group_id 미존재: " + groupNumber));
        g.updateDisplayName(groupName, actor);
        if (!active) {
            g.deactivate(actor);
        }
        log.info("ST-CRUD-2 setting_group update — groupNumber={} active={} actor={}", groupNumber, active, actor);
        return repository.save(g);
    }

    /**
     * DELETE — 비활성 toggle (active=false). 물리 삭제 없음.
     * product_setting_group 연결 row 존재 시 안내 포함 후 비활성 처리 (보존).
     *
     * @see BR-V12
     * @see BR-V13
     */
    @Auditable("ST-CRUD-2 SETTING_GROUP 비활성 (IT_OPS)")
    @Transactional
    public DeactivateResult deactivate(short groupNumber, String actor) {
        validateGroupNumber(groupNumber);
        SettingGroup g = repository.findById(groupNumber)
            .orElseThrow(() -> new EntityNotFoundException("setting_group_id 미존재: " + groupNumber));
        boolean hasLinks = !productSettingGroupRepository.findByGroupNumber(groupNumber).isEmpty();
        g.deactivate(actor);
        repository.save(g);
        log.info("ST-CRUD-2 setting_group deactivate — groupNumber={} hasLinks={} actor={}",
            groupNumber, hasLinks, actor);
        return new DeactivateResult(groupNumber, hasLinks);
    }

    /** @throws IllegalArgumentException groupNumber ∉ [1,8] (→ HTTP 400) */
    private static void validateGroupNumber(short groupNumber) {
        if (groupNumber < GROUP_MIN || groupNumber > GROUP_MAX) {
            throw new IllegalArgumentException(
                "setting_group_id 범위 위반 (1~8): " + groupNumber + " (BR-V12·V13)");
        }
    }

    /**
     * 비활성 결과 VO — {@code hasLinks=true} 시 컨트롤러가 안내 메시지 포함.
     *
     * @param groupNumber 비활성 처리된 그룹 번호
     * @param hasLinks    product_setting_group 연결 row 존재 여부
     */
    public record DeactivateResult(short groupNumber, boolean hasLinks) {}
}
