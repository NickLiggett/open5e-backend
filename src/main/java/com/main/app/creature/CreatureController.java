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
@RequestMapping("/api/creatures")
public class CreatureController {

    private final CreatureRepository creatureRepository;
    private final ResourceQueries queries;

    public CreatureController(CreatureRepository creatureRepository, ResourceQueries queries) {
        this.creatureRepository = creatureRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<CreatureDTO> list(CreatureFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(creatureRepository, filter.toSpecification(), pageable, CreatureDTO::from);
    }

    @GetMapping("/{key}")
    public CreatureDTO get(@PathVariable String key) {
        return queries.get(creatureRepository, "creature", key, CreatureDTO::from);
    }

    @GetMapping("/test")
    public String test() {
        return "Creature controller works!";
    }
}
