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

import java.math.BigDecimal;

/** A row of services; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "services", schema = "open5e")
public class ServiceOffering extends OwnedResource {

    @Column(name = "name")
    private String name;

    @Column(name = "\"desc\"")
    private String desc;

    @Column(name = "cost")
    private BigDecimal cost;

    @Column(name = "detail")
    private String detail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crossreferences")
    private CrossReferences crossreferences;
}
