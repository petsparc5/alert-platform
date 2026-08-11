/**
 * Dispatch module: consumes notification requests and routes them to the
 * appropriate {@code NotificationChannel} strategy (email, Slack, …), applying
 * retries with backoff, dead-lettering failures, and recording delivery status.
 */
package com.petsparc5.alerts.dispatch;
