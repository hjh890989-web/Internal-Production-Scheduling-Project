package com.scheduling.order.api;

import com.scheduling.order.domain.DuplicateOrderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Order 도메인 예외 → HTTP 응답 변환 — TK-02-1-1.
 *
 * @see DuplicateOrderResponse
 */
@RestControllerAdvice(basePackages = "com.scheduling.order")
public class OrderExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderExceptionHandler.class);

    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<DuplicateOrderResponse> handleDuplicate(DuplicateOrderException e) {
        log.warn("Duplicate order rejected: hose={} delivery={} version={}",
            e.getHoseId(), e.getDeliveryDate(), e.getMasterVersion());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            new DuplicateOrderResponse(
                "DUPLICATE_ORDER",
                e.getMessage(),
                e.getHoseId(),
                e.getDeliveryDate().toString(),
                e.getMasterVersion()
            )
        );
    }
}
