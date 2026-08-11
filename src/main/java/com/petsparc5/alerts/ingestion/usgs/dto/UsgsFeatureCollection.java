package com.petsparc5.alerts.ingestion.usgs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The GeoJSON {@code FeatureCollection} returned by the USGS earthquake summary
 * feed.
 *
 * @param features the earthquake features contained in the feed
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsgsFeatureCollection(List<UsgsFeature> features) {
}
