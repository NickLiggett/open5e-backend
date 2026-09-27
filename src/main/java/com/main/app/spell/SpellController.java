package com.main.app.spell;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spells")
public class SpellController {

    private final SpellRepository spellRepository;
    private final ResourceQueries queries;

    public SpellController(SpellRepository spellRepository, ResourceQueries queries) {
        this.spellRepository = spellRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<SpellDTO> list(SpellFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(spellRepository, filter.toSpecification(), pageable, SpellDTO::from);
    }

    @GetMapping("/{key}")
    public SpellDTO get(@PathVariable String key) {
        return queries.get(spellRepository, "spell", key, SpellDTO::from);
    }
}
