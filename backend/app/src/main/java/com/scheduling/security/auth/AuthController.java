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

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    public record LoginRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{8}$", message = "사번 8자리 숫자") String employeeId,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$", message = "PIN 4자리 숫자") String pin
    ) {}

    public record LoginResponse(String token, String employeeId, String role, Instant expiresAt) {}

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

            log.info("EP-AUTH login success — employee_id={} role={}", req.employeeId(), role);
            return ResponseEntity.ok(new LoginResponse(
                result.token(), req.employeeId(), role, result.expiresAt()));

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

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle("로그인 실패");
        return ResponseEntity.status(status).body(pd);
    }
}
