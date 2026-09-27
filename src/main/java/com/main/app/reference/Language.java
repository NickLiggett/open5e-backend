package com.main.app.reference;

import com.main.app.common.CrossReferences;
import com.main.app.ownership.OwnedResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A row of languages; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "languages", schema = "open5e")
public class Language extends OwnedResource {

    @Column(name = "name")
    private String name;

    @Column(name = "\"desc\"")
    private String desc;

    @Column(name = "is_exotic")
    private Boolean isExotic;

    @Column(name = "is_secret")
    private Boolean isSecret;

    @Column(name = "script_language")
    private String scriptLanguage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crossreferences")
    private CrossReferences crossreferences;
}
