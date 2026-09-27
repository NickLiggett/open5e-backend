package com.main.app.rule;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleRepository ruleRepository;
    private final ResourceQueries queries;

    public RuleController(RuleRepository ruleRepository, ResourceQueries queries) {
        this.ruleRepository = ruleRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<RuleDTO> list(RuleFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(ruleRepository, filter.toSpecification(), pageable, RuleDTO::from);
    }

    @GetMapping("/{key}")
    public RuleDTO get(@PathVariable String key) {
        return queries.get(ruleRepository, "rule", key, RuleDTO::from);
    }
}
