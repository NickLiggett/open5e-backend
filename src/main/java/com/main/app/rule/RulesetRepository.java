package com.main.app.rule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RulesetRepository extends JpaRepository<Ruleset, String>, JpaSpecificationExecutor<Ruleset> {
}
