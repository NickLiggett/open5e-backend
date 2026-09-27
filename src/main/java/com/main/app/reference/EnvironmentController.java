package com.main.app.reference;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/environments")
public class EnvironmentController {

    private final EnvironmentRepository environmentRepository;
    private final ResourceQueries queries;

    public EnvironmentController(EnvironmentRepository environmentRepository, ResourceQueries queries) {
        this.environmentRepository = environmentRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<EnvironmentDTO> list(EnvironmentFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(environmentRepository, filter.toSpecification(), pageable, EnvironmentDTO::from);
    }

    @GetMapping("/{key}")
    public EnvironmentDTO get(@PathVariable String key) {
        return queries.get(environmentRepository, "environment", key, EnvironmentDTO::from);
    }
}
