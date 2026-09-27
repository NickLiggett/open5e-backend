package com.main.app.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MagicItemRepository extends JpaRepository<MagicItem, String>, JpaSpecificationExecutor<MagicItem> {
}
