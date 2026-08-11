package com.petsparc5.alerts.normalization.usgs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsparc5.alerts.ingestion.usgs.dto.UsgsFeature;
import com.petsparc5.alerts.normalization.EventPublisher;
import com.petsparc5.alerts.persistence.entity.AlertCategory;
import com.petsparc5.alerts.persistence.entity.Event;
import com.petsparc5.alerts.persistence.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsgsRawEventListenerTest {

    @Mock
    private UsgsEventNormalizer normalizer;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventPublisher eventPublisher;

    private UsgsRawEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new UsgsRawEventListener(new ObjectMapper(), normalizer, eventRepository, eventPublisher);
    }

    @Test
    void testListenPersistsAndPublishesNewEvent() {
        String rawPayload = sampleFeaturePayload();
        Event event = usgsEvent();
        when(normalizer.normalize(any(UsgsFeature.class), eq(rawPayload))).thenReturn(event);
        when(eventRepository.existsByDedupKey("usgs:us7000n7x8")).thenReturn(false);
        when(eventRepository.save(event)).thenReturn(event);

        listener.listen(rawPayload);

        verify(eventRepository).save(event);
        verify(eventPublisher).publish(event);
    }

    @Test
    void testListenSkipsAlreadyPersistedEventGivenExistingDedupKey() {
        String rawPayload = sampleFeaturePayload();
        Event event = usgsEvent();
        when(normalizer.normalize(any(UsgsFeature.class), eq(rawPayload))).thenReturn(event);
        when(eventRepository.existsByDedupKey("usgs:us7000n7x8")).thenReturn(true);

        listener.listen(rawPayload);

        verify(eventRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testListenWithMalformedPayloadDoesNotThrow() {
        assertThatCode(() -> listener.listen("not valid json")).doesNotThrowAnyException();

        verifyNoInteractions(eventRepository, eventPublisher);
    }

    private String sampleFeaturePayload() {
        return "{\"id\":\"us7000n7x8\",\"properties\":{\"mag\":5.2,\"place\":\"12km SE of Ridgecrest, CA\",\"time\":1754835791000},"
                + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[-117.61,35.58,8.2]}}";
    }

    private Event usgsEvent() {
        return Event.builder()
                .dedupKey("usgs:us7000n7x8")
                .category(AlertCategory.DISASTER)
                .type("EARTHQUAKE")
                .source("usgs")
                .severity(5.2)
                .occurredAt(OffsetDateTime.now())
                .payload(sampleFeaturePayload())
                .build();
    }
}
