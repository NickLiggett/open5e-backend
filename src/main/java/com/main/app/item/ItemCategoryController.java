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
@RequestMapping("/api/itemcategories")
public class ItemCategoryController {

    private final ItemCategoryRepository itemCategoryRepository;
    private final ResourceQueries queries;

    public ItemCategoryController(ItemCategoryRepository itemCategoryRepository, ResourceQueries queries) {
        this.itemCategoryRepository = itemCategoryRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<ItemCategoryDTO> list(ItemCategoryFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(itemCategoryRepository, filter.toSpecification(), pageable, ItemCategoryDTO::from);
    }

    @GetMapping("/{key}")
    public ItemCategoryDTO get(@PathVariable String key) {
        return queries.get(itemCategoryRepository, "item category", key, ItemCategoryDTO::from);
    }
}
