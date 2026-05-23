/**
 * 사업 KPI 영속 모듈 — EP-47 (KPI-001~019).
 *
 * <p>NS-S01~S09 (보조 KPI) + K-V01~06 (성형) + K-E01~06 (압출) 일별 측정값 영속 +
 * Grafana 대시보드 조회 (REQ-NF-KPI-001~019).
 *
 * <p>의존: common (만). 다른 도메인 모듈에 의존 안 함 — KPI 는 외부 source (스케줄러 /
 * 운영 백오피스) 가 record API 호출.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Business KPI",
    allowedDependencies = { "common" }
)
package com.scheduling.kpi;
