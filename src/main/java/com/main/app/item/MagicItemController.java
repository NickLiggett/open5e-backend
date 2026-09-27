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
@RequestMapping("/api/magicitems")
public class MagicItemController {

    private final MagicItemRepository magicItemRepository;
    private final ResourceQueries queries;

    public MagicItemController(MagicItemRepository magicItemRepository, ResourceQueries queries) {
        this.magicItemRepository = magicItemRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<MagicItemDTO> list(MagicItemFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(magicItemRepository, filter.toSpecification(), pageable, MagicItemDTO::from);
    }

    @GetMapping("/{key}")
    public MagicItemDTO get(@PathVariable String key) {
        return queries.get(magicItemRepository, "magic item", key, MagicItemDTO::from);
    }
}
