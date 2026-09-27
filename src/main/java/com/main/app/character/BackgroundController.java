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
@RequestMapping("/api/backgrounds")
public class BackgroundController {

    private final BackgroundRepository backgroundRepository;
    private final ResourceQueries queries;

    public BackgroundController(BackgroundRepository backgroundRepository, ResourceQueries queries) {
        this.backgroundRepository = backgroundRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<BackgroundDTO> list(BackgroundFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(backgroundRepository, filter.toSpecification(), pageable, BackgroundDTO::from);
    }

    @GetMapping("/{key}")
    public BackgroundDTO get(@PathVariable String key) {
        return queries.get(backgroundRepository, "background", key, BackgroundDTO::from);
    }
}
