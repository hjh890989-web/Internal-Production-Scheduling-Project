package com.scheduling.master.kd;

import com.scheduling.master.api.KdOrderLookup;
import com.scheduling.master.api.KdOrderSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Profile("with-infra")
class KdOrderLookupImpl implements KdOrderLookup {

    private final KdOrderRepository repository;
    private final Clock clock;

    KdOrderLookupImpl(KdOrderRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public List<KdOrderSummary> findOpenByHose(String hoseId) {
        return repository.findOpenByHose(hoseId).stream().map(this::toSummary).toList();
    }

    @Override
    public List<KdOrderSummary> findOpenByHoseIn(List<String> hoseIds) {
        if (hoseIds == null || hoseIds.isEmpty()) return List.of();
        return repository.findOpenByHoseIn(hoseIds).stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional
    public int consume(UUID kdOrderId, int qty, String actor) {
        KdOrder k = repository.findById(kdOrderId)
            .orElseThrow(() -> new IllegalArgumentException("kd_order_id 미존재: " + kdOrderId));
        int actualConsumed = k.consume(qty, Instant.now(clock), actor);
        repository.save(k);
        return actualConsumed;
    }

    @Override
    public Map<String, Long> remainingByHose() {
        return repository.findRemainingSumByHose().stream()
            .collect(Collectors.toMap(
                KdOrderRepository.HoseRemainingProjection::getHoseId,
                p -> p.getTotalRemaining() == null ? 0L : p.getTotalRemaining()));
    }

    private KdOrderSummary toSummary(KdOrder k) {
        return new KdOrderSummary(
            k.getKdOrderId(), k.getHoseId(), k.getOrderQty(), k.getRemainingQty(),
            k.getOrderDate(), k.getStatus().name());
    }
}
