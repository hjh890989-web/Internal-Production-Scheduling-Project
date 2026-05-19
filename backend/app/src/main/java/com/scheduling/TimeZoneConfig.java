package com.scheduling;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * KST (Asia/Seoul) 시간 통일 — BR-X04 (TK-34-3-1).
 *
 * <p>3 layer 강제:
 * <ul>
 *   <li>JVM default — {@link TimeZone#setDefault(TimeZone)}</li>
 *   <li>System property — {@code user.timezone}</li>
 *   <li>{@link Clock} bean — 도메인 코드가 {@code Clock} 주입 받아 사용 (정적 {@code Instant.now()} 금지)</li>
 * </ul>
 *
 * <p>도메인 사용 패턴:
 * <pre>{@code
 *   @Service
 *   public class ScheduleService {
 *       private final Clock clock;
 *
 *       public Schedule confirm() {
 *           Instant now = Instant.now(clock);   // ← Clock 주입 (testable)
 *           return new Schedule(now);
 *       }
 *   }
 * }</pre>
 *
 * <p>{@link com.scheduling.architecture.KstTimezoneArchTest} 가 {@code Instant.now()} /
 * {@code LocalDate.now()} 등 정적 호출 0건을 강제.
 *
 * <p>본 클래스는 application 합성 패키지({@code com.scheduling}) 에 위치 — Spring Modulith
 * 도메인 모듈 (order·vc·ex·master·audit·notify·common) 과 별개의 launcher 구성.
 */
@Configuration
public class TimeZoneConfig {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(KST));
        System.setProperty("user.timezone", KST.getId());
    }

    /**
     * Application-wide {@link Clock} — 모든 도메인 코드가 본 bean 주입.
     * 테스트에서는 {@code Clock.fixed(...)} 로 override.
     */
    @Bean
    public Clock clock() {
        return Clock.system(KST);
    }
}
