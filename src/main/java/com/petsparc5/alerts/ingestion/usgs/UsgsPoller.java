package com.petsparc5.alerts.ingestion.usgs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsparc5.alerts.ingestion.RawEvent;
import com.petsparc5.alerts.ingestion.RawEventPublisher;
import com.petsparc5.alerts.ingestion.SourcePoller;
import com.petsparc5.alerts.ingestion.usgs.dto.UsgsFeature;
import com.petsparc5.alerts.ingestion.usgs.dto.UsgsFeatureCollection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled poller that fetches the USGS earthquake feed and publishes each
 * earthquake as a {@link RawEvent} to the {@code raw.usgs} topic, keyed by its
 * USGS event id.
 */
@Slf4j
@Component
public class UsgsPoller extends SourcePoller<UsgsFeatureCollection> {

    private static final String SOURCE = "usgs";

    private final ObjectMapper objectMapper;

    /**
     * Creates a poller wiring the feed client, publisher, and JSON mapper.
     *
     * @param feedClient        the client used to fetch the USGS feed
     * @param rawEventPublisher the publisher used to emit raw events
     * @param objectMapper      the mapper used to serialize raw payloads
     */
    public UsgsPoller(UsgsFeedClient feedClient,
                      RawEventPublisher rawEventPublisher,
                      ObjectMapper objectMapper) {
        super(feedClient, rawEventPublisher);
        this.objectMapper = objectMapper;
    }

    /**
     * Maps a {@link UsgsFeatureCollection} to one {@link RawEvent} per feature,
     * skipping features with a null id and serializing each valid feature as JSON.
     *
     * @param collection the deserialized USGS feature collection
     * @return the raw events derived from the feature collection
     */
    @Override
    protected List<RawEvent> toRawEvents(UsgsFeatureCollection collection) {
        return collection.features().stream()
                .filter(this::hasValidId)
                .map(this::toRawEvent)
                .toList();
    }

    /**
     * Fetches the USGS feed and publishes one raw event per valid earthquake feature.
     */
    @Scheduled(fixedDelayString = "${ingestion.usgs.poll-interval-ms}")
    public void poll() {
        safeExecute();
    }

    private boolean hasValidId(UsgsFeature feature) {
        if (feature.id() == null) {
            log.warn("Skipping USGS feature with null id");
            return false;
        }
        return true;
    }

    private RawEvent toRawEvent(UsgsFeature feature) {
        try {
            return new RawEvent(SOURCE, feature.id(), objectMapper.writeValueAsString(feature));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
