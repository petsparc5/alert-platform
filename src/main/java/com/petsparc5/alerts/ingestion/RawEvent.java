package com.petsparc5.alerts.ingestion;

/**
 * An untouched payload pulled from an external source, awaiting normalization.
 * The {@code source} and {@code externalId} together form the deduplication key
 * ({@code source:externalId}) and drive the Kafka topic and partitioning.
 *
 * @param source     the originating source identifier (e.g. {@code usgs})
 * @param externalId the source-assigned identifier of the item
 * @param payload    the raw payload serialized as JSON
 */
public record RawEvent(String source, String externalId, String payload) {
}
