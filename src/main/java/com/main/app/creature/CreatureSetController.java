package com.main.app.creature;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/creaturesets")
public class CreatureSetController {

    private final CreatureSetRepository creatureSetRepository;
    private final ResourceQueries queries;

    public CreatureSetController(CreatureSetRepository creatureSetRepository, ResourceQueries queries) {
        this.creatureSetRepository = creatureSetRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<CreatureSetDTO> list(CreatureSetFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(creatureSetRepository, filter.toSpecification(), pageable, CreatureSetDTO::from);
    }

    @GetMapping("/{key}")
    public CreatureSetDTO get(@PathVariable String key) {
        return queries.get(creatureSetRepository, "creature set", key, CreatureSetDTO::from);
    }
}
