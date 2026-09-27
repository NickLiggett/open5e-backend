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
@RequestMapping("/api/spellschools")
public class SpellSchoolController {

    private final SpellSchoolRepository spellSchoolRepository;
    private final ResourceQueries queries;

    public SpellSchoolController(SpellSchoolRepository spellSchoolRepository, ResourceQueries queries) {
        this.spellSchoolRepository = spellSchoolRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<SpellSchoolDTO> list(SpellSchoolFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(spellSchoolRepository, filter.toSpecification(), pageable, SpellSchoolDTO::from);
    }

    @GetMapping("/{key}")
    public SpellSchoolDTO get(@PathVariable String key) {
        return queries.get(spellSchoolRepository, "spell school", key, SpellSchoolDTO::from);
    }
}
