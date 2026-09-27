package com.main.app.rule;

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
@RequestMapping("/api/rulesets")
public class RulesetController {

    private static final ResourceType<Ruleset, RulesetDTO> TYPE =
            new ResourceType<>(Ruleset.class, RulesetDTO.class, "/api/rulesets", "ruleset", RulesetDTO::from);

    private final RulesetRepository rulesetRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public RulesetController(RulesetRepository rulesetRepository, ResourceQueries queries, ResourceWriter writer) {
        this.rulesetRepository = rulesetRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<RulesetDTO> list(RulesetFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(rulesetRepository, filter.toSpecification(), pageable, RulesetDTO::from);
    }

    @GetMapping("/{key}")
    public RulesetDTO get(@PathVariable String key) {
        return queries.get(rulesetRepository, "ruleset", key, RulesetDTO::from);
    }

    @PostMapping
    public ResponseEntity<RulesetDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public RulesetDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public RulesetDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<RulesetDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
