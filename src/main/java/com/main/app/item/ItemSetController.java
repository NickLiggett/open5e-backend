package com.main.app.item;

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
@RequestMapping("/api/itemsets")
public class ItemSetController {

    private static final ResourceType<ItemSet, ItemSetDTO> TYPE =
            new ResourceType<>(ItemSet.class, ItemSetDTO.class, "/api/itemsets", "item set", ItemSetDTO::from);

    private final ItemSetRepository itemSetRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public ItemSetController(ItemSetRepository itemSetRepository, ResourceQueries queries, ResourceWriter writer) {
        this.itemSetRepository = itemSetRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<ItemSetDTO> list(ItemSetFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(itemSetRepository, filter.toSpecification(), pageable, ItemSetDTO::from);
    }

    @GetMapping("/{key}")
    public ItemSetDTO get(@PathVariable String key) {
        return queries.get(itemSetRepository, "item set", key, ItemSetDTO::from);
    }

    @PostMapping
    public ResponseEntity<ItemSetDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public ItemSetDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public ItemSetDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<ItemSetDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
