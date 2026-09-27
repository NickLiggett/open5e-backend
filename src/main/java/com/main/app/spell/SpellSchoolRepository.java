package com.main.app.spell;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SpellSchoolRepository extends JpaRepository<SpellSchool, String>, JpaSpecificationExecutor<SpellSchool> {
}
