package com.scheduling.vc.conflict;

import com.scheduling.vc.allocator.AllocationConflict;

/**
 * 분류된 conflict — TK-VC15-1-1.
 *
 * <p>원본 {@link AllocationConflict} + {@link ConflictCategory} wrap.
 */
public record ClassifiedConflict(AllocationConflict conflict, ConflictCategory category) {

    public String hoseId() { return conflict.hoseId(); }
    public String reason() { return conflict.reason(); }
}
