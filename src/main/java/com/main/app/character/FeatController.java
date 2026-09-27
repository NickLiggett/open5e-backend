package com.main.app.character;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feats")
public class FeatController {

    private final FeatRepository featRepository;
    private final ResourceQueries queries;

    public FeatController(FeatRepository featRepository, ResourceQueries queries) {
        this.featRepository = featRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<FeatDTO> list(FeatFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(featRepository, filter.toSpecification(), pageable, FeatDTO::from);
    }

    @GetMapping("/{key}")
    public FeatDTO get(@PathVariable String key) {
        return queries.get(featRepository, "feat", key, FeatDTO::from);
    }
}
