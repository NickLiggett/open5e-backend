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
@RequestMapping("/api/armor")
public class ArmorController {

    private final ArmorRepository armorRepository;
    private final ResourceQueries queries;

    public ArmorController(ArmorRepository armorRepository, ResourceQueries queries) {
        this.armorRepository = armorRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<ArmorDTO> list(ArmorFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(armorRepository, filter.toSpecification(), pageable, ArmorDTO::from);
    }

    @GetMapping("/{key}")
    public ArmorDTO get(@PathVariable String key) {
        return queries.get(armorRepository, "armor", key, ArmorDTO::from);
    }
}
