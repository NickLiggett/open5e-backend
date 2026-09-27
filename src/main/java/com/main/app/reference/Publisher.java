package com.main.app.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A row of publishers: global reference data, visible to everyone. */
@Getter
@Setter
@Entity
@Table(name = "publishers", schema = "open5e")
public class Publisher {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "name")
    private String name;
}
