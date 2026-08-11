package com.petsparc5.alerts.persistence.repository;

import com.petsparc5.alerts.persistence.entity.ChannelConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Data access for {@link ChannelConfig} entities.
 */
public interface ChannelConfigRepository extends JpaRepository<ChannelConfig, Long> {

    /**
     * Finds all enabled channel configurations for a user.
     *
     * @param userId the owning user's identifier
     * @return the user's enabled channel configurations
     */
    List<ChannelConfig> findByUserIdAndEnabledTrue(UUID userId);
}
