package com.main.app.controller;

import com.main.app.entity.Creature;
import com.main.app.service.CreatureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/creatures")
public class CreatureController {

    private final CreatureService creatureService;

    public CreatureController(CreatureService creatureService) {
        this.creatureService = creatureService;
    }

    @GetMapping
    public List<Creature> getAllCreatures() {
        return creatureService.getAllCreatures();
    }

    @GetMapping("/{id}")
    public Optional<Creature> getCreatureByKey(@PathVariable String id) {
        System.out.println("id: " + id);
        return creatureService.getCreatureByKey(id);
    }

    @GetMapping("/test")
    public String test() {
        return "Creature controller works!";
    }
}
