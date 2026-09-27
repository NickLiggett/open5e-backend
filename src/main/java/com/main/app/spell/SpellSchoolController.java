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
@RequestMapping("/api/spellschools")
public class SpellSchoolController {

    private static final ResourceType<SpellSchool, SpellSchoolDTO> TYPE =
            new ResourceType<>(SpellSchool.class, SpellSchoolDTO.class, "/api/spellschools", "spell school", SpellSchoolDTO::from);

    private final SpellSchoolRepository spellSchoolRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public SpellSchoolController(SpellSchoolRepository spellSchoolRepository, ResourceQueries queries, ResourceWriter writer) {
        this.spellSchoolRepository = spellSchoolRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<SpellSchoolDTO> list(SpellSchoolFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(spellSchoolRepository, filter.toSpecification(), pageable, SpellSchoolDTO::from);
    }

    @GetMapping("/{key}")
    public SpellSchoolDTO get(@PathVariable String key) {
        return queries.get(spellSchoolRepository, "spell school", key, SpellSchoolDTO::from);
    }

    @PostMapping
    public ResponseEntity<SpellSchoolDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public SpellSchoolDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public SpellSchoolDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<SpellSchoolDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
