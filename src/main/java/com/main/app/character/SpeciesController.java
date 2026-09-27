package com.main.app.character;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/species")
public class SpeciesController {

    private final SpeciesRepository speciesRepository;
    private final ResourceQueries queries;

    public SpeciesController(SpeciesRepository speciesRepository, ResourceQueries queries) {
        this.speciesRepository = speciesRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<SpeciesDTO> list(SpeciesFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(speciesRepository, filter.toSpecification(), pageable, SpeciesDTO::from);
    }

    @GetMapping("/{key}")
    public SpeciesDTO get(@PathVariable String key) {
        return queries.get(speciesRepository, "species", key, SpeciesDTO::from);
    }
}
