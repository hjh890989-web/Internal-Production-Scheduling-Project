package com.scheduling.master.calendar;

import com.scheduling.master.api.WorkingCalendar;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Holiday 마스터 REST — TK-06-1-1 (EP-06 ST-06-1, IT_OPS 단독 mutation).
 *
 * <p>{@code GET /api/v1/master/holiday[?year=2026]} — 인증된 모든 role.
 * <p>{@code POST / DELETE /api/v1/master/holiday/{date}} — ROLE_IT_OPS.
 *
 * <p>변경 시 {@link WorkingCalendar#reload()} 즉시 호출 — vc/ex 캐시 갱신.
 */
@RestController
@RequestMapping("/api/v1/master/holiday")
@Profile("with-infra")
public class HolidayController {

    private final HolidayRepository repository;
    private final WorkingCalendar calendar;

    public HolidayController(HolidayRepository repository, WorkingCalendar calendar) {
        this.repository = repository;
        this.calendar = calendar;
    }

    public record HolidayPayload(
        @NotNull LocalDate holidayDate,
        @NotBlank @Size(max = 100) String holidayName,
        @NotNull HolidayType holidayType,
        String description
    ) {}

    public record HolidayResponse(
        LocalDate holidayDate,
        String holidayName,
        HolidayType holidayType,
        String description,
        String createdBy
    ) {
        static HolidayResponse from(Holiday h) {
            return new HolidayResponse(h.getHolidayDate(), h.getHolidayName(),
                h.getHolidayType(), h.getDescription(), h.getCreatedBy());
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<HolidayResponse> list(@RequestParam(required = false) Integer year) {
        List<Holiday> rows = (year == null) ? repository.findAll() : repository.findByYear(year);
        return rows.stream().map(HolidayResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<HolidayResponse> add(@Valid @RequestBody HolidayPayload payload,
                                               java.security.Principal principal) {
        Holiday saved = repository.save(new Holiday(
            payload.holidayDate(), payload.holidayName(), payload.holidayType(),
            payload.description(), principal.getName()
        ));
        calendar.reload();
        return ResponseEntity.status(HttpStatus.CREATED).body(HolidayResponse.from(saved));
    }

    @DeleteMapping("/{date}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<Void> remove(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (!repository.existsById(date)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(date);
        calendar.reload();
        return ResponseEntity.noContent().build();
    }
}
