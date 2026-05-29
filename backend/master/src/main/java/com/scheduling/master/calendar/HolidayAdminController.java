package com.scheduling.master.calendar;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.security.Principal;
import java.time.LocalDate;

/**
 * Sprint 21 ST-CRUD-5 Holiday 마스터 관리 Controller (IT_OPS 전용 write).
 *
 * <p>경로: {@code /api/v1/master/holidays} (복수형 — 기존 {@code HolidayController} /api/v1/master/holiday 와 충돌 없음).
 * GET 은 기존 {@code HolidayController} 유지.
 *
 * <p>BR-X02 audit — {@code @Auditable} via {@link HolidayAdminService}.
 * BR-X04 — LocalDate (timezone-free, KST 정합).
 *
 * @see HolidayAdminService
 * @see BR-X02
 * @see BR-X04
 */
@RestController
@RequestMapping("/api/v1/master/holidays")
public class HolidayAdminController {

    private final HolidayAdminService service;

    public HolidayAdminController(HolidayAdminService service) {
        this.service = service;
    }

    /** POST payload — date + name 필수, type·description 선택. */
    public record HolidayCreateRequest(
        @NotNull LocalDate date,
        @NotBlank @Size(max = 100) String name,
        HolidayType type,
        String description
    ) {}

    /** 응답 DTO — entity 직접 노출 방지. */
    public record HolidayAdminResponse(
        LocalDate holidayDate,
        String holidayName,
        HolidayType holidayType,
        String description,
        String createdBy
    ) {
        static HolidayAdminResponse from(Holiday h) {
            return new HolidayAdminResponse(h.getHolidayDate(), h.getHolidayName(),
                h.getHolidayType(), h.getDescription(), h.getCreatedBy());
        }
    }

    /**
     * 신규 휴일 추가.
     * <ul>
     *   <li>201 Created — 성공</li>
     *   <li>409 Conflict — 동일 날짜 중복</li>
     * </ul>
     *
     * @see BR-X02
     */
    @PostMapping
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> add(@RequestBody @Valid HolidayCreateRequest request,
                                 Principal principal) {
        try {
            HolidayType type = request.type() != null ? request.type() : HolidayType.LEGAL;
            Holiday saved = service.add(request.date(), request.name(), type,
                request.description(), actorOf(principal));
            return ResponseEntity.status(HttpStatus.CREATED).body(HolidayAdminResponse.from(saved));
        } catch (EntityExistsException e) {
            return problem(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /**
     * 휴일 삭제.
     * <ul>
     *   <li>204 No Content — 성공</li>
     *   <li>404 Not Found — 대상 날짜 미존재</li>
     * </ul>
     *
     * @see BR-X02
     */
    @DeleteMapping("/{date}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> remove(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            service.remove(date);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private static String actorOf(Principal principal) {
        return principal != null ? principal.getName() : "anonymousUser";
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle("HOLIDAY 관리 오류");
        return ResponseEntity.status(status).body(pd);
    }
}
