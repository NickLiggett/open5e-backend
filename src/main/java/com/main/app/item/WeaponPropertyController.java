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
@RequestMapping("/api/weaponproperties")
public class WeaponPropertyController {

    private final WeaponPropertyRepository weaponPropertyRepository;
    private final ResourceQueries queries;

    public WeaponPropertyController(WeaponPropertyRepository weaponPropertyRepository, ResourceQueries queries) {
        this.weaponPropertyRepository = weaponPropertyRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<WeaponPropertyDTO> list(WeaponPropertyFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(weaponPropertyRepository, filter.toSpecification(), pageable, WeaponPropertyDTO::from);
    }

    @GetMapping("/{key}")
    public WeaponPropertyDTO get(@PathVariable String key) {
        return queries.get(weaponPropertyRepository, "weapon property", key, WeaponPropertyDTO::from);
    }
}
