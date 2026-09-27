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
@RequestMapping("/api/gamesystems")
public class GameSystemController {

    private final GameSystemRepository gameSystemRepository;
    private final ResourceQueries queries;

    public GameSystemController(GameSystemRepository gameSystemRepository, ResourceQueries queries) {
        this.gameSystemRepository = gameSystemRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<GameSystemDTO> list(GameSystemFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(gameSystemRepository, filter.toSpecification(), pageable, GameSystemDTO::from);
    }

    @GetMapping("/{key}")
    public GameSystemDTO get(@PathVariable String key) {
        return queries.get(gameSystemRepository, "game system", key, GameSystemDTO::from);
    }
}
