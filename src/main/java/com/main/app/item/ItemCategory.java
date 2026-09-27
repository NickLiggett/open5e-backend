package com.main.app.item;

import com.main.app.ownership.OwnedResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A row of itemcategories; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "itemcategories", schema = "open5e")
public class ItemCategory extends OwnedResource {

    @Column(name = "name")
    private String name;
}
