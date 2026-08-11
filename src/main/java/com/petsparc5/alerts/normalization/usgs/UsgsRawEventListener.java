package com.petsparc5.alerts.normalization.usgs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsparc5.alerts.normalization.EventPublisher;
import com.petsparc5.alerts.persistence.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka listener that consumes raw USGS payloads from {@code raw.usgs},
 * normalizes each into a canonical {@link com.petsparc5.alerts.persistence.entity.Event},
 * deduplicates against {@code dedup_key}, persists it, and republishes it to
 * the unified {@code events} topic.
 */
@Slf4j
@Component
public class UsgsRawEventListener {

    private final ObjectMapper objectMapper;
    private final UsgsEventNormalizer normalizer;
    private final EventRepository eventRepository;
    private final EventPublisher eventPublisher;

    /**
     * Creates a listener wiring the JSON mapper, normalizer, repository, and publisher.
     *
     * @param objectMapper    the mapper used to deserialize raw USGS payloads
     * @param normalizer      the normalizer used to map raw payloads to canonical events
     * @param eventRepository the repository used to check for and persist events
     * @param eventPublisher  the publisher used to republish normalized events
     */
    public UsgsRawEventListener(ObjectMapper objectMapper,
                                UsgsEventNormalizer normalizer,
                                EventRepository eventRepository,
                                EventPublisher eventPublisher) {
        this.objectMapper = objectMapper;
        this.normalizer = normalizer;
        this.eventRepository = eventRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Consumes a raw USGS payload, normalizing, deduplicating, persisting, and
     * republishing it as a canonical event.
     *
     * @param rawPayload the raw USGS feature payload as JSON
     */
    @KafkaListener(topics = "raw.usgs")
    public void listen(String rawPayload) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
