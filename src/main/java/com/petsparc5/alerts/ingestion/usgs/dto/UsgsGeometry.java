package com.petsparc5.alerts.ingestion.usgs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * GeoJSON geometry of a USGS earthquake feature.
 *
 * @param type        the GeoJSON geometry type (e.g. {@code Point})
 * @param coordinates the {@code [longitude, latitude, depth]} triple
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsgsGeometry(String type, List<Double> coordinates) {
}
