/**
 * Normalization module: consumes per-source raw events, maps them to the
 * canonical {@code Event} model, deduplicates via the {@code dedup_key} unique
 * constraint, persists them, and republishes to the unified {@code events} topic.
 */
package com.petsparc5.alerts.normalization;
