-- Baseline schema for the open5e database, taken from open5e_backup.dump (PostgreSQL 18).
-- Flyway creates the open5e schema itself (spring.flyway.schemas), so it is not created here.
-- Existing databases are baselined at this version instead of running it (see application.properties).

-- Reference data

CREATE TABLE open5e.publishers (
    key  text NOT NULL,
    name text,
    CONSTRAINT publishers_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.gamesystems (
    key            text NOT NULL,
    name           text,
    "desc"         text,
    content_prefix text,
    CONSTRAINT gamesystems_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.licenses (
    key    text NOT NULL,
    name   text,
    "desc" text,
    CONSTRAINT licenses_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.documents (
    key              text NOT NULL,
    licenses         jsonb,
    publisher        jsonb,
    gamesystem       jsonb,
    display_name     text,
    name             text,
    "desc"           text,
    type             text,
    author           text,
    publication_date timestamp with time zone,
    permalink        text,
    distance_unit    text,
    weight_unit      text,
    CONSTRAINT documents_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.abilities (
    key          text NOT NULL,
    descriptions jsonb,
    skills       jsonb,
    name         text,
    short_desc   text,
    document_key text,
    CONSTRAINT abilities_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.skills (
    key          text NOT NULL,
    descriptions jsonb,
    name         text,
    document_key text,
    ability      text,
    CONSTRAINT skills_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.sizes (
    key                text NOT NULL,
    document           jsonb,
    distance_unit      text,
    name               text,
    rank               integer,
    space_diameter     integer,
    suggested_hit_dice text,
    document_key       text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT sizes_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.alignments (
    key               text NOT NULL,
    morality          text,
    societal_attitude text,
    short_name        text,
    descriptions      jsonb,
    document          jsonb,
    document_key      text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT alignments_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.languages (
    key             text NOT NULL,
    document        jsonb,
    name            text,
    "desc"          text,
    is_exotic       boolean,
    is_secret       boolean,
    script_language text,
    crossreferences jsonb,
    document_key    text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT languages_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.damagetypes (
    key          text NOT NULL,
    descriptions jsonb,
    name         text,
    document_key text,
    CONSTRAINT damagetypes_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.conditions (
    key          text NOT NULL,
    document     jsonb,
    icon         jsonb,
    descriptions jsonb,
    name         text,
    document_key text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT conditions_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.environments (
    key             text NOT NULL,
    name            text,
    "desc"          text,
    aquatic         boolean,
    planar          boolean,
    interior        boolean,
    document_key    text,
    crossreferences jsonb,
    CONSTRAINT environments_pkey PRIMARY KEY (key)
);

-- Items and equipment

CREATE TABLE open5e.itemrarities (
    name text,
    key  text NOT NULL,
    rank integer,
    CONSTRAINT itemrarities_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.itemcategories (
    key          text NOT NULL,
    document     jsonb,
    name         text,
    document_key text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT itemcategories_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.itemsets (
    key             text NOT NULL,
    items           jsonb,
    name            text,
    "desc"          text,
    document_key    text,
    crossreferences jsonb,
    CONSTRAINT itemsets_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.items (
    key             text NOT NULL,
    name            text,
    "desc"          text,
    category        jsonb,
    weapon          jsonb,
    armor           jsonb,
    size            jsonb,
    weight          numeric,
    weight_unit     text,
    cost            numeric,
    document        jsonb,
    crossreferences jsonb,
    category_key    text GENERATED ALWAYS AS ((category ->> 'key'::text)) STORED,
    document_key    text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT items_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.magicitems (
    key                 text NOT NULL,
    name                text,
    "desc"              text,
    category            jsonb,
    rarity              jsonb,
    weapon              jsonb,
    armor               jsonb,
    size                jsonb,
    weight              numeric,
    weight_unit         text,
    cost                numeric,
    requires_attunement boolean,
    attunement_detail   text,
    document            jsonb,
    crossreferences     jsonb,
    category_key        text GENERATED ALWAYS AS ((category ->> 'key'::text)) STORED,
    document_key        text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    rarity_key          text GENERATED ALWAYS AS ((rarity ->> 'key'::text)) STORED,
    CONSTRAINT magicitems_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.weaponproperties (
    key             text NOT NULL,
    name            text,
    "desc"          text,
    document_key    text,
    type            text,
    crossreferences jsonb,
    CONSTRAINT weaponproperties_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.weapons (
    key           text NOT NULL,
    document      jsonb,
    properties    jsonb,
    damage_type   jsonb,
    distance_unit text,
    name          text,
    damage_dice   text,
    range         integer,
    long_range    integer,
    is_simple     boolean,
    is_improvised boolean,
    document_key  text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT weapons_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.armor (
    key                         text NOT NULL,
    ac_display                  text,
    category                    text,
    document                    jsonb,
    name                        text,
    grants_stealth_disadvantage boolean,
    strength_score_required     integer,
    ac_base                     integer,
    ac_add_dexmod               boolean,
    ac_cap_dexmod               integer,
    document_key                text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT armor_pkey PRIMARY KEY (key)
);

-- Spells

CREATE TABLE open5e.spellschools (
    key             text NOT NULL,
    name            text,
    "desc"          text,
    document_key    text,
    crossreferences jsonb,
    CONSTRAINT spellschools_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.spells (
    key                text NOT NULL,
    document           jsonb,
    casting_options    jsonb,
    school             jsonb,
    classes            jsonb,
    range_unit         text,
    shape_size_unit    text,
    name               text,
    "desc"             text,
    level              integer,
    higher_level       text,
    target_type        text,
    range_text         text,
    range              integer,
    ritual             boolean,
    casting_time       text,
    reaction_condition text,
    verbal             boolean,
    somatic            boolean,
    material           boolean,
    material_specified text,
    material_cost      numeric,
    material_consumed  boolean,
    target_count       integer,
    saving_throw_ability text,
    attack_roll        boolean,
    damage_roll        text,
    damage_types       jsonb,
    duration           text,
    shape_type         text,
    shape_size         integer,
    concentration      boolean,
    crossreferences    jsonb,
    document_key       text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT spells_pkey PRIMARY KEY (key)
);

-- Character options

CREATE TABLE open5e.classes (
    key               text NOT NULL,
    features          jsonb,
    document          jsonb,
    saving_throws     jsonb,
    subclass_of       jsonb,
    name              text,
    "desc"            text,
    hit_dice          text,
    caster_type       text,
    primary_abilities jsonb,
    crossreferences   jsonb,
    hit_points        jsonb,
    document_key      text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    subclass_of_key   text GENERATED ALWAYS AS ((subclass_of ->> 'key'::text)) STORED,
    CONSTRAINT classes_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.backgrounds (
    key             text NOT NULL,
    benefits        jsonb,
    document        jsonb,
    name            text,
    "desc"          text,
    crossreferences jsonb,
    document_key    text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT backgrounds_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.feats (
    key              text NOT NULL,
    has_prerequisite boolean,
    benefits         jsonb,
    document         jsonb,
    name             text,
    "desc"           text,
    prerequisite     text,
    type             text,
    crossreferences  jsonb,
    document_key     text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT feats_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.species (
    key               text NOT NULL,
    is_subspecies     boolean,
    document          jsonb,
    traits            jsonb,
    name              text,
    "desc"            text,
    subspecies_of_key text,
    crossreferences   jsonb,
    document_key      text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT species_pkey PRIMARY KEY (key)
);

-- Creatures

CREATE TABLE open5e.creaturetypes (
    key          text NOT NULL,
    descriptions jsonb,
    name         text,
    document_key text,
    CONSTRAINT creaturetypes_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.creaturesets (
    key          text NOT NULL,
    creatures    jsonb,
    name         text,
    document_key text,
    CONSTRAINT creaturesets_pkey PRIMARY KEY (key)
);

-- The JSON-shaped columns here are character varying (not jsonb) to match the existing data and entity mapping.
CREATE TABLE open5e.creatures (
    key                        text NOT NULL,
    name                       text,
    document                   character varying,
    type                       character varying,
    size                       character varying,
    challenge_rating           double precision,
    proficiency_bonus          integer,
    speed                      character varying,
    speed_all                  character varying,
    category                   text,
    subcategory                text,
    alignment                  text,
    languages                  character varying,
    armor_class                integer,
    armor_detail               text,
    hit_points                 integer,
    hit_dice                   text,
    experience_points          integer,
    ability_scores             character varying,
    modifiers                  character varying,
    initiative_bonus           integer,
    saving_throws              character varying,
    saving_throws_all          character varying,
    skill_bonuses              character varying,
    skill_bonuses_all          character varying,
    passive_perception         integer,
    resistances_and_immunities character varying,
    normal_sight_range         integer,
    darkvision_range           integer,
    blindsight_range           integer,
    tremorsense_range          integer,
    truesight_range            integer,
    actions                    character varying,
    traits                     character varying,
    creaturesets               character varying,
    environments               character varying,
    illustration               character varying,
    crossreferences            character varying,
    CONSTRAINT creatures_pkey PRIMARY KEY (key)
);

-- Rules and misc

CREATE TABLE open5e.rulesets (
    name            text,
    key             text NOT NULL,
    document        jsonb,
    "desc"          text,
    rules           jsonb,
    crossreferences jsonb,
    document_key    text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT rulesets_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.rules (
    key                  text NOT NULL,
    name                 text,
    "desc"               text,
    index                integer,
    initial_header_level integer,
    document_key         text,
    ruleset              text,
    crossreferences      jsonb,
    CONSTRAINT rules_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.images (
    name         text,
    key          text NOT NULL,
    file_url     text,
    alt_text     text,
    attribution  text,
    document     jsonb,
    document_key text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT images_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.services (
    key             text NOT NULL,
    document        jsonb,
    name            text,
    "desc"          text,
    cost            numeric,
    detail          text,
    crossreferences jsonb,
    document_key    text GENERATED ALWAYS AS ((document ->> 'key'::text)) STORED,
    CONSTRAINT services_pkey PRIMARY KEY (key)
);

-- Indexes

CREATE INDEX abilities_document_key_idx        ON open5e.abilities        USING btree (document_key);
CREATE INDEX alignments_document_key_idx       ON open5e.alignments       USING btree (document_key);
CREATE INDEX armor_document_key_idx            ON open5e.armor            USING btree (document_key);
CREATE INDEX backgrounds_document_key_idx      ON open5e.backgrounds      USING btree (document_key);
CREATE INDEX classes_document_key_idx          ON open5e.classes          USING btree (document_key);
CREATE INDEX classes_subclass_of_key_idx       ON open5e.classes          USING btree (subclass_of_key);
CREATE INDEX conditions_document_key_idx       ON open5e.conditions       USING btree (document_key);
CREATE INDEX creatures_cr_idx                  ON open5e.creatures        USING btree (challenge_rating);
CREATE INDEX creatures_name_lower_idx          ON open5e.creatures        USING btree (lower(name));
CREATE INDEX creaturesets_document_key_idx     ON open5e.creaturesets     USING btree (document_key);
CREATE INDEX creaturetypes_document_key_idx    ON open5e.creaturetypes    USING btree (document_key);
CREATE INDEX damagetypes_document_key_idx      ON open5e.damagetypes      USING btree (document_key);
CREATE INDEX environments_document_key_idx     ON open5e.environments     USING btree (document_key);
CREATE INDEX feats_document_key_idx            ON open5e.feats            USING btree (document_key);
CREATE INDEX images_document_key_idx           ON open5e.images           USING btree (document_key);
CREATE INDEX itemcategories_document_key_idx   ON open5e.itemcategories   USING btree (document_key);
CREATE INDEX items_category_key_idx            ON open5e.items            USING btree (category_key);
CREATE INDEX items_document_key_idx            ON open5e.items            USING btree (document_key);
CREATE INDEX items_name_lower_idx              ON open5e.items            USING btree (lower(name));
CREATE INDEX itemsets_document_key_idx         ON open5e.itemsets         USING btree (document_key);
CREATE INDEX languages_document_key_idx        ON open5e.languages        USING btree (document_key);
CREATE INDEX magicitems_category_key_idx       ON open5e.magicitems       USING btree (category_key);
CREATE INDEX magicitems_document_key_idx       ON open5e.magicitems       USING btree (document_key);
CREATE INDEX magicitems_name_lower_idx         ON open5e.magicitems       USING btree (lower(name));
CREATE INDEX magicitems_rarity_key_idx         ON open5e.magicitems       USING btree (rarity_key);
CREATE INDEX rules_document_key_idx            ON open5e.rules            USING btree (document_key);
CREATE INDEX rulesets_document_key_idx         ON open5e.rulesets         USING btree (document_key);
CREATE INDEX services_document_key_idx         ON open5e.services         USING btree (document_key);
CREATE INDEX sizes_document_key_idx            ON open5e.sizes            USING btree (document_key);
CREATE INDEX skills_document_key_idx           ON open5e.skills           USING btree (document_key);
CREATE INDEX species_document_key_idx          ON open5e.species          USING btree (document_key);
CREATE INDEX species_subspecies_of_key_idx     ON open5e.species          USING btree (subspecies_of_key);
CREATE INDEX spells_classes_gin                ON open5e.spells           USING gin (classes);
CREATE INDEX spells_damage_types_gin           ON open5e.spells           USING gin (damage_types);
CREATE INDEX spells_document_key_idx           ON open5e.spells           USING btree (document_key);
CREATE INDEX spells_level_idx                  ON open5e.spells           USING btree (level);
CREATE INDEX spells_name_lower_idx             ON open5e.spells           USING btree (lower(name));
CREATE INDEX spellschools_document_key_idx     ON open5e.spellschools     USING btree (document_key);
CREATE INDEX weaponproperties_document_key_idx ON open5e.weaponproperties USING btree (document_key);
CREATE INDEX weapons_document_key_idx          ON open5e.weapons          USING btree (document_key);

-- Foreign keys (deferred so data can be bulk-loaded in any table order within a transaction)

ALTER TABLE open5e.abilities        ADD CONSTRAINT abilities_document_key_fk        FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.alignments       ADD CONSTRAINT alignments_document_key_fk       FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.armor            ADD CONSTRAINT armor_document_key_fk            FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.backgrounds      ADD CONSTRAINT backgrounds_document_key_fk      FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.classes          ADD CONSTRAINT classes_document_key_fk          FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.classes          ADD CONSTRAINT classes_subclass_of_key_fk       FOREIGN KEY (subclass_of_key)   REFERENCES open5e.classes (key)        DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.conditions       ADD CONSTRAINT conditions_document_key_fk       FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.creaturesets     ADD CONSTRAINT creaturesets_document_key_fk     FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.creaturetypes    ADD CONSTRAINT creaturetypes_document_key_fk    FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.damagetypes      ADD CONSTRAINT damagetypes_document_key_fk      FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.environments     ADD CONSTRAINT environments_document_key_fk     FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.feats            ADD CONSTRAINT feats_document_key_fk            FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.images           ADD CONSTRAINT images_document_key_fk           FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.itemcategories   ADD CONSTRAINT itemcategories_document_key_fk   FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.items            ADD CONSTRAINT items_category_key_fk            FOREIGN KEY (category_key)      REFERENCES open5e.itemcategories (key) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.items            ADD CONSTRAINT items_document_key_fk            FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.itemsets         ADD CONSTRAINT itemsets_document_key_fk         FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.languages        ADD CONSTRAINT languages_document_key_fk        FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.magicitems       ADD CONSTRAINT magicitems_category_key_fk       FOREIGN KEY (category_key)      REFERENCES open5e.itemcategories (key) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.magicitems       ADD CONSTRAINT magicitems_document_key_fk       FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.magicitems       ADD CONSTRAINT magicitems_rarity_key_fk         FOREIGN KEY (rarity_key)        REFERENCES open5e.itemrarities (key)   DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.rules            ADD CONSTRAINT rules_document_key_fk            FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.rulesets         ADD CONSTRAINT rulesets_document_key_fk         FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.services         ADD CONSTRAINT services_document_key_fk         FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.sizes            ADD CONSTRAINT sizes_document_key_fk            FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.skills           ADD CONSTRAINT skills_document_key_fk           FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.species          ADD CONSTRAINT species_document_key_fk          FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.species          ADD CONSTRAINT species_subspecies_of_key_fk     FOREIGN KEY (subspecies_of_key) REFERENCES open5e.species (key)        DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.spells           ADD CONSTRAINT spells_document_key_fk           FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.spellschools     ADD CONSTRAINT spellschools_document_key_fk     FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.weaponproperties ADD CONSTRAINT weaponproperties_document_key_fk FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE open5e.weapons          ADD CONSTRAINT weapons_document_key_fk          FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
