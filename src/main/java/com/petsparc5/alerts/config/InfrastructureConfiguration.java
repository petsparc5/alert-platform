package com.petsparc5.alerts.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Manual bean registrations for infrastructure components that Spring Boot 4.0
 * no longer auto-configures out of the box. See design decision D28.
 */
@Configuration
public class InfrastructureConfiguration {

    /**
     * Provides a shared {@link RestClient.Builder} for injection into components
     * that perform outbound HTTP calls.
     *
     * @return a pre-configured builder instance
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Provides a shared {@link ObjectMapper} for JSON serialization and
     * deserialization across the application.
     *
     * @return a default {@link ObjectMapper} instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
