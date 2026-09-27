package com.main.app.item;

import com.main.app.common.CrossReferences;
import com.main.app.ownership.OwnedResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A row of weaponproperties; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "weaponproperties", schema = "open5e")
public class WeaponProperty extends OwnedResource {

    @Column(name = "name")
    private String name;

    @Column(name = "\"desc\"")
    private String desc;

    @Column(name = "type")
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crossreferences")
    private CrossReferences crossreferences;
}
