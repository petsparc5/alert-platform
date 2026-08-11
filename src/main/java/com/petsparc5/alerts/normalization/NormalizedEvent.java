package com.petsparc5.alerts.normalization;

import com.petsparc5.alerts.persistence.entity.AlertCategory;

import java.time.OffsetDateTime;

/**
 * Wire representation of a canonical event published to the {@code events}
 * topic, decoupled from the {@link com.petsparc5.alerts.persistence.entity.Event}
 * persistence entity.
 *
 * @param dedupKey   the {@code source:externalId} deduplication key
 * @param category   the top-level alert category
 * @param type       the source-specific event type (e.g. {@code EARTHQUAKE})
 * @param source     the originating source identifier
 * @param severity   the event severity, if applicable
 * @param occurredAt the time the event occurred at the source
 * @param payload    the original raw payload serialized as JSON
 */
public record NormalizedEvent(String dedupKey, AlertCategory category, String type, String source,
                               Double severity, OffsetDateTime occurredAt, String payload) {
}
