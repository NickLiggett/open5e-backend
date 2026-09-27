package com.main.app.creature;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;
import com.main.app.common.ImageReference;
import com.main.app.common.NamedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * A creature stat block. The {@code jsonb} columns are mapped onto records by Hibernate (see
 * {@link com.main.app.common.json.HibernateJsonConfig}).
 */
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document")
    private DocumentSummary document;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "type")
    private NamedReference type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "size")
    private NamedReference size;

    @Column(name = "challenge_rating")
    private Float challengeRating;

    @Column(name = "proficiency_bonus")
    private Integer proficiencyBonus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "speed")
    private CreatureSpeed speed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "speed_all")
    private CreatureSpeed speedAll;

    @Column(name = "category")
    private String category;

    @Column(name = "subcategory")
    private String subcategory;

    @Column(name = "alignment")
    private String alignment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "languages")
    private CreatureLanguages languages;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ability_scores")
    private AbilityScores abilityScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modifiers")
    private AbilityScores modifiers;

    @Column(name = "initiative_bonus")
    private Integer initiativeBonus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "saving_throws")
    private AbilityScores savingThrows;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "saving_throws_all")
    private AbilityScores savingThrowsAll;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skill_bonuses")
    private SkillBonuses skillBonuses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skill_bonuses_all")
    private SkillBonuses skillBonusesAll;

    @Column(name = "passive_perception")
    private Integer passivePerception;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resistances_and_immunities")
    private ResistancesAndImmunities resistancesAndImmunities;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actions")
    private List<CreatureAction> actions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "traits")
    private List<CreatureTrait> traits;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "creaturesets")
    private List<String> creatureSets;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "environments")
    private List<NamedReference> environments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "illustration")
    private ImageReference illustration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crossreferences")
    private CrossReferences crossreferences;

    public Creature() {
    }

    public Creature(String key) {
        this.key = key;
    }
}
