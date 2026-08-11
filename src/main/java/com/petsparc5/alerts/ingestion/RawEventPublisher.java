package com.petsparc5.alerts.ingestion;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link RawEvent}s to their per-source {@code raw.<source>} Kafka
 * topic, keyed by {@code source:externalId} so that every record for one item
 * lands on the same partition and can be deduplicated downstream.
 */
@Component
public class RawEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Creates a publisher backed by the given Kafka template.
     *
     * @param kafkaTemplate the template used to send raw payloads
     */
    public RawEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a raw event to the {@code raw.<source>} topic keyed by
     * {@code source:externalId}.
     *
     * @param rawEvent the raw event to publish
     */
    public void publish(RawEvent rawEvent) {
        String topic = "raw." + rawEvent.source();
        String key = rawEvent.source() + ":" + rawEvent.externalId();
        kafkaTemplate.send(topic, key, rawEvent.payload());
    }
}
