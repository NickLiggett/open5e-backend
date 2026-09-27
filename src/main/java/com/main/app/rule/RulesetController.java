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
@RequestMapping("/api/rulesets")
public class RulesetController {

    private final RulesetRepository rulesetRepository;
    private final ResourceQueries queries;

    public RulesetController(RulesetRepository rulesetRepository, ResourceQueries queries) {
        this.rulesetRepository = rulesetRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<RulesetDTO> list(RulesetFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(rulesetRepository, filter.toSpecification(), pageable, RulesetDTO::from);
    }

    @GetMapping("/{key}")
    public RulesetDTO get(@PathVariable String key) {
        return queries.get(rulesetRepository, "ruleset", key, RulesetDTO::from);
    }
}
