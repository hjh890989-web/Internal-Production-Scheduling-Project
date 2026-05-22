package com.scheduling.master.setting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSettingGroupRepository
    extends JpaRepository<ProductSettingGroup, ProductSettingGroup.PK> {

    List<ProductSettingGroup> findByHoseId(String hoseId);

    List<ProductSettingGroup> findByGroupNumber(short groupNumber);
}
