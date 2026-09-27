package com.main.app.item;

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

import java.util.List;

/** A row of itemsets; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "itemsets", schema = "open5e")
public class ItemSet extends OwnedResource {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items")
    private List<NamedReference> items;

    @Column(name = "name")
    private String name;

    @Column(name = "\"desc\"")
    private String desc;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crossreferences")
    private CrossReferences crossreferences;
}
