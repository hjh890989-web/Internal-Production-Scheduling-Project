package com.scheduling.security.auth;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Sprint 10 EP-AUTH 자체 JWT 발급/검증 service (ST-AUTH-4, NFR-SEC-007).
 *
 * <p>HS256 + 8h 유효기간. 클레임 — issuer / subject (사번) / role / iat / exp.
 *
 * <p>Spring Security oauth2-jose 의 {@link JwtEncoder} / {@link JwtDecoder} bean (SecurityConfig 등록)
 * 사용 — nimbus-jose-jwt transitively 의존성, 별도 jjwt 라이브러리 불필요.
 *
 * <p>{@link Clock} 주입 — ArchUnit KstTimezoneArchTest 정합 (BR-X04).
 */
@Service
public class JwtService {

    public static final Duration TOKEN_VALIDITY = Duration.ofHours(8);
    public static final String ISSUER = "scheduling-local";
    public static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;

    public JwtService(JwtEncoder encoder, JwtDecoder decoder, Clock clock) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.clock = clock;
    }

    /** HS256 토큰 발급 — subject = 사번, role claim 부착, iat/exp 자동. */
    public TokenResult generate(String employeeId, String role) {
        Instant now = clock.instant();
        Instant exp = now.plus(TOKEN_VALIDITY);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(employeeId)
            .issuedAt(now)
            .expiresAt(exp)
            .claim(ROLE_CLAIM, role)
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResult(token, exp);
    }

    /**
     * 토큰 검증 — HS256 서명 + exp + iat. 만료 시 {@code JwtValidationException},
     * 서명 불일치 시 {@code BadJwtException} ({@link org.springframework.security.oauth2.jwt.JwtException} 계열).
     */
    public Jwt parse(String token) {
        return decoder.decode(token);
    }

    public record TokenResult(String token, Instant expiresAt) {}
}
