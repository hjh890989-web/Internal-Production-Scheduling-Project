package com.scheduling.order.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 표준 Order 스키마 (commit 전 staging DTO) — TK-01-2-1.
 *
 * <p>SRS §6.2.4 ORDER 엔티티 6 필드 매핑 대상:
 * <ol>
 *   <li>{@code orderId} — 매핑 단계 UUID 발급, ST-01-2 commit 후 DB sequence</li>
 *   <li>{@code hoseId} — 대문자·하이픈·숫자 정규식 ({@code ^[A-Z0-9 -]+$})</li>
 *   <li>{@code deliveryDate} — KST LocalDate (BR-X04)</li>
 *   <li>{@code qty} — 양수 정수</li>
 *   <li>{@code orderType} — 4 enum (FORECAST·WEEKLY·KD·CONFIRMED)</li>
 *   <li>{@code customer} — 기본값 "내수" (없으면 자동 fallback)</li>
 * </ol>
 *
 * <p>Jakarta Validation 강제 — controller / service layer 의 @Valid 적용 시점에 검증.
 * 본 record 의 canonical constructor 는 기본값(customer="내수", orderId=random UUID) 적용.
 */
public record OrderDraft(
    UUID orderId,
    @NotBlank @Pattern(regexp = "^[A-Z0-9 -]+$") String hoseId,
    @NotNull LocalDate deliveryDate,
    @Min(1) int qty,
    @NotNull OrderType orderType,
    @NotBlank String customer
) {
    public OrderDraft {
        if (orderId == null) {
            orderId = UUID.randomUUID();
        }
        if (customer == null || customer.isBlank()) {
            customer = "내수";
        }
    }

    /** 기존 마스터 Order → OrderDraft 변환 (TK-02-2-1 PrecedenceResolver 가 동질 비교). */
    public static OrderDraft fromExisting(Order order) {
        return new OrderDraft(
            order.getOrderId(),
            order.getHoseId(),
            order.getDeliveryDate(),
            order.getQty(),
            order.getOrderType(),
            order.getCustomer()
        );
    }
}
