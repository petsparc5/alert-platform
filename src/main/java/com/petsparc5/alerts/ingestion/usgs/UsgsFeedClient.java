package com.petsparc5.alerts.ingestion.usgs;

import com.petsparc5.alerts.ingestion.FeedClient;
import com.petsparc5.alerts.ingestion.usgs.dto.UsgsFeatureCollection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the USGS earthquake GeoJSON summary feed. Fetches and
 * deserializes the feed into a {@link UsgsFeatureCollection}.
 */
@Component
public class UsgsFeedClient implements FeedClient<UsgsFeatureCollection> {

    private final RestClient restClient;
    private final String feedUrl;

    /**
     * Creates a client that reads from the configured USGS feed URL.
     *
     * @param restClientBuilder the builder used to construct the underlying client
     * @param feedUrl           the USGS GeoJSON summary feed URL
     */
    public UsgsFeedClient(RestClient.Builder restClientBuilder,
                          @Value("${ingestion.usgs.feed-url}") String feedUrl) {
        this.restClient = restClientBuilder.build();
        this.feedUrl = feedUrl;
    }

    /**
     * Fetches the current USGS earthquake feed.
     *
     * @return the deserialized feature collection
     */
    public UsgsFeatureCollection fetch() {
        UsgsFeatureCollection collection = restClient.get()
                .uri(feedUrl)
                .retrieve()
                .body(UsgsFeatureCollection.class);
        if (collection == null || collection.features() == null) {
            throw new IllegalStateException("USGS feed returned a null or structurally invalid response body");
        }
        return collection;
    }
}
