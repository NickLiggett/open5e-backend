package com.main.app.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A row of licenses: global reference data, visible to everyone. */
@Getter
@Setter
@Entity
@Table(name = "licenses", schema = "open5e")
public class License {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "name")
    private String name;

    @Column(name = "\"desc\"")
    private String desc;
}
