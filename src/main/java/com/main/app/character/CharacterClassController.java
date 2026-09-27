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
@RequestMapping("/api/classes")
public class CharacterClassController {

    private static final ResourceType<CharacterClass, CharacterClassDTO> TYPE =
            new ResourceType<>(CharacterClass.class, CharacterClassDTO.class, "/api/classes", "class", CharacterClassDTO::from);

    private final CharacterClassRepository characterClassRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public CharacterClassController(CharacterClassRepository characterClassRepository, ResourceQueries queries, ResourceWriter writer) {
        this.characterClassRepository = characterClassRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<CharacterClassDTO> list(CharacterClassFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(characterClassRepository, filter.toSpecification(), pageable, CharacterClassDTO::from);
    }

    @GetMapping("/{key}")
    public CharacterClassDTO get(@PathVariable String key) {
        return queries.get(characterClassRepository, "class", key, CharacterClassDTO::from);
    }

    @PostMapping
    public ResponseEntity<CharacterClassDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public CharacterClassDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public CharacterClassDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<CharacterClassDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
