package com.main.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "creatures", schema = "open5e")
public class Creature {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "document")
    private String document;

    @Column(name = "type")
    private String type;

    @Column(name = "size")
    private String size;

    @Column(name = "challenge_rating")
    private Float challengeRating;

    @Column(name = "proficiency_bonus")
    private Integer proficiencyBonus;

    @Column(name = "speed")
    private String speed;

    @Column(name = "speed_all")
    private String speedAll;

    @Column(name = "category")
    private String category;

    @Column(name = "subcategory")
    private String subcategory;

    @Column(name = "alignment")
    private String alignment;

    @Column(name = "languages")
    private String languages;

    @Column(name = "armor_class")
    private Integer armorClass;

    @Column(name = "armor_detail")
    private String armorDetail;

    @Column(name = "hit_points")
    private Integer hitPoints;

    @Column(name = "hit_dice")
    private String hitDice;

    @Column(name = "experience_points")
    private Integer experiencePoints;

    @Column(name = "ability_scores")
    private String abilityScores;

    @Column(name = "modifiers")
    private String modifiers;

    @Column(name = "initiative_bonus")
    private Integer initiativeBonus;

    @Column(name = "saving_throws")
    private String savingThrows;

    @Column(name = "saving_throws_all")
    private String savingThrowsAll;

    @Column(name = "skill_bonuses")
    private String skillBonuses;

    @Column(name = "skill_bonuses_all")
    private String skillBonusesAll;

    @Column(name = "passive_perception")
    private Integer passivePerception;

    @Column(name = "resistances_and_immunities")
    private String resistancesAndImmunities;

    @Column(name = "normal_sight_range")
    private Integer normalSightRange;

    @Column(name = "darkvision_range")
    private Integer darkvisionRange;

    @Column(name = "blindsight_range")
    private Integer blindsightRange;

    @Column(name = "tremorsense_range")
    private Integer tremorsenseRange;

    @Column(name = "truesight_range")
    private Integer truesightRange;

    @Column(name = "actions")
    private String actions;

    @Column(name = "traits")
    private String traits;

    @Column(name = "creaturesets")
    private String creatureSets;

    @Column(name = "environments")
    private String environments;

    @Column(name = "illustration")
    private String illustration;

    @Column(name = "crossreferences")
    private String crossreferences;

    public Creature() {
    }

    public Creature(String key) {
        this.key = key;
    }

    // getters and setters
}
