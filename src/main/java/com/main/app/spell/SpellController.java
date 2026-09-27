package com.main.app.spell;

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
@RequestMapping("/api/spells")
public class SpellController {

    private static final ResourceType<Spell, SpellDTO> TYPE =
            new ResourceType<>(Spell.class, SpellDTO.class, "/api/spells", "spell", SpellDTO::from);

    private final SpellRepository spellRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public SpellController(SpellRepository spellRepository, ResourceQueries queries, ResourceWriter writer) {
        this.spellRepository = spellRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<SpellDTO> list(SpellFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(spellRepository, filter.toSpecification(), pageable, SpellDTO::from);
    }

    @GetMapping("/{key}")
    public SpellDTO get(@PathVariable String key) {
        return queries.get(spellRepository, "spell", key, SpellDTO::from);
    }

    @PostMapping
    public ResponseEntity<SpellDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public SpellDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public SpellDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<SpellDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
