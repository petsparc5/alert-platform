package com.petsparc5.alerts.persistence.repository;

import com.petsparc5.alerts.persistence.entity.AlertCategory;
import com.petsparc5.alerts.persistence.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for {@link AlertRule} entities.
 */
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    /**
     * Finds all active alert rules for a category.
     *
     * @param category the category of events to match
     * @return the active rules in that category
     */
    List<AlertRule> findByActiveTrueAndCategory(AlertCategory category);
}
