package com.main.app.rule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RuleRepository extends JpaRepository<Rule, String>, JpaSpecificationExecutor<Rule> {
}
