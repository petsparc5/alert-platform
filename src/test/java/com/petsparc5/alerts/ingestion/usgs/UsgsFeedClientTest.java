package com.petsparc5.alerts.ingestion.usgs;

import com.petsparc5.alerts.ingestion.usgs.dto.UsgsFeature;
import com.petsparc5.alerts.ingestion.usgs.dto.UsgsFeatureCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UsgsFeedClientTest {

    private static final String FEED_URL =
            "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_hour.geojson";

    private MockRestServiceServer server;
    private UsgsFeedClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new UsgsFeedClient(builder, FEED_URL);
    }

    @Test
    void testFetchReturnsAllFeatures() throws IOException {
        server.expect(requestTo(FEED_URL))
                .andRespond(withSuccess(sampleFeed(), MediaType.APPLICATION_JSON));

        UsgsFeatureCollection collection = client.fetch();

        assertThat(collection.features()).hasSize(2);
    }

    @Test
    void testFetchParsesFeatureIdMagnitudePlaceTimeAndCoordinates() throws IOException {
        server.expect(requestTo(FEED_URL))
                .andRespond(withSuccess(sampleFeed(), MediaType.APPLICATION_JSON));

        UsgsFeature feature = client.fetch().features().getFirst();

        assertThat(feature.id()).isEqualTo("us7000n7x8");
        assertThat(feature.properties().mag()).isEqualTo(5.2);
        assertThat(feature.properties().place()).isEqualTo("12km SE of Ridgecrest, CA");
        assertThat(feature.properties().time()).isEqualTo(1754835791000L);
        assertThat(feature.geometry().coordinates()).containsExactly(-117.61, 35.58, 8.2);
    }

    @Test
    void testFetchWithEmptyFeatureCollection() {
        server.expect(requestTo(FEED_URL))
                .andRespond(withSuccess("{\"type\":\"FeatureCollection\",\"features\":[]}",
                        MediaType.APPLICATION_JSON));

        UsgsFeatureCollection collection = client.fetch();

        assertThat(collection.features()).isEmpty();
    }

    @Test
    void testFetchShouldThrowRestClientResponseExceptionGivenServerError() {
        server.expect(requestTo(FEED_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetch())
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void testFetchShouldThrowIllegalStateExceptionGivenNullFeaturesList() {
        server.expect(requestTo(FEED_URL))
                .andRespond(withSuccess("{\"features\":null}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetch())
                .isInstanceOf(IllegalStateException.class);
    }

    private String sampleFeed() throws IOException {
        return new ClassPathResource("usgs/all_hour_sample.geojson")
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
