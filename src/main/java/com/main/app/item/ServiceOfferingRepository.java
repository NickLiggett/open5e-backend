package com.main.app.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, String>, JpaSpecificationExecutor<ServiceOffering> {
}
