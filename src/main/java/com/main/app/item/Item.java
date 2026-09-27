package com.main.app.item;

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

import java.math.BigDecimal;

/** A row of items; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "items", schema = "open5e")
public class Item extends OwnedResource {

    @Column(name = "name")
    private String name;

    @Column(name = "\"desc\"")
    private String desc;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category")
    private NamedReference category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weapon")
    private WeaponStats weapon;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "armor")
    private ArmorStats armor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "size")
    private NamedReference size;

    @Column(name = "weight")
    private BigDecimal weight;

    @Column(name = "weight_unit")
    private String weightUnit;

    @Column(name = "cost")
    private BigDecimal cost;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crossreferences")
    private CrossReferences crossreferences;

    @Column(name = "category_key")
    private String categoryKey;

    /** Keeps the key columns used by list filters in step with the objects they come from. */
    @PrePersist
    @PreUpdate
    void syncKeys() {
        categoryKey = category == null ? null : category.key();
    }
}
