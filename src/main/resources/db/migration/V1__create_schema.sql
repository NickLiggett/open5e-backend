-- The open5e schema: the 33 Open5e content tables (they mirror the Open5e v2 API, endpoint for table and field for
-- column), plus users and sharing. Flyway creates the open5e schema itself (spring.flyway.schemas).
--
-- Ownership: every resource belongs to a document (document_key). Documents with no owner are default content,
-- visible to everyone and read-only; users' own content lives in documents they own, shared through
-- document_members. See docs/PLAN.md.

-- Users and sharing

-- username identifies dev-profile users (X-User header); idp_subject is the identity provider's issuer and subject.
CREATE TABLE open5e.users (
    id           bigint GENERATED ALWAYS AS IDENTITY,
    username     text        NOT NULL,
    idp_subject  text,
    display_name text,
    created_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_username_key UNIQUE (username),
    CONSTRAINT users_idp_subject_key UNIQUE (idp_subject)
);

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
    owner_id         bigint,
    CONSTRAINT documents_pkey PRIMARY KEY (key)
);

-- Sharing: members can see a document's content (VIEWER) or also change it (EDITOR).
CREATE TABLE open5e.document_members (
    document_key text        NOT NULL,
    user_id      bigint      NOT NULL,
    role         text        NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT document_members_pkey PRIMARY KEY (document_key, user_id),
    CONSTRAINT document_members_role_check CHECK (role IN ('VIEWER', 'EDITOR')),
    CONSTRAINT document_members_document_key_fk FOREIGN KEY (document_key) REFERENCES open5e.documents (key) ON DELETE CASCADE,
    CONSTRAINT document_members_user_id_fk FOREIGN KEY (user_id) REFERENCES open5e.users (id) ON DELETE CASCADE
);

CREATE TABLE open5e.abilities (
    key          text NOT NULL,
    descriptions jsonb,
    skills       jsonb,
    name         text,
    short_desc   text,
    document_key text NOT NULL,
    derived_from text,
    CONSTRAINT abilities_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.skills (
    key          text NOT NULL,
    descriptions jsonb,
    name         text,
    document_key text NOT NULL,
    ability      text,
    derived_from text,
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
    document_key       text NOT NULL,
    derived_from       text,
    CONSTRAINT sizes_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.alignments (
    key               text NOT NULL,
    morality          text,
    societal_attitude text,
    short_name        text,
    descriptions      jsonb,
    document          jsonb,
    document_key      text NOT NULL,
    derived_from      text,
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
    document_key    text NOT NULL,
    derived_from    text,
    CONSTRAINT languages_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.damagetypes (
    key          text NOT NULL,
    descriptions jsonb,
    name         text,
    document_key text NOT NULL,
    derived_from text,
    CONSTRAINT damagetypes_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.conditions (
    key          text NOT NULL,
    document     jsonb,
    icon         jsonb,
    descriptions jsonb,
    name         text,
    document_key text NOT NULL,
    derived_from text,
    CONSTRAINT conditions_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.environments (
    key             text NOT NULL,
    name            text,
    "desc"          text,
    aquatic         boolean,
    planar          boolean,
    interior        boolean,
    document_key    text NOT NULL,
    crossreferences jsonb,
    derived_from    text,
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
    document_key text NOT NULL,
    derived_from text,
    CONSTRAINT itemcategories_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.itemsets (
    key             text NOT NULL,
    items           jsonb,
    name            text,
    "desc"          text,
    document_key    text NOT NULL,
    crossreferences jsonb,
    derived_from    text,
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
    category_key    text,
    document_key    text NOT NULL,
    derived_from    text,
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
    category_key        text,
    document_key        text NOT NULL,
    rarity_key          text,
    derived_from        text,
    CONSTRAINT magicitems_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.weaponproperties (
    key             text NOT NULL,
    name            text,
    "desc"          text,
    document_key    text NOT NULL,
    type            text,
    crossreferences jsonb,
    derived_from    text,
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
    document_key  text NOT NULL,
    derived_from  text,
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
    document_key                text NOT NULL,
    derived_from                text,
    CONSTRAINT armor_pkey PRIMARY KEY (key)
);

-- Spells

CREATE TABLE open5e.spellschools (
    key             text NOT NULL,
    name            text,
    "desc"          text,
    document_key    text NOT NULL,
    crossreferences jsonb,
    derived_from    text,
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
    document_key       text NOT NULL,
    derived_from         text,
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
    document_key      text NOT NULL,
    subclass_of_key   text,
    derived_from      text,
    CONSTRAINT classes_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.backgrounds (
    key             text NOT NULL,
    benefits        jsonb,
    document        jsonb,
    name            text,
    "desc"          text,
    crossreferences jsonb,
    document_key    text NOT NULL,
    derived_from    text,
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
    document_key     text NOT NULL,
    derived_from     text,
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
    document_key      text NOT NULL,
    derived_from      text,
    CONSTRAINT species_pkey PRIMARY KEY (key)
);

-- Creatures

CREATE TABLE open5e.creaturetypes (
    key          text NOT NULL,
    descriptions jsonb,
    name         text,
    document_key text NOT NULL,
    derived_from text,
    CONSTRAINT creaturetypes_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.creaturesets (
    key          text NOT NULL,
    creatures    jsonb,
    name         text,
    document_key text NOT NULL,
    derived_from text,
    CONSTRAINT creaturesets_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.creatures (
    key                        text NOT NULL,
    name                       text,
    document                   jsonb,
    type                       jsonb,
    size                       jsonb,
    challenge_rating           double precision,
    proficiency_bonus          integer,
    speed                      jsonb,
    speed_all                  jsonb,
    category                   text,
    subcategory                text,
    alignment                  text,
    languages                  jsonb,
    armor_class                integer,
    armor_detail               text,
    hit_points                 integer,
    hit_dice                   text,
    experience_points          integer,
    ability_scores             jsonb,
    modifiers                  jsonb,
    initiative_bonus           integer,
    saving_throws              jsonb,
    saving_throws_all          jsonb,
    skill_bonuses              jsonb,
    skill_bonuses_all          jsonb,
    passive_perception         integer,
    resistances_and_immunities jsonb,
    normal_sight_range         integer,
    darkvision_range           integer,
    blindsight_range           integer,
    tremorsense_range          integer,
    truesight_range            integer,
    actions                    jsonb,
    traits                     jsonb,
    creaturesets               jsonb,
    environments               jsonb,
    illustration               jsonb,
    crossreferences            jsonb,
    document_key               text NOT NULL,
    derived_from               text,
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
    document_key    text NOT NULL,
    derived_from    text,
    CONSTRAINT rulesets_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.rules (
    key                  text NOT NULL,
    name                 text,
    "desc"               text,
    index                integer,
    initial_header_level integer,
    document_key         text NOT NULL,
    ruleset              text,
    crossreferences      jsonb,
    derived_from         text,
    CONSTRAINT rules_pkey PRIMARY KEY (key)
);

CREATE TABLE open5e.images (
    name         text,
    key          text NOT NULL,
    file_url     text,
    alt_text     text,
    attribution  text,
    document     jsonb,
    document_key text NOT NULL,
    derived_from text,
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
    document_key    text NOT NULL,
    derived_from    text,
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
CREATE INDEX creatures_document_key_idx        ON open5e.creatures        USING btree (document_key);
CREATE INDEX creatures_cr_idx                  ON open5e.creatures        USING btree (challenge_rating);
CREATE INDEX creatures_name_lower_idx          ON open5e.creatures        USING btree (lower(name));
CREATE INDEX creaturesets_document_key_idx     ON open5e.creaturesets     USING btree (document_key);
CREATE INDEX creaturetypes_document_key_idx    ON open5e.creaturetypes    USING btree (document_key);
CREATE INDEX document_members_user_id_idx     ON open5e.document_members USING btree (user_id);
CREATE INDEX documents_owner_id_idx           ON open5e.documents        USING btree (owner_id);
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
ALTER TABLE open5e.creatures        ADD CONSTRAINT creatures_document_key_fk        FOREIGN KEY (document_key)      REFERENCES open5e.documents (key)      DEFERRABLE INITIALLY DEFERRED;
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
ALTER TABLE open5e.documents        ADD CONSTRAINT documents_owner_id_fk            FOREIGN KEY (owner_id)          REFERENCES open5e.users (id);

-- Customized copies point at the resource they were copied from. The original can be deleted; the copy stays.

ALTER TABLE open5e.abilities        ADD CONSTRAINT abilities_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.abilities (key) ON DELETE SET NULL;
ALTER TABLE open5e.alignments       ADD CONSTRAINT alignments_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.alignments (key) ON DELETE SET NULL;
ALTER TABLE open5e.armor            ADD CONSTRAINT armor_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.armor (key) ON DELETE SET NULL;
ALTER TABLE open5e.backgrounds      ADD CONSTRAINT backgrounds_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.backgrounds (key) ON DELETE SET NULL;
ALTER TABLE open5e.classes          ADD CONSTRAINT classes_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.classes (key) ON DELETE SET NULL;
ALTER TABLE open5e.conditions       ADD CONSTRAINT conditions_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.conditions (key) ON DELETE SET NULL;
ALTER TABLE open5e.creatures        ADD CONSTRAINT creatures_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.creatures (key) ON DELETE SET NULL;
ALTER TABLE open5e.creaturesets     ADD CONSTRAINT creaturesets_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.creaturesets (key) ON DELETE SET NULL;
ALTER TABLE open5e.creaturetypes    ADD CONSTRAINT creaturetypes_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.creaturetypes (key) ON DELETE SET NULL;
ALTER TABLE open5e.damagetypes      ADD CONSTRAINT damagetypes_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.damagetypes (key) ON DELETE SET NULL;
ALTER TABLE open5e.environments     ADD CONSTRAINT environments_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.environments (key) ON DELETE SET NULL;
ALTER TABLE open5e.feats            ADD CONSTRAINT feats_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.feats (key) ON DELETE SET NULL;
ALTER TABLE open5e.images           ADD CONSTRAINT images_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.images (key) ON DELETE SET NULL;
ALTER TABLE open5e.itemcategories   ADD CONSTRAINT itemcategories_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.itemcategories (key) ON DELETE SET NULL;
ALTER TABLE open5e.items            ADD CONSTRAINT items_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.items (key) ON DELETE SET NULL;
ALTER TABLE open5e.itemsets         ADD CONSTRAINT itemsets_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.itemsets (key) ON DELETE SET NULL;
ALTER TABLE open5e.languages        ADD CONSTRAINT languages_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.languages (key) ON DELETE SET NULL;
ALTER TABLE open5e.magicitems       ADD CONSTRAINT magicitems_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.magicitems (key) ON DELETE SET NULL;
ALTER TABLE open5e.rules            ADD CONSTRAINT rules_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.rules (key) ON DELETE SET NULL;
ALTER TABLE open5e.rulesets         ADD CONSTRAINT rulesets_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.rulesets (key) ON DELETE SET NULL;
ALTER TABLE open5e.services         ADD CONSTRAINT services_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.services (key) ON DELETE SET NULL;
ALTER TABLE open5e.sizes            ADD CONSTRAINT sizes_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.sizes (key) ON DELETE SET NULL;
ALTER TABLE open5e.skills           ADD CONSTRAINT skills_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.skills (key) ON DELETE SET NULL;
ALTER TABLE open5e.species          ADD CONSTRAINT species_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.species (key) ON DELETE SET NULL;
ALTER TABLE open5e.spells           ADD CONSTRAINT spells_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.spells (key) ON DELETE SET NULL;
ALTER TABLE open5e.spellschools     ADD CONSTRAINT spellschools_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.spellschools (key) ON DELETE SET NULL;
ALTER TABLE open5e.weaponproperties ADD CONSTRAINT weaponproperties_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.weaponproperties (key) ON DELETE SET NULL;
ALTER TABLE open5e.weapons          ADD CONSTRAINT weapons_derived_from_fk FOREIGN KEY (derived_from) REFERENCES open5e.weapons (key) ON DELETE SET NULL;
