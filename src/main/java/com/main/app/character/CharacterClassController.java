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
@RequestMapping("/api/classes")
public class CharacterClassController {

    private final CharacterClassRepository characterClassRepository;
    private final ResourceQueries queries;

    public CharacterClassController(CharacterClassRepository characterClassRepository, ResourceQueries queries) {
        this.characterClassRepository = characterClassRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<CharacterClassDTO> list(CharacterClassFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(characterClassRepository, filter.toSpecification(), pageable, CharacterClassDTO::from);
    }

    @GetMapping("/{key}")
    public CharacterClassDTO get(@PathVariable String key) {
        return queries.get(characterClassRepository, "class", key, CharacterClassDTO::from);
    }
}
