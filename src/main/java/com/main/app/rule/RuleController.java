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
@RequestMapping("/api/rules")
public class RuleController {

    private static final ResourceType<Rule, RuleDTO> TYPE =
            new ResourceType<>(Rule.class, RuleDTO.class, "/api/rules", "rule", RuleDTO::from);

    private final RuleRepository ruleRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public RuleController(RuleRepository ruleRepository, ResourceQueries queries, ResourceWriter writer) {
        this.ruleRepository = ruleRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<RuleDTO> list(RuleFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(ruleRepository, filter.toSpecification(), pageable, RuleDTO::from);
    }

    @GetMapping("/{key}")
    public RuleDTO get(@PathVariable String key) {
        return queries.get(ruleRepository, "rule", key, RuleDTO::from);
    }

    @PostMapping
    public ResponseEntity<RuleDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public RuleDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public RuleDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<RuleDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
