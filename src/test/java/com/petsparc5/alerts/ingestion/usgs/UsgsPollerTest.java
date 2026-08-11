package com.petsparc5.alerts.ingestion.usgs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsparc5.alerts.ingestion.RawEvent;
import com.petsparc5.alerts.ingestion.RawEventPublisher;
import com.petsparc5.alerts.ingestion.usgs.dto.UsgsFeature;
import com.petsparc5.alerts.ingestion.usgs.dto.UsgsFeatureCollection;
import com.petsparc5.alerts.ingestion.usgs.dto.UsgsGeometry;
import com.petsparc5.alerts.ingestion.usgs.dto.UsgsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsgsPollerTest {

    @Mock
    private UsgsFeedClient usgsFeedClient;

    @Mock
    private RawEventPublisher rawEventPublisher;

    @Captor
    private ArgumentCaptor<RawEvent> rawEventCaptor;

    private UsgsPoller poller;

    @BeforeEach
    void setUp() {
        poller = new UsgsPoller(usgsFeedClient, rawEventPublisher, new ObjectMapper());
    }

    @Test
    void testPollPublishesOneRawEventPerFeature() {
        when(usgsFeedClient.fetch()).thenReturn(feedWithTwoFeatures());

        poller.poll();

        verify(rawEventPublisher, times(2)).publish(rawEventCaptor.capture());
    }

    @Test
    void testPollPublishesRawEventKeyedByUsgsEventId() {
        when(usgsFeedClient.fetch()).thenReturn(feedWithTwoFeatures());

        poller.poll();

        verify(rawEventPublisher, times(2)).publish(rawEventCaptor.capture());
        assertThat(rawEventCaptor.getAllValues())
                .allSatisfy(rawEvent -> assertThat(rawEvent.source()).isEqualTo("usgs"))
                .extracting(RawEvent::externalId)
                .containsExactly("us7000n7x8", "us7000n80a");
    }

    @Test
    void testPollSerializesFeatureAsRawEventPayload() {
        when(usgsFeedClient.fetch()).thenReturn(feedWithTwoFeatures());

        poller.poll();

        verify(rawEventPublisher, times(2)).publish(rawEventCaptor.capture());
        assertThat(rawEventCaptor.getAllValues().getFirst().payload())
                .contains("us7000n7x8")
                .contains("Ridgecrest");
    }

    @Test
    void testPollWithEmptyFeedPublishesNothing() {
        when(usgsFeedClient.fetch()).thenReturn(new UsgsFeatureCollection(List.of()));

        poller.poll();

        verifyNoInteractions(rawEventPublisher);
    }

    @Test
    void testPollSkipsFeatureWithNullId() {
        UsgsFeature nullIdFeature = new UsgsFeature(null,
                new UsgsProperties(3.0, "somewhere", 1754835791000L),
                new UsgsGeometry("Point", List.of(0.0, 0.0, 0.0)));
        UsgsFeature validFeature = new UsgsFeature("us7000n7x8",
                new UsgsProperties(5.2, "12km SE of Ridgecrest, CA", 1754835791000L),
                new UsgsGeometry("Point", List.of(-117.61, 35.58, 8.2)));
        when(usgsFeedClient.fetch()).thenReturn(new UsgsFeatureCollection(List.of(nullIdFeature, validFeature)));

        poller.poll();

        verify(rawEventPublisher, times(1)).publish(rawEventCaptor.capture());
        assertThat(rawEventCaptor.getValue().externalId()).isEqualTo("us7000n7x8");
    }

    @Test
    void testPollWithFeedClientExceptionPublishesNothing() {
        when(usgsFeedClient.fetch()).thenThrow(new RuntimeException("connection refused"));

        poller.poll();

        verifyNoInteractions(rawEventPublisher);
    }

    private UsgsFeatureCollection feedWithTwoFeatures() {
        UsgsFeature first = new UsgsFeature(
                "us7000n7x8",
                new UsgsProperties(5.2, "12km SE of Ridgecrest, CA", 1754835791000L),
                new UsgsGeometry("Point", List.of(-117.61, 35.58, 8.2)));
        UsgsFeature second = new UsgsFeature(
                "us7000n80a",
                new UsgsProperties(6.4, "78km SW of Tokyo, Japan", 1754878495000L),
                new UsgsGeometry("Point", List.of(139.21, 35.11, 30.0)));
        return new UsgsFeatureCollection(List.of(first, second));
    }
}
