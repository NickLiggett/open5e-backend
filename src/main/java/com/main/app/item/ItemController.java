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
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository itemRepository;
    private final ResourceQueries queries;

    public ItemController(ItemRepository itemRepository, ResourceQueries queries) {
        this.itemRepository = itemRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<ItemDTO> list(ItemFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(itemRepository, filter.toSpecification(), pageable, ItemDTO::from);
    }

    @GetMapping("/{key}")
    public ItemDTO get(@PathVariable String key) {
        return queries.get(itemRepository, "item", key, ItemDTO::from);
    }
}
