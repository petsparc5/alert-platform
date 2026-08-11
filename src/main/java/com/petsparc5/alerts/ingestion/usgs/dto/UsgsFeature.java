package com.petsparc5.alerts.ingestion.usgs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single USGS earthquake feature from the GeoJSON summary feed.
 *
 * @param id         the USGS-assigned event id (used as the external id)
 * @param properties the descriptive properties of the earthquake
 * @param geometry   the location geometry of the earthquake
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsgsFeature(String id, UsgsProperties properties, UsgsGeometry geometry) {
}
