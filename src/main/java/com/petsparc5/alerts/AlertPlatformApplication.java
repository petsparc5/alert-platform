package com.petsparc5.alerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Alert Platform, a modular monolith that ingests external
 * events, normalizes and matches them against user alert rules, and dispatches
 * notifications across pluggable channels.
 */
@SpringBootApplication
public class AlertPlatformApplication {

    /**
     * Bootstraps the Spring application context.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(AlertPlatformApplication.class, args);
    }
}
