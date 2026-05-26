package com.scheduling.security.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Sprint 10 EP-AUTH AppUser Repository (NFR-SEC-007).
 *
 * <p>{@code employee_id} 가 PK 이므로 {@code findById} 와 {@code findByEmployeeId} 동일하지만,
 * {@link AppUserDetailsService#loadUserByUsername} 가 username 기반 lookup 의미를 명시적으로 표현.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByEmployeeId(String employeeId);
}
