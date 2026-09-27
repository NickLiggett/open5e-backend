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
@RequestMapping("/api/conditions")
public class ConditionController {

    private final ConditionRepository conditionRepository;
    private final ResourceQueries queries;

    public ConditionController(ConditionRepository conditionRepository, ResourceQueries queries) {
        this.conditionRepository = conditionRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<ConditionDTO> list(ConditionFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(conditionRepository, filter.toSpecification(), pageable, ConditionDTO::from);
    }

    @GetMapping("/{key}")
    public ConditionDTO get(@PathVariable String key) {
        return queries.get(conditionRepository, "condition", key, ConditionDTO::from);
    }
}
