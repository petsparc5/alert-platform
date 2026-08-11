package com.petsparc5.alerts.normalization;

import com.petsparc5.alerts.persistence.entity.Event;

/**
 * Contract for mapping a source-specific deserialized raw payload into a
 * canonical {@link Event}.
 *
 * @param <T> the type of the deserialized raw payload
 */
public interface EventNormalizer<T> {

    /**
     * Maps a deserialized raw payload into a canonical event.
     *
     * @param raw        the deserialized raw payload
     * @param rawPayload the original raw payload JSON, preserved on the canonical event
     * @return the canonical event derived from the raw payload
     */
    Event normalize(T raw, String rawPayload);
}
