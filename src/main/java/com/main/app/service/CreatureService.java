package com.main.app.service;

import com.main.app.dtos.Creature.CreatureDTO;
import com.main.app.repository.CreatureRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CreatureService {

    private final CreatureRepository creatureRepository;
    private final CreatureMapper creatureMapper;

    public CreatureService(CreatureRepository creatureRepository, CreatureMapper creatureMapper) {
        this.creatureRepository = creatureRepository;
        this.creatureMapper = creatureMapper;
    }

    public List<CreatureDTO> getAllCreatures() {
        return creatureRepository.findAll().stream()
                .map(creatureMapper::toDto)
                .toList();
    }

    public Optional<CreatureDTO> getCreatureByKey(String key) {
        return creatureRepository.findById(key).map(creatureMapper::toDto);
    }
}
