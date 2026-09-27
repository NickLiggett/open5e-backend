package com.main.app.reference;

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
@RequestMapping("/api/skills")
public class SkillController {

    private static final ResourceType<Skill, SkillDTO> TYPE =
            new ResourceType<>(Skill.class, SkillDTO.class, "/api/skills", "skill", SkillDTO::from);

    private final SkillRepository skillRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public SkillController(SkillRepository skillRepository, ResourceQueries queries, ResourceWriter writer) {
        this.skillRepository = skillRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<SkillDTO> list(SkillFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(skillRepository, filter.toSpecification(), pageable, SkillDTO::from);
    }

    @GetMapping("/{key}")
    public SkillDTO get(@PathVariable String key) {
        return queries.get(skillRepository, "skill", key, SkillDTO::from);
    }

    @PostMapping
    public ResponseEntity<SkillDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public SkillDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public SkillDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<SkillDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
