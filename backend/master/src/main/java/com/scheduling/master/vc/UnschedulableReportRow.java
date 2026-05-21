package com.scheduling.master.vc;

import java.time.LocalDate;

/**
 * Unschedulable 리포트 row — TK-04-2-2.
 *
 * <p>{@link UnschedulableFilterService} 의 결과 hose_id 목록을 caller (EP-05)
 * 가 Order 도메인에서 enrich 하여 본 record 로 변환 → {@link UnschedulableReportGenerator} 호출.
 *
 * <p>Modulith 경계 — master 모듈은 order 도메인 모름 → caller 가 책임.
 *
 * @param hoseId        품번 (BR-V11 unschedulable)
 * @param deliveryDate  납기일
 * @param qty           수량
 * @param customer      거래처
 */
public record UnschedulableReportRow(
    String hoseId,
    LocalDate deliveryDate,
    int qty,
    String customer
) {
}
