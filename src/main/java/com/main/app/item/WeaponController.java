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
@RequestMapping("/api/weapons")
public class WeaponController {

    private final WeaponRepository weaponRepository;
    private final ResourceQueries queries;

    public WeaponController(WeaponRepository weaponRepository, ResourceQueries queries) {
        this.weaponRepository = weaponRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<WeaponDTO> list(WeaponFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(weaponRepository, filter.toSpecification(), pageable, WeaponDTO::from);
    }

    @GetMapping("/{key}")
    public WeaponDTO get(@PathVariable String key) {
        return queries.get(weaponRepository, "weapon", key, WeaponDTO::from);
    }
}
