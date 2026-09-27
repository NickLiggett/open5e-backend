package com.main.app.character;

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
@RequestMapping("/api/species")
public class SpeciesController {

    private static final ResourceType<Species, SpeciesDTO> TYPE =
            new ResourceType<>(Species.class, SpeciesDTO.class, "/api/species", "species", SpeciesDTO::from);

    private final SpeciesRepository speciesRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public SpeciesController(SpeciesRepository speciesRepository, ResourceQueries queries, ResourceWriter writer) {
        this.speciesRepository = speciesRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<SpeciesDTO> list(SpeciesFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(speciesRepository, filter.toSpecification(), pageable, SpeciesDTO::from);
    }

    @GetMapping("/{key}")
    public SpeciesDTO get(@PathVariable String key) {
        return queries.get(speciesRepository, "species", key, SpeciesDTO::from);
    }

    @PostMapping
    public ResponseEntity<SpeciesDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public SpeciesDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public SpeciesDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<SpeciesDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
