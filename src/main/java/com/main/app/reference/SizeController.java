package com.main.app.reference;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sizes")
public class SizeController {

    private final SizeRepository sizeRepository;
    private final ResourceQueries queries;

    public SizeController(SizeRepository sizeRepository, ResourceQueries queries) {
        this.sizeRepository = sizeRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<SizeDTO> list(SizeFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(sizeRepository, filter.toSpecification(), pageable, SizeDTO::from);
    }

    @GetMapping("/{key}")
    public SizeDTO get(@PathVariable String key) {
        return queries.get(sizeRepository, "size", key, SizeDTO::from);
    }
}
