package com.petsparc5.alerts.normalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsparc5.alerts.persistence.entity.AlertCategory;
import com.petsparc5.alerts.persistence.entity.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    private EventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new EventPublisher(kafkaTemplate, new ObjectMapper());
    }

    @Test
    void testPublishSendsNormalizedEventToEventsTopic() {
        Event event = usgsEvent();

        publisher.publish(event);

        verify(kafkaTemplate).send("events", "usgs:us7000n7x8", payloadCaptor.capture());
    }

    @Test
    void testPublishKeysMessageByDedupKey() {
        Event event = usgsEvent();

        publisher.publish(event);

        verify(kafkaTemplate).send("events", "usgs:us7000n7x8", payloadCaptor.capture());
    }

    @Test
    void testPublishSerializesCanonicalEventFieldsAsPayload() {
        Event event = usgsEvent();

        publisher.publish(event);

        verify(kafkaTemplate).send("events", "usgs:us7000n7x8", payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .contains("\"dedupKey\":\"usgs:us7000n7x8\"")
                .contains("\"category\":\"DISASTER\"")
                .contains("\"type\":\"EARTHQUAKE\"");
    }

    private Event usgsEvent() {
        return Event.builder()
                .dedupKey("usgs:us7000n7x8")
                .category(AlertCategory.DISASTER)
                .type("EARTHQUAKE")
                .source("usgs")
                .severity(5.2)
                .occurredAt(OffsetDateTime.of(2026, 8, 10, 12, 0, 0, 0, ZoneOffset.UTC))
                .payload("{\"id\":\"us7000n7x8\"}")
                .build();
    }
}
