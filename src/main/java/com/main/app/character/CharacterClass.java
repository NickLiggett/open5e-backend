package com.main.app.character;

import com.main.app.common.CrossReferences;
import com.main.app.common.NamedReference;
import com.main.app.ownership.OwnedResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/** A row of classes; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "classes", schema = "open5e")
public class CharacterClass extends OwnedResource {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features")
    private List<ClassFeature> features;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "saving_throws")
    private List<NamedReference> savingThrows;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subclass_of")
    private NamedReference subclassOf;

    @Column(name = "name")
    private String name;

    @Column(name = "\"desc\"")
    private String desc;

    @Column(name = "hit_dice")
    private String hitDice;

    @Column(name = "caster_type")
    private String casterType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "primary_abilities")
    private List<NamedReference> primaryAbilities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crossreferences")
    private CrossReferences crossreferences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hit_points")
    private HitPoints hitPoints;

    @Column(name = "subclass_of_key")
    private String subclassOfKey;

    /** Keeps the key columns used by list filters in step with the objects they come from. */
    @PrePersist
    @PreUpdate
    void syncKeys() {
        subclassOfKey = subclassOf == null ? null : subclassOf.key();
    }
}
