package com.main.app.creature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/creatures")
public class CreatureController {

    private static final Logger log = LoggerFactory.getLogger(CreatureController.class);

    private final CreatureService creatureService;

    public CreatureController(CreatureService creatureService) {
        this.creatureService = creatureService;
    }

    @GetMapping
    public List<CreatureDTO> getAllCreatures() {
        return creatureService.getAllCreatures();
    }

    @GetMapping("/{key}")
    public ResponseEntity<CreatureDTO> getCreatureByKey(@PathVariable String key) {
        log.debug("Fetching creature {}", key);
        return ResponseEntity.of(creatureService.getCreatureByKey(key));
    }

    @GetMapping("/test")
    public String test() {
        return "Creature controller works!";
    }
}
