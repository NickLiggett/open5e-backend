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
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillRepository skillRepository;
    private final ResourceQueries queries;

    public SkillController(SkillRepository skillRepository, ResourceQueries queries) {
        this.skillRepository = skillRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<SkillDTO> list(SkillFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(skillRepository, filter.toSpecification(), pageable, SkillDTO::from);
    }

    @GetMapping("/{key}")
    public SkillDTO get(@PathVariable String key) {
        return queries.get(skillRepository, "skill", key, SkillDTO::from);
    }
}
