package com.main.app.service;

import com.main.app.entity.Creature;
import com.main.app.respository.CreatureRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CreatureService {

    private final CreatureRepository creatureRepository;

    public CreatureService(CreatureRepository creatureRepository) {
        this.creatureRepository = creatureRepository;
    }

    public List<Creature> getAllCreatures() {
        return creatureRepository.findAll();
    }

    public Optional<Creature> getCreatureByKey(String id) {
        System.out.println("Service: " + id);
        return creatureRepository.findById(id);
    }
    // ...
}
