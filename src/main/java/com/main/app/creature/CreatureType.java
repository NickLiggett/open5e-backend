package com.main.app.creature;

import com.main.app.common.Description;
import com.main.app.ownership.OwnedResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/** A row of creaturetypes; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "creaturetypes", schema = "open5e")
public class CreatureType extends OwnedResource {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "descriptions")
    private List<Description> descriptions;

    @Column(name = "name")
    private String name;
}
