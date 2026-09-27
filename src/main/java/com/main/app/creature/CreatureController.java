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
@RequestMapping("/api/creatures")
public class CreatureController {

    private static final ResourceType<Creature, CreatureDTO> TYPE =
            new ResourceType<>(Creature.class, CreatureDTO.class, "/api/creatures", "creature", CreatureDTO::from);

    private final CreatureRepository creatureRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public CreatureController(CreatureRepository creatureRepository, ResourceQueries queries, ResourceWriter writer) {
        this.creatureRepository = creatureRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<CreatureDTO> list(CreatureFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(creatureRepository, filter.toSpecification(), pageable, CreatureDTO::from);
    }

    @GetMapping("/{key}")
    public CreatureDTO get(@PathVariable String key) {
        return queries.get(creatureRepository, "creature", key, CreatureDTO::from);
    }

    @PostMapping
    public ResponseEntity<CreatureDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public CreatureDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public CreatureDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<CreatureDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }

    @GetMapping("/test")
    public String test() {
        return "Creature controller works!";
    }
}
