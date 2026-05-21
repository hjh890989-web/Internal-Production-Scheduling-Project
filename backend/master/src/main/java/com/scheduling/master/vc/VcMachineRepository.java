package com.scheduling.master.vc;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * VcMachine 영속 — TK-05-1-1 (BR-V05).
 *
 * <p>master 모듈 내부 Repository. 다른 모듈 (vc 등) 은 {@link com.scheduling.master.api.VcMachineQuery}
 * facade 를 통해 접근 — Modulith 경계 준수.
 */
public interface VcMachineRepository extends JpaRepository<VcMachine, String> {

    List<VcMachine> findByActive(boolean active);
}
