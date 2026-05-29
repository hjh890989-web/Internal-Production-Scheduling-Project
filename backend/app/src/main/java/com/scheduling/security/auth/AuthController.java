package com.scheduling.security.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static com.scheduling.security.RoleConstants.ROLE_PREFIX;

/**
 * Sprint 10 EP-AUTH 사번+PIN 로그인 REST endpoint (ST-AUTH-4, NFR-SEC-007).
 *
 * <p>POST /api/v1/auth/login (사번 8자리 + PIN 4자리) → 200 + JWT 토큰.
 *
 * <p>응답 코드:
 * <ul>
 *   <li>200 OK — 토큰 발급 (8h 유효)</li>
 *   <li>400 Bad Request — empId/pin validation 실패 (regex)</li>
 *   <li>401 Unauthorized — 사번 미존재 또는 PIN 불일치 (5회 누적 잠금 적용)</li>
 *   <li>423 Locked — 5회 실패 누적 → 10분 잠금 활성</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final PinPolicyService pinPolicyService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          LoginAttemptService loginAttemptService,
                          PinPolicyService pinPolicyService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.pinPolicyService = pinPolicyService;
    }

    public record LoginRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{8}$", message = "사번 8자리 숫자") String employeeId,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$", message = "PIN 4자리 숫자") String pin
    ) {}

    /** Sprint 22 ST-SEC-2 — {@code pinExpired} 추가 (true 시 프론트가 강제 변경 화면 노출). */
    public record LoginResponse(String token, String employeeId, String role,
                                Instant expiresAt, boolean pinExpired) {}

    public record ChangePinRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{4}$", message = "현재 PIN 4자리 숫자") String currentPin,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$", message = "새 PIN 4자리 숫자") String newPin
    ) {}

    @PostMapping("/login")
    @PreAuthorize("permitAll()")    // ArchUnit PreAuthorizeArchTest 정합 — anonymous 도달 명시
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.employeeId(), req.pin()));

            // 성공 — 카운터 reset + JWT 발급
            loginAttemptService.recordSuccess(req.employeeId());

            String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .findFirst()
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .orElse("UNKNOWN");

            JwtService.TokenResult result = jwtService.generate(req.employeeId(), role);

            // Sprint 22 ST-SEC-2 — PIN 30일 만료 시 pinExpired=true (프론트 강제 변경 화면)
            boolean pinExpired = pinPolicyService.isPinExpired(req.employeeId());

            log.info("EP-AUTH login success — employee_id={} role={} pinExpired={}",
                req.employeeId(), role, pinExpired);
            return ResponseEntity.ok(new LoginResponse(
                result.token(), req.employeeId(), role, result.expiresAt(), pinExpired));

        } catch (LockedException e) {
            log.warn("EP-AUTH login locked — employee_id={}", req.employeeId());
            return problem(HttpStatus.LOCKED, "계정 잠금 — 5회 실패 후 10분 잠금 적용");
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            // 실패 카운터 증가 (5회 도달 시 자동 잠금 — 다음 요청부터 423)
            loginAttemptService.recordFailure(req.employeeId());
            log.info("EP-AUTH login failed — employee_id={}", req.employeeId());
            return problem(HttpStatus.UNAUTHORIZED, "사번 또는 PIN 불일치");
        }
    }

    /**
     * Sprint 22 ST-SEC-2/4 — 인증된 본인 PIN 변경 (강제 변경 화면 / 자가 변경).
     *
     * <p>현재 PIN 재인증 후 새 PIN 적용 + last_pin_change_at = now (30일 clock reset).
     * 사번은 JWT principal (sub) 에서 추출 — body 사번 위조 방지.
     *
     * <ul>
     *   <li>200 OK — 변경 완료 (이후 로그인 pinExpired=false)</li>
     *   <li>401 — 현재 PIN 불일치</li>
     *   <li>423 — 5회 실패 누적 잠금</li>
     * </ul>
     */
    @PostMapping("/change-pin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePin(@RequestBody @Valid ChangePinRequest req,
                                       java.security.Principal principal) {
        String employeeId = principal.getName();
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(employeeId, req.currentPin()));
        } catch (LockedException e) {
            return problem(HttpStatus.LOCKED, "계정 잠금 — 5회 실패 후 10분 잠금 적용");
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            loginAttemptService.recordFailure(employeeId);
            return problem(HttpStatus.UNAUTHORIZED, "현재 PIN 불일치");
        }
        pinPolicyService.changeOwnPin(employeeId, req.newPin());
        loginAttemptService.recordSuccess(employeeId);
        log.info("EP-SEC-HARDEN change-pin success — employee_id={}", employeeId);
        return ResponseEntity.ok().build();
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle("로그인 실패");
        return ResponseEntity.status(status).body(pd);
    }
}
