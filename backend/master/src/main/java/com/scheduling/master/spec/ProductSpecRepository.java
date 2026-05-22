package com.scheduling.master.spec;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSpecRepository extends JpaRepository<ProductSpec, String> {

    List<ProductSpec> findByIsSpecLt7True();
}
