package com.scheduling.order.watcher;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

/**
 * PickedFile 영속 — TK-01-3-2.
 *
 * <p>{@link #existsRecentSuccessByHash} = 24h 중복 검출 hot path. 부분 인덱스
 * {@code idx_picked_file_duplicate_window} 적중.
 */
public interface PickedFileRepository extends JpaRepository<PickedFile, UUID> {

    /**
     * 같은 해시 + 최근 윈도우 내 INGESTED / PROCESSING 존재 여부.
     */
    @Query("""
        SELECT COUNT(p) > 0 FROM PickedFile p
        WHERE p.fileHash = :hash
          AND p.status IN (com.scheduling.order.watcher.PickedFileStatus.INGESTED,
                           com.scheduling.order.watcher.PickedFileStatus.PROCESSING)
          AND p.discoveredAt >= :threshold
        """)
    boolean existsRecentSuccessByHash(@Param("hash") String hash,
                                       @Param("threshold") Instant threshold);
}
