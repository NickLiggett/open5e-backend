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
@RequestMapping("/api/feats")
public class FeatController {

    private static final ResourceType<Feat, FeatDTO> TYPE =
            new ResourceType<>(Feat.class, FeatDTO.class, "/api/feats", "feat", FeatDTO::from);

    private final FeatRepository featRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public FeatController(FeatRepository featRepository, ResourceQueries queries, ResourceWriter writer) {
        this.featRepository = featRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<FeatDTO> list(FeatFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(featRepository, filter.toSpecification(), pageable, FeatDTO::from);
    }

    @GetMapping("/{key}")
    public FeatDTO get(@PathVariable String key) {
        return queries.get(featRepository, "feat", key, FeatDTO::from);
    }

    @PostMapping
    public ResponseEntity<FeatDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public FeatDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public FeatDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<FeatDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
