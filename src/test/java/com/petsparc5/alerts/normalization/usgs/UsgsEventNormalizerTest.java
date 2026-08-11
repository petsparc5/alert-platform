package com.petsparc5.alerts.normalization.usgs;

import com.petsparc5.alerts.ingestion.usgs.dto.UsgsFeature;
import com.petsparc5.alerts.ingestion.usgs.dto.UsgsGeometry;
import com.petsparc5.alerts.ingestion.usgs.dto.UsgsProperties;
import com.petsparc5.alerts.persistence.entity.AlertCategory;
import com.petsparc5.alerts.persistence.entity.Event;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UsgsEventNormalizerTest {

    private final UsgsEventNormalizer normalizer = new UsgsEventNormalizer();

    @Test
    void testNormalizeSetsDedupKeyFromSourceAndFeatureId() {
        UsgsFeature feature = feature("us7000n7x8", 5.2, "12km SE of Ridgecrest, CA", 1754835791000L);

        Event event = normalizer.normalize(feature, "{}");

        assertThat(event.getDedupKey()).isEqualTo("usgs:us7000n7x8");
    }

    @Test
    void testNormalizeSetsCategoryDisasterAndTypeEarthquake() {
        UsgsFeature feature = feature("us7000n7x8", 5.2, "12km SE of Ridgecrest, CA", 1754835791000L);

        Event event = normalizer.normalize(feature, "{}");

        assertThat(event.getCategory()).isEqualTo(AlertCategory.DISASTER);
        assertThat(event.getType()).isEqualTo("EARTHQUAKE");
    }

    @Test
    void testNormalizeSetsSourceToUsgs() {
        UsgsFeature feature = feature("us7000n7x8", 5.2, "12km SE of Ridgecrest, CA", 1754835791000L);

        Event event = normalizer.normalize(feature, "{}");

        assertThat(event.getSource()).isEqualTo("usgs");
    }

    @Test
    void testNormalizeSetsSeverityFromMagnitude() {
        UsgsFeature feature = feature("us7000n80a", 6.4, "78km SW of Tokyo, Japan", 1754878495000L);

        Event event = normalizer.normalize(feature, "{}");

        assertThat(event.getSeverity()).isEqualTo(6.4);
    }

    @Test
    void testNormalizeConvertsEpochMillisTimeToOccurredAt() {
        UsgsFeature feature = feature("us7000n7x8", 5.2, "12km SE of Ridgecrest, CA", 1754835791000L);

        Event event = normalizer.normalize(feature, "{}");

        assertThat(event.getOccurredAt())
                .isEqualTo(OffsetDateTime.ofInstant(Instant.ofEpochMilli(1754835791000L), ZoneOffset.UTC));
    }

    @Test
    void testNormalizePreservesRawPayload() {
        UsgsFeature feature = feature("us7000n7x8", 5.2, "12km SE of Ridgecrest, CA", 1754835791000L);
        String rawPayload = "{\"id\":\"us7000n7x8\"}";

        Event event = normalizer.normalize(feature, rawPayload);

        assertThat(event.getPayload()).isEqualTo(rawPayload);
    }

    private UsgsFeature feature(String id, double mag, String place, long time) {
        return new UsgsFeature(id,
                new UsgsProperties(mag, place, time),
                new UsgsGeometry("Point", List.of(-117.61, 35.58, 8.2)));
    }
}
