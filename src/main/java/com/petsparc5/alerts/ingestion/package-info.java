/**
 * Ingestion module: pulls events from external sources via polling and accepts
 * pushed events via webhooks, publishing each raw payload to its per-source
 * {@code raw.<source>} Kafka topic.
 */
package com.petsparc5.alerts.ingestion;
