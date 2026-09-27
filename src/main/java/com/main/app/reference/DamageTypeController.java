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
@RequestMapping("/api/damagetypes")
public class DamageTypeController {

    private final DamageTypeRepository damageTypeRepository;
    private final ResourceQueries queries;

    public DamageTypeController(DamageTypeRepository damageTypeRepository, ResourceQueries queries) {
        this.damageTypeRepository = damageTypeRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<DamageTypeDTO> list(DamageTypeFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(damageTypeRepository, filter.toSpecification(), pageable, DamageTypeDTO::from);
    }

    @GetMapping("/{key}")
    public DamageTypeDTO get(@PathVariable String key) {
        return queries.get(damageTypeRepository, "damage type", key, DamageTypeDTO::from);
    }
}
