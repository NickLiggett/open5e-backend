package com.main.app.character;

import com.main.app.common.CrossReferences;
import com.main.app.ownership.OwnedResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/** A row of species; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "species", schema = "open5e")
public class Species extends OwnedResource {

    @Column(name = "is_subspecies")
    private Boolean isSubspecies;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "traits")
    private List<SpeciesTrait> traits;

    @Column(name = "name")
    private String name;

    @Column(name = "\"desc\"")
    private String desc;

    @Column(name = "subspecies_of_key")
    private String subspeciesOfKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crossreferences")
    private CrossReferences crossreferences;
}
