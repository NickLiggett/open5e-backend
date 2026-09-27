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
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseRepository licenseRepository;
    private final ResourceQueries queries;

    public LicenseController(LicenseRepository licenseRepository, ResourceQueries queries) {
        this.licenseRepository = licenseRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<LicenseDTO> list(LicenseFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(licenseRepository, filter.toSpecification(), pageable, LicenseDTO::from);
    }

    @GetMapping("/{key}")
    public LicenseDTO get(@PathVariable String key) {
        return queries.get(licenseRepository, "license", key, LicenseDTO::from);
    }
}
