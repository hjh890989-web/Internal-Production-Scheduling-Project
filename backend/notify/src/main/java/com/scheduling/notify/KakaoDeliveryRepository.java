package com.scheduling.notify;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KakaoDeliveryRepository extends JpaRepository<KakaoDeliveryAttempt, UUID> {
    List<KakaoDeliveryAttempt> findByNotificationIdOrderByAttemptedAtDesc(UUID notificationId);

    long countByStatus(KakaoDeliveryAttempt.Status status);
}
