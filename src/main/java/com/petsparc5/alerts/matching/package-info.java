/**
 * Matching module: consumes normalized events, evaluates them against active
 * alert rules, and emits one notification request per matched (user, channel)
 * to the {@code notifications} topic.
 */
package com.petsparc5.alerts.matching;
