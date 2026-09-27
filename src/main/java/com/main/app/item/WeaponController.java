package com.main.app.item;

import com.main.app.common.web.ResourceQueries;
import com.main.app.ownership.CopyRequest;
import com.main.app.ownership.ResourceType;
import com.main.app.ownership.ResourceWriter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/weapons")
public class WeaponController {

    private static final ResourceType<Weapon, WeaponDTO> TYPE =
            new ResourceType<>(Weapon.class, WeaponDTO.class, "/api/weapons", "weapon", WeaponDTO::from);

    private final WeaponRepository weaponRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public WeaponController(WeaponRepository weaponRepository, ResourceQueries queries, ResourceWriter writer) {
        this.weaponRepository = weaponRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<WeaponDTO> list(WeaponFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(weaponRepository, filter.toSpecification(), pageable, WeaponDTO::from);
    }

    @GetMapping("/{key}")
    public WeaponDTO get(@PathVariable String key) {
        return queries.get(weaponRepository, "weapon", key, WeaponDTO::from);
    }

    @PostMapping
    public ResponseEntity<WeaponDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public WeaponDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public WeaponDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<WeaponDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
