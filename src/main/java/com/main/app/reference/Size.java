package com.main.app.reference;

import com.main.app.ownership.OwnedResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A row of sizes; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "sizes", schema = "open5e")
public class Size extends OwnedResource {

    @Column(name = "distance_unit")
    private String distanceUnit;

    @Column(name = "name")
    private String name;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "space_diameter")
    private Integer spaceDiameter;

    @Column(name = "suggested_hit_dice")
    private String suggestedHitDice;
}
