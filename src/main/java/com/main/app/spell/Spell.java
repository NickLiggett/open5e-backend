package com.main.app.spell;

import com.main.app.common.CrossReferences;
import com.main.app.common.NamedReference;
import com.main.app.ownership.OwnedResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;

/** A row of spells; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "spells", schema = "open5e")
public class Spell extends OwnedResource {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "casting_options")
    private List<CastingOption> castingOptions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "school")
    private NamedReference school;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "classes")
    private List<NamedReference> classes;

    @Column(name = "range_unit")
    private String rangeUnit;

    @Column(name = "shape_size_unit")
    private String shapeSizeUnit;

    @Column(name = "name")
    private String name;

    @Column(name = "\"desc\"")
    private String desc;

    @Column(name = "level")
    private Integer level;

    @Column(name = "higher_level")
    private String higherLevel;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "range_text")
    private String rangeText;

    @Column(name = "range")
    private Integer range;

    @Column(name = "ritual")
    private Boolean ritual;

    @Column(name = "casting_time")
    private String castingTime;

    @Column(name = "reaction_condition")
    private String reactionCondition;

    @Column(name = "verbal")
    private Boolean verbal;

    @Column(name = "somatic")
    private Boolean somatic;

    @Column(name = "material")
    private Boolean material;

    @Column(name = "material_specified")
    private String materialSpecified;

    @Column(name = "material_cost")
    private BigDecimal materialCost;

    @Column(name = "material_consumed")
    private Boolean materialConsumed;

    @Column(name = "target_count")
    private Integer targetCount;

    @Column(name = "saving_throw_ability")
    private String savingThrowAbility;

    @Column(name = "attack_roll")
    private Boolean attackRoll;

    @Column(name = "damage_roll")
    private String damageRoll;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "damage_types")
    private List<String> damageTypes;

    @Column(name = "duration")
    private String duration;

    @Column(name = "shape_type")
    private String shapeType;

    @Column(name = "shape_size")
    private Integer shapeSize;

    @Column(name = "concentration")
    private Boolean concentration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crossreferences")
    private CrossReferences crossreferences;
}
