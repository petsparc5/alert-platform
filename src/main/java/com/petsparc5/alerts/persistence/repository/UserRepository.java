package com.petsparc5.alerts.persistence.repository;

import com.petsparc5.alerts.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by the SHA-256 hash of their API key.
     *
     * @param apiKeyHash the SHA-256 hash of the presented API key
     * @return the matching user, if any
     */
    Optional<User> findByApiKeyHash(String apiKeyHash);

    /**
     * Finds a user by email address.
     *
     * @param email the email address to look up
     * @return the matching user, if any
     */
    Optional<User> findByEmail(String email);
}
