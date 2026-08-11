package com.petsparc5.alerts.normalization.usgs;

import com.petsparc5.alerts.ingestion.usgs.dto.UsgsFeature;
import com.petsparc5.alerts.normalization.EventNormalizer;
import com.petsparc5.alerts.persistence.entity.Event;
import org.springframework.stereotype.Component;

/**
 * Maps a {@link UsgsFeature} to a canonical {@link Event} with category
 * {@code DISASTER}, type {@code EARTHQUAKE}, and severity equal to the
 * earthquake magnitude.
 */
@Component
public class UsgsEventNormalizer implements EventNormalizer<UsgsFeature> {

    private static final String SOURCE = "usgs";
    private static final String TYPE = "EARTHQUAKE";

    /**
     * Maps a USGS earthquake feature to a canonical event.
     *
     * @param raw        the deserialized USGS feature
     * @param rawPayload the original raw payload JSON, preserved on the canonical event
     * @return the canonical event derived from the feature
     */
    @Override
    public Event normalize(UsgsFeature raw, String rawPayload) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
