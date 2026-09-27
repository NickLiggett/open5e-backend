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
@RequestMapping("/api/itemrarities")
public class ItemRarityController {

    private final ItemRarityRepository itemRarityRepository;
    private final ResourceQueries queries;

    public ItemRarityController(ItemRarityRepository itemRarityRepository, ResourceQueries queries) {
        this.itemRarityRepository = itemRarityRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<ItemRarityDTO> list(ItemRarityFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(itemRarityRepository, filter.toSpecification(), pageable, ItemRarityDTO::from);
    }

    @GetMapping("/{key}")
    public ItemRarityDTO get(@PathVariable String key) {
        return queries.get(itemRarityRepository, "item rarity", key, ItemRarityDTO::from);
    }
}
