---
name: spring-security-keycloak-setup
description: Spring Security 6 + Keycloak 24 (SAML/OIDC) + local fallback + RBAC (이 프로젝트 사내 IdP).
---

# Spring Security + Keycloak Setup

본 프로젝트 (Spring Security 6 + Keycloak 24 + 사내 IdP) 의 인증·인가 표준. SAML/OIDC + local fallback 이중화.

## 인증 전략

| 환경 | 1차 | Fallback | 비고 |
|---|---|---|---|
| PROD | Keycloak (SAML → 사내 AD) | local DB | IdP down 시 운영자 ROLE 만 local 로그인 |
| STG | Keycloak (OIDC) | local DB | Sprint 0 부터 |
| Dev | local DB | — | Keycloak 컨테이너 선택적 |

## 의존성

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-saml2-service-provider")
}
```

## SecurityFilterChain

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final LocalUserDetailsService localUsers;
    private final JwtAuthConverter jwtConverter;

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/api/**", "/actuator/**")
            .cors(withDefaults())
            .csrf(c -> c.disable())                 // JWT/세션 분리
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasAuthority("ROLE_IT_OPS")
                .requestMatchers("/api/v1/master/**").hasAuthority("ROLE_IT_OPS")
                .requestMatchers(HttpMethod.POST, "/api/v1/schedule/**").hasAuthority("ROLE_PLANNER")  // confirm 포함, BR-X05 는 서비스 레이어에서
                .requestMatchers(HttpMethod.GET, "/api/v1/schedule/**").authenticated()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtConverter)))
            .build();
    }

    @Bean
    SecurityFilterChain webSso(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/login/**", "/logout", "/saml2/**")
            .saml2Login(withDefaults())
            .oauth2Login(withDefaults())
            .formLogin(f -> f.loginPage("/login/local")    // local fallback
                .successHandler(new SavedRequestAwareAuthenticationSuccessHandler()))
            .userDetailsService(localUsers)
            .build();
    }
}
```

## JWT → Authority 변환 (TK-30-2-1)

```java
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var realm = (Map<String, Object>) jwt.getClaim("realm_access");
        var roles = ((List<String>) realm.getOrDefault("roles", List.of()))
            .stream()
            .filter(Roles.VALID::contains)            // unknown role 차단
            .map(r -> "ROLE_" + r)
            .map(SimpleGrantedAuthority::new)
            .map(GrantedAuthority.class::cast)
            .toList();
        return new JwtAuthenticationToken(jwt, roles, jwt.getSubject());
    }
}
```

## application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: scheduling-app
            client-secret: ${KEYCLOAK_CLIENT_SECRET}
            scope: openid, profile, email, roles
            authorization-grant-type: authorization_code
            redirect-uri: '{baseUrl}/login/oauth2/code/{registrationId}'
        provider:
          keycloak:
            issuer-uri: https://idp.intra/realms/scheduling
            user-name-attribute: preferred_username
      resourceserver:
        jwt:
          issuer-uri: https://idp.intra/realms/scheduling
          jwk-set-uri: https://idp.intra/realms/scheduling/protocol/openid-connect/certs
      saml2:
        relyingparty:
          registration:
            scheduling-sp:
              entity-id: scheduling-sp
              assertingparty:
                metadata-uri: https://idp.intra/realms/scheduling/protocol/saml/descriptor
```

## RBAC (Role-Based Access Control)

Phase 2 EP-30 ST-30-2 (TK-30-2-1·2) 4 role 표준. Keycloak realm role → Spring `GrantedAuthority` 자동 매핑 (`ROLE_` prefix 적용).

| Keycloak role | Spring authority | 대상 | 권한 |
|---|---|---|---|
| `PLANNER` | `ROLE_PLANNER` | 생산계획팀 | 스케줄 작성·확정·override (BR-X01·X05·X07) |
| `STK_USER` | `ROLE_STK_USER` | 현장 STK 작업자 | 시뮬뷰 조회·제안 |
| `IT_OPS` | `ROLE_IT_OPS` | IT 운영팀 | 마스터·시스템 관리 + Actuator·Grafana |
| `READ_ONLY` | `ROLE_READ_ONLY` | 감사·임원 | 조회 전용 |

### Roles 상수 (TK-30-2-1)

```java
public final class Roles {
    public static final String PLANNER    = "PLANNER";
    public static final String STK_USER   = "STK_USER";
    public static final String IT_OPS     = "IT_OPS";
    public static final String READ_ONLY  = "READ_ONLY";

    public static final Set<String> VALID = Set.of(PLANNER, STK_USER, IT_OPS, READ_ONLY);
    private Roles() {}
}
```

### Dual-review (BR-X05) — Role 분리 아닌 작성자 ≠ 승인자

BR-X05 는 별도 `APPROVER` role 이 아닌, **같은 `PLANNER` role 내에서 작성자 ID ≠ 승인자 ID** 강제.

```java
@Service
public class ScheduleConfirmService {

    @PreAuthorize("hasAuthority('ROLE_PLANNER')")
    @BR("X05")
    public void confirm(UUID id, Authentication auth) {
        var schedule = repo.findById(id).orElseThrow();
        if (schedule.createdBy().equals(auth.getName())) {
            throw new BusinessException(BrCode.X05, "작성자는 본인 작업 승인 불가");
        }
        schedule.confirm(auth.getName(), clock.instant());
        repo.save(schedule);
    }
}
```

### Endpoint 표준 (TK-30-2-2)

```java
.authorizeHttpRequests(a -> a
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/actuator/prometheus", "/actuator/**").hasAuthority("ROLE_IT_OPS")
    .requestMatchers("/api/v1/master/**").hasAuthority("ROLE_IT_OPS")
    .requestMatchers(HttpMethod.POST, "/api/v1/schedule/**/confirm").hasAuthority("ROLE_PLANNER")
    .requestMatchers(HttpMethod.POST, "/api/v1/schedule/**").hasAuthority("ROLE_PLANNER")
    .requestMatchers(HttpMethod.GET, "/api/v1/schedule/**").authenticated()
    .anyRequest().authenticated())
```

## Local Fallback

```java
@Service
@RequiredArgsConstructor
public class LocalUserDetailsService implements UserDetailsService {

    private final LocalUserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) {
        var user = repo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(username));
        if (!user.isLocalFallbackEnabled()) {
            throw new DisabledException("Local fallback disabled — use Keycloak");
        }
        return User.builder()
            .username(user.username())
            .password(user.passwordHash())  // BCrypt
            .authorities(user.roles().stream().map(SimpleGrantedAuthority::new).toList())
            .build();
    }
}
```

**중요** — Local 사용자는 운영자 등 소수 한정. Keycloak 정상 운영 시 사용 금지.

## CORS

```java
@Bean
CorsConfigurationSource corsConfig() {
    var cfg = new CorsConfiguration();
    cfg.setAllowedOriginPatterns(List.of("https://scheduling.intra"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cfg);
    return source;
}
```

## Audit (BR-X02)

```java
@Component
@RequiredArgsConstructor
public class AuthAuditListener {

    private final AuditLogger logger;

    @EventListener
    public void onLogin(AuthenticationSuccessEvent e) {
        logger.log(AuditAction.LOGIN, e.getAuthentication().getName());
    }

    @EventListener
    public void onFail(AbstractAuthenticationFailureEvent e) {
        logger.log(AuditAction.LOGIN_FAIL, e.getAuthentication().getName());
    }
}
```

## Anti-patterns
- `csrf().disable()` 를 form login 에도 적용 (CSRF 취약)
- JWT 검증 없이 client 신뢰
- Local 비밀번호 평문 저장 — BCrypt 강제
- Role 을 hard-code (`ROLE_USER`) — Keycloak realm role 동기화
- 세션 + JWT 동시 운영 (`/api` 는 stateless, `/web` 은 session)
- `@PreAuthorize` 에 SpEL 만으로 BR — 도메인 서비스에 명시적 BR 가드 중복

## 참고
- Phase 2 EP-30 (Keycloak) — `Phase 2/4.Tasks/Tasks/EP-30/` (4 role 정의 TK-30-2-1·2)
- Phase 2 EP-34 ST-34-1 (Dual-review BR-X05) — `Phase 2/4.Tasks/Tasks/EP-34/ST-34-1/`
- BR-X02 audit 사양 — `Phase 2/1.SRS/`
- SRS REQ-FUNC-CO-001 · NFR-SEC-002·003 — RBAC 사양
