package com.petsparc5.alerts.normalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsparc5.alerts.persistence.entity.Event;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes canonical {@link Event}s to the unified {@code events} Kafka
 * topic, keyed by {@code dedup_key}.
 */
@Component
public class EventPublisher {

    private static final String TOPIC = "events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Creates a publisher backed by the given Kafka template and JSON mapper.
     *
     * @param kafkaTemplate the template used to send normalized events
     * @param objectMapper  the mapper used to serialize normalized events
     */
    public EventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a canonical event to the {@code events} topic keyed by its
     * {@code dedup_key}.
     *
     * @param event the canonical event to publish
     */
    public void publish(Event event) {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
