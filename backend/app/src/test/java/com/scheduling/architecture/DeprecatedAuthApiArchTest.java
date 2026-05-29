package com.scheduling.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Sprint 22 ST-SEC-1 TK-SEC-1-4 — Spring Security 6.4+ deprecated 인증 API 참조 0 강제.
 *
 * <p>Spring Boot 3.5 (Spring Security 6.5) 에서 {@link DaoAuthenticationProvider} 의
 * no-arg 생성자 + {@code setUserDetailsService(UserDetailsService)} 가 deprecated.
 * 권장 패턴은 생성자 주입 {@code new DaoAuthenticationProvider(userDetailsService)}.
 *
 * <p>본 테스트가 GREEN 이면 {@code com.scheduling.SecurityConfig#authenticationManager} 가
 * deprecation 경고 없이 구성됨 (Phase 4 EP-SEC-HARDEN carry-over 해소).
 *
 * @see com.scheduling.SecurityConfig
 */
@AnalyzeClasses(
    packages = "com.scheduling",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class DeprecatedAuthApiArchTest {

    @ArchTest
    static final ArchRule no_no_arg_dao_provider_constructor = noClasses()
        .should().callConstructor(DaoAuthenticationProvider.class)
        .because("Spring Security 6.4+ — no-arg DaoAuthenticationProvider() 는 deprecated. "
            + "new DaoAuthenticationProvider(userDetailsService) 생성자 주입 사용 (ST-SEC-1)");

    @ArchTest
    static final ArchRule no_set_user_details_service = noClasses()
        .should().callMethod(DaoAuthenticationProvider.class, "setUserDetailsService", UserDetailsService.class)
        .because("Spring Security 6.4+ — setUserDetailsService 는 deprecated. "
            + "생성자 주입으로 대체 (ST-SEC-1)");
}
