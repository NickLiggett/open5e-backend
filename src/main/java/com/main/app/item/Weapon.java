package com.main.app.item;

import com.main.app.common.NamedReference;
import com.main.app.ownership.OwnedResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/** A row of weapons; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "weapons", schema = "open5e")
public class Weapon extends OwnedResource {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties")
    private List<WeaponPropertyUse> properties;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "damage_type")
    private NamedReference damageType;

    @Column(name = "distance_unit")
    private String distanceUnit;

    @Column(name = "name")
    private String name;

    @Column(name = "damage_dice")
    private String damageDice;

    @Column(name = "range")
    private Integer range;

    @Column(name = "long_range")
    private Integer longRange;

    @Column(name = "is_simple")
    private Boolean isSimple;

    @Column(name = "is_improvised")
    private Boolean isImprovised;
}
