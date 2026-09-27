package com.main.app.item;

import com.main.app.common.web.ResourceQueries;
import com.main.app.ownership.CopyRequest;
import com.main.app.ownership.ResourceType;
import com.main.app.ownership.ResourceWriter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/services")
public class ServiceOfferingController {

    private static final ResourceType<ServiceOffering, ServiceOfferingDTO> TYPE =
            new ResourceType<>(ServiceOffering.class, ServiceOfferingDTO.class, "/api/services", "service", ServiceOfferingDTO::from);

    private final ServiceOfferingRepository serviceOfferingRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public ServiceOfferingController(ServiceOfferingRepository serviceOfferingRepository, ResourceQueries queries, ResourceWriter writer) {
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<ServiceOfferingDTO> list(ServiceOfferingFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(serviceOfferingRepository, filter.toSpecification(), pageable, ServiceOfferingDTO::from);
    }

    @GetMapping("/{key}")
    public ServiceOfferingDTO get(@PathVariable String key) {
        return queries.get(serviceOfferingRepository, "service", key, ServiceOfferingDTO::from);
    }

    @PostMapping
    public ResponseEntity<ServiceOfferingDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public ServiceOfferingDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public ServiceOfferingDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<ServiceOfferingDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
