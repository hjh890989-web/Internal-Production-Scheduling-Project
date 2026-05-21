package com.scheduling.master.vc;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VcHoseRuleRepository extends JpaRepository<VcHoseRule, String> {

    List<VcHoseRule> findByMachinePin(String machinePin);
}
