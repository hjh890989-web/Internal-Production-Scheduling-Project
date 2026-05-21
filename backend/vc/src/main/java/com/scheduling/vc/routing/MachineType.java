package com.scheduling.vc.routing;

/**
 * 가류기 유형 — TK-05-4-1.
 *
 * <p>{@code com.scheduling.master.vc.MachineType} 과 동일 의미. vc 모듈 routing 패키지의
 * cross-module 회피를 위한 local enum (Modulith 경계 — master.vc 직접 import 불가).
 * 정책 prioritize() 출력으로 사용.
 */
public enum MachineType {
    LP,
    IC
}
