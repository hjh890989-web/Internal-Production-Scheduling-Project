/**
 * 성형 가류 스케줄링 모듈 (PDD-02).
 *
 * 저압가류기 4대 + IC가류기 1대 슬롯·회전수·셋팅 그룹·합금형 제약.
 * BR-V07 (당일 락) · BR-V12~V17 (좌/우·호기·앵글상한·규격<7 제약).
 *
 * 의존: common, master::api (제약 조회), audit::events, order::events (수주 변경 구독).
 * 발행 이벤트: VcConfirmedEvent · VcChangedEvent (ex·notify 구독).
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "VC Scheduling",
    allowedDependencies = { "common", "master::api", "audit::events", "order::events" }
)
package com.scheduling.vc;
