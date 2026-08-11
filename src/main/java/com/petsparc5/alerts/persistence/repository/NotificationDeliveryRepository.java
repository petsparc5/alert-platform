package com.petsparc5.alerts.persistence.repository;

import com.petsparc5.alerts.persistence.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Data access for {@link NotificationDelivery} entities.
 */
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    /**
     * Finds all delivery records for a user.
     *
     * @param userId the recipient user's identifier
     * @return the user's delivery records
     */
    List<NotificationDelivery> findByUserId(UUID userId);
}
