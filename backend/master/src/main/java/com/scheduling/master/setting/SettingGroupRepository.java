package com.scheduling.master.setting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettingGroupRepository extends JpaRepository<SettingGroup, Short> {

    List<SettingGroup> findAllByOrderByGroupNumberAsc();
}
