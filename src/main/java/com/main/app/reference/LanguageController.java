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
@RequestMapping("/api/languages")
public class LanguageController {

    private final LanguageRepository languageRepository;
    private final ResourceQueries queries;

    public LanguageController(LanguageRepository languageRepository, ResourceQueries queries) {
        this.languageRepository = languageRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<LanguageDTO> list(LanguageFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(languageRepository, filter.toSpecification(), pageable, LanguageDTO::from);
    }

    @GetMapping("/{key}")
    public LanguageDTO get(@PathVariable String key) {
        return queries.get(languageRepository, "language", key, LanguageDTO::from);
    }
}
