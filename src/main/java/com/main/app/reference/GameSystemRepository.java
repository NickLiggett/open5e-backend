package com.main.app.reference;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GameSystemRepository extends JpaRepository<GameSystem, String>, JpaSpecificationExecutor<GameSystem> {
}
