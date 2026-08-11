package com.petsparc5.alerts.persistence.repository;

import com.petsparc5.alerts.persistence.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for {@link Event} entities.
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Finds an event by its deduplication key.
     *
     * @param dedupKey the {@code source:externalId} deduplication key
     * @return the matching event, if any
     */
    Optional<Event> findByDedupKey(String dedupKey);

    /**
     * Reports whether an event with the given deduplication key exists.
     *
     * @param dedupKey the {@code source:externalId} deduplication key
     * @return {@code true} if such an event is already persisted
     */
    boolean existsByDedupKey(String dedupKey);
}
