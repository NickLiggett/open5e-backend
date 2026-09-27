package com.main.app.creature;

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
@RequestMapping("/api/creaturesets")
public class CreatureSetController {

    private static final ResourceType<CreatureSet, CreatureSetDTO> TYPE =
            new ResourceType<>(CreatureSet.class, CreatureSetDTO.class, "/api/creaturesets", "creature set", CreatureSetDTO::from);

    private final CreatureSetRepository creatureSetRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public CreatureSetController(CreatureSetRepository creatureSetRepository, ResourceQueries queries, ResourceWriter writer) {
        this.creatureSetRepository = creatureSetRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<CreatureSetDTO> list(CreatureSetFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(creatureSetRepository, filter.toSpecification(), pageable, CreatureSetDTO::from);
    }

    @GetMapping("/{key}")
    public CreatureSetDTO get(@PathVariable String key) {
        return queries.get(creatureSetRepository, "creature set", key, CreatureSetDTO::from);
    }

    @PostMapping
    public ResponseEntity<CreatureSetDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public CreatureSetDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public CreatureSetDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<CreatureSetDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
