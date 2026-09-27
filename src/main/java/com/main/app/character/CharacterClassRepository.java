package com.main.app.character;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CharacterClassRepository extends JpaRepository<CharacterClass, String>, JpaSpecificationExecutor<CharacterClass> {
}
