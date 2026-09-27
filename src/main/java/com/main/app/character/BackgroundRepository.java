package com.main.app.character;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BackgroundRepository extends JpaRepository<Background, String>, JpaSpecificationExecutor<Background> {
}
