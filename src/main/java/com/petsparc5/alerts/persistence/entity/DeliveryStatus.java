package com.petsparc5.alerts.persistence.entity;

/**
 * Delivery lifecycle state of a single notification attempt.
 */
public enum DeliveryStatus {
    PENDING,
    SENT,
    FAILED,
    DEAD_LETTERED
}
