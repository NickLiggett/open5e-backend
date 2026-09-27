package com.main.app.item;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itemsets")
public class ItemSetController {

    private final ItemSetRepository itemSetRepository;
    private final ResourceQueries queries;

    public ItemSetController(ItemSetRepository itemSetRepository, ResourceQueries queries) {
        this.itemSetRepository = itemSetRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<ItemSetDTO> list(ItemSetFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(itemSetRepository, filter.toSpecification(), pageable, ItemSetDTO::from);
    }

    @GetMapping("/{key}")
    public ItemSetDTO get(@PathVariable String key) {
        return queries.get(itemSetRepository, "item set", key, ItemSetDTO::from);
    }
}
