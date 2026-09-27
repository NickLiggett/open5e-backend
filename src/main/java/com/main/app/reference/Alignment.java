package com.main.app.reference;

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

/** A row of alignments; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "alignments", schema = "open5e")
public class Alignment extends OwnedResource {

    @Column(name = "morality")
    private String morality;

    @Column(name = "societal_attitude")
    private String societalAttitude;

    @Column(name = "short_name")
    private String shortName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "descriptions")
    private List<Description> descriptions;
}
