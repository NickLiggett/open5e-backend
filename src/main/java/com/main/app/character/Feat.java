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

/** A row of feats; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "feats", schema = "open5e")
public class Feat extends OwnedResource {

    @Column(name = "has_prerequisite")
    private Boolean hasPrerequisite;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "benefits")
    private List<Benefit> benefits;

    @Column(name = "name")
    private String name;

    @Column(name = "\"desc\"")
    private String desc;

    @Column(name = "prerequisite")
    private String prerequisite;

    @Column(name = "type")
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crossreferences")
    private CrossReferences crossreferences;
}
