package com.main.app.creature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CreatureSetRepository extends JpaRepository<CreatureSet, String>, JpaSpecificationExecutor<CreatureSet> {
}
