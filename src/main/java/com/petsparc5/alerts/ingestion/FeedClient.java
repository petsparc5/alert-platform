package com.petsparc5.alerts.ingestion;

/**
 * Contract for pull-based ingestion sources. Each implementation is responsible
 * for fetching and deserializing data from one external source into a
 * source-specific DTO.
 *
 * @param <T> the type of the deserialized feed response
 */
public interface FeedClient<T> {

    /**
     * Fetches and deserializes the current state of the feed.
     *
     * @return the deserialized feed response
     */
    T fetch();
}
