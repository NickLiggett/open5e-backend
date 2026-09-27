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
@RequestMapping("/api/creaturetypes")
public class CreatureTypeController {

    private final CreatureTypeRepository creatureTypeRepository;
    private final ResourceQueries queries;

    public CreatureTypeController(CreatureTypeRepository creatureTypeRepository, ResourceQueries queries) {
        this.creatureTypeRepository = creatureTypeRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<CreatureTypeDTO> list(CreatureTypeFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(creatureTypeRepository, filter.toSpecification(), pageable, CreatureTypeDTO::from);
    }

    @GetMapping("/{key}")
    public CreatureTypeDTO get(@PathVariable String key) {
        return queries.get(creatureTypeRepository, "creature type", key, CreatureTypeDTO::from);
    }
}
