package com.main.app.item;

import com.main.app.ownership.OwnedResource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A row of armor; key, document and derived-from come from {@link OwnedResource}. */
@Getter
@Setter
@Entity
@Table(name = "armor", schema = "open5e")
public class Armor extends OwnedResource {

    @Column(name = "ac_display")
    private String acDisplay;

    @Column(name = "category")
    private String category;

    @Column(name = "name")
    private String name;

    @Column(name = "grants_stealth_disadvantage")
    private Boolean grantsStealthDisadvantage;

    @Column(name = "strength_score_required")
    private Integer strengthScoreRequired;

    @Column(name = "ac_base")
    private Integer acBase;

    @Column(name = "ac_add_dexmod")
    private Boolean acAddDexmod;

    @Column(name = "ac_cap_dexmod")
    private Integer acCapDexmod;
}
