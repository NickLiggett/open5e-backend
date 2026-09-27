package com.main.app.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A row of itemrarities: global reference data, visible to everyone. */
@Getter
@Setter
@Entity
@Table(name = "itemrarities", schema = "open5e")
public class ItemRarity {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "name")
    private String name;

    @Column(name = "rank")
    private Integer rank;
}
