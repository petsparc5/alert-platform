package com.petsparc5.alerts.ingestion.usgs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Descriptive properties of a USGS earthquake feature.
 *
 * @param mag   the earthquake magnitude
 * @param place a human-readable location description
 * @param time  the origin time in epoch milliseconds
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsgsProperties(Double mag, String place, Long time) {
}
