package com.main.app.item;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
public class ServiceOfferingController {

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final ResourceQueries queries;

    public ServiceOfferingController(ServiceOfferingRepository serviceOfferingRepository, ResourceQueries queries) {
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<ServiceOfferingDTO> list(ServiceOfferingFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(serviceOfferingRepository, filter.toSpecification(), pageable, ServiceOfferingDTO::from);
    }

    @GetMapping("/{key}")
    public ServiceOfferingDTO get(@PathVariable String key) {
        return queries.get(serviceOfferingRepository, "service", key, ServiceOfferingDTO::from);
    }
}
