package com.petsparc5.alerts.persistence.entity;

/**
 * Delivery channel variant for notifications. Distinct variants of the same
 * medium (e.g. {@code SLACK_WEBHOOK} vs a future {@code SLACK_APP}) are modelled
 * as separate values so new channels can be added without a schema migration.
 */
public enum ChannelType {
    EMAIL,
    SLACK_WEBHOOK
}
