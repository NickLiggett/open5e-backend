package com.main.app.reference;

import com.main.app.common.Description;
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

/** A row of abilities; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "abilities", schema = "open5e")
public class Ability extends OwnedResource {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "descriptions")
    private List<Description> descriptions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills")
    private List<NamedReference> skills;

    @Column(name = "name")
    private String name;

    @Column(name = "short_desc")
    private String shortDesc;
}
