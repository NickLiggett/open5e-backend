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
@RequestMapping("/api/abilities")
public class AbilityController {

    private final AbilityRepository abilityRepository;
    private final ResourceQueries queries;

    public AbilityController(AbilityRepository abilityRepository, ResourceQueries queries) {
        this.abilityRepository = abilityRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<AbilityDTO> list(AbilityFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(abilityRepository, filter.toSpecification(), pageable, AbilityDTO::from);
    }

    @GetMapping("/{key}")
    public AbilityDTO get(@PathVariable String key) {
        return queries.get(abilityRepository, "ability", key, AbilityDTO::from);
    }
}
