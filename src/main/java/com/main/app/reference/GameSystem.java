package com.main.app.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A row of gamesystems: global reference data, visible to everyone. */
@Getter
@Setter
@Entity
@Table(name = "gamesystems", schema = "open5e")
public class GameSystem {

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "name")
    private String name;

    @Column(name = "\"desc\"")
    private String desc;

    @Column(name = "content_prefix")
    private String contentPrefix;
}
