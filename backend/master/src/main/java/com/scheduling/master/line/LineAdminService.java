package com.scheduling.master.line;

import com.scheduling.audit.aop.Auditable;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Sprint 21 ST-CRUD-4 LINE_TYPE + LINE_PRODUCT_COMPATIBILITY 관리 Service (IT_OPS 전용).
 *
 * <p>{@code @Auditable} BR-X02 — 모든 mutation audit_log 기록.
 * 비활성 처리 시 기존 schedule 의존 row 보존 (active=false toggle만 수행).
 *
 * @see BR-X02
 */
@Service
public class LineAdminService {

    private static final Logger log = LoggerFactory.getLogger(LineAdminService.class);

    private final LineTypeRepository lineTypeRepository;
    private final LineProductCompatibilityRepository compatibilityRepository;

    public LineAdminService(LineTypeRepository lineTypeRepository,
                            LineProductCompatibilityRepository compatibilityRepository) {
        this.lineTypeRepository = lineTypeRepository;
        this.compatibilityRepository = compatibilityRepository;
    }

    public List<LineType> list() {
        return lineTypeRepository.findAll();
    }

    @Auditable("ST-CRUD-4 LINE_TYPE 추가 (IT_OPS)")
    @Transactional
    public LineType create(String lineId, String lineType, short priority,
                           String description, String actor) {
        if (lineTypeRepository.existsById(lineId)) {
            throw new EntityExistsException("line_id 중복: " + lineId);
        }
        LineType entity = new LineType(lineId, lineType, priority, true, description, actor);
        log.info("ST-CRUD-4 line create — lineId={} lineType={}", lineId, lineType);
        return lineTypeRepository.save(entity);
    }

    @Auditable("ST-CRUD-4 LINE_TYPE 수정 (IT_OPS)")
    @Transactional
    public LineType update(String lineId, String lineType, short priority,
                           String description, String actor) {
        LineType entity = lineTypeRepository.findById(lineId)
            .orElseThrow(() -> new EntityNotFoundException("line_id 미존재: " + lineId));
        entity.update(lineType, priority, description, actor);
        log.info("ST-CRUD-4 line update — lineId={}", lineId);
        return lineTypeRepository.save(entity);
    }

    @Auditable("ST-CRUD-4 LINE_TYPE 비활성 (IT_OPS)")
    @Transactional
    public void deactivate(String lineId, String actor) {
        LineType entity = lineTypeRepository.findById(lineId)
            .orElseThrow(() -> new EntityNotFoundException("line_id 미존재: " + lineId));
        entity.deactivate(actor);
        lineTypeRepository.save(entity);
        log.info("ST-CRUD-4 line deactivate — lineId={}", lineId);
    }

    /**
     * product 호환 매핑 전체 교체 (Set&lt;hoseId&gt; replace).
     *
     * <p>기존 매핑 삭제 후 신규 hoseId set 으로 재삽입. fordOnly=false 기본값.
     *
     * @see BR-X02
     */
    @Auditable("ST-CRUD-4 LINE_PRODUCT_COMPATIBILITY 갱신 (IT_OPS)")
    @Transactional
    public List<LineProductCompatibility> replaceProducts(String lineId,
                                                          Set<String> hoseIds,
                                                          String actor) {
        if (!lineTypeRepository.existsById(lineId)) {
            throw new EntityNotFoundException("line_id 미존재: " + lineId);
        }
        compatibilityRepository.deleteByLineId(lineId);
        List<LineProductCompatibility> mappings = hoseIds.stream()
            .map(hoseId -> new LineProductCompatibility(hoseId, lineId, false, actor))
            .toList();
        List<LineProductCompatibility> saved = compatibilityRepository.saveAll(mappings);
        log.info("ST-CRUD-4 line products replace — lineId={} count={}", lineId, saved.size());
        return saved;
    }
}
