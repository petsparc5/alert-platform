package com.petsparc5.alerts.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RawEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private RawEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RawEventPublisher(kafkaTemplate);
    }

    @Test
    void testPublishSendsPayloadToPerSourceTopic() {
        RawEvent rawEvent = new RawEvent("usgs", "us7000n7x8", "{\"id\":\"us7000n7x8\"}");

        publisher.publish(rawEvent);

        verify(kafkaTemplate).send("raw.usgs", "usgs:us7000n7x8", "{\"id\":\"us7000n7x8\"}");
    }

    @Test
    void testPublishKeysMessageBySourceAndExternalId() {
        RawEvent rawEvent = new RawEvent("usgs", "us7000n80a", "{\"id\":\"us7000n80a\"}");

        publisher.publish(rawEvent);

        verify(kafkaTemplate).send("raw.usgs", "usgs:us7000n80a", "{\"id\":\"us7000n80a\"}");
    }
}
