package com.main.app.character;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FeatRepository extends JpaRepository<Feat, String>, JpaSpecificationExecutor<Feat> {
}
