-- Ownership: default content has no owner and is read-only; each user's own content lives in documents they own,
-- and can be shared with other users through document_members. See docs/PLAN.md.

-- Users. username identifies dev-mode users (X-User header); idp_subject is for the identity provider later.
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

-- Document owners. Null means default content, visible to everyone.
ALTER TABLE open5e.documents ADD COLUMN owner_id bigint;
ALTER TABLE open5e.documents ADD CONSTRAINT documents_owner_id_fk FOREIGN KEY (owner_id) REFERENCES open5e.users (id);
CREATE INDEX documents_owner_id_idx ON open5e.documents USING btree (owner_id);

-- Sharing: members can see a document's content (VIEWER) or also edit it (EDITOR).
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
CREATE INDEX document_members_user_id_idx ON open5e.document_members USING btree (user_id);

-- Key columns computed from embedded JSON become plain columns, so the app can set them on user-created rows.
-- Existing values are kept.
ALTER TABLE open5e.alignments       ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.armor            ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.backgrounds      ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.classes          ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.classes          ALTER COLUMN subclass_of_key DROP EXPRESSION;
ALTER TABLE open5e.conditions       ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.feats            ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.images           ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.itemcategories   ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.items            ALTER COLUMN category_key DROP EXPRESSION;
ALTER TABLE open5e.items            ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.languages        ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.magicitems       ALTER COLUMN category_key DROP EXPRESSION;
ALTER TABLE open5e.magicitems       ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.magicitems       ALTER COLUMN rarity_key DROP EXPRESSION;
ALTER TABLE open5e.rulesets         ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.services         ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.sizes            ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.species          ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.spells           ALTER COLUMN document_key DROP EXPRESSION;
ALTER TABLE open5e.weapons          ALTER COLUMN document_key DROP EXPRESSION;

-- Creatures only had the embedded document JSON; give them a document_key like every other resource.
ALTER TABLE open5e.creatures ADD COLUMN document_key text;
UPDATE open5e.creatures SET document_key = document ->> 'key';
ALTER TABLE open5e.creatures ADD CONSTRAINT creatures_document_key_fk FOREIGN KEY (document_key) REFERENCES open5e.documents (key) DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX creatures_document_key_idx ON open5e.creatures USING btree (document_key);

-- Every resource belongs to a document; visibility is decided through it.
ALTER TABLE open5e.abilities        ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.alignments       ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.armor            ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.backgrounds      ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.classes          ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.conditions       ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.creatures        ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.creaturesets     ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.creaturetypes    ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.damagetypes      ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.environments     ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.feats            ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.images           ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.itemcategories   ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.items            ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.itemsets         ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.languages        ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.magicitems       ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.rules            ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.rulesets         ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.services         ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.sizes            ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.skills           ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.species          ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.spells           ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.spellschools     ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.weaponproperties ALTER COLUMN document_key SET NOT NULL;
ALTER TABLE open5e.weapons          ALTER COLUMN document_key SET NOT NULL;

-- Customized copies point at the resource they were copied from. The original can be deleted; the copy stays.
ALTER TABLE open5e.abilities        ADD COLUMN derived_from text CONSTRAINT abilities_derived_from_fk REFERENCES open5e.abilities (key) ON DELETE SET NULL;
ALTER TABLE open5e.alignments       ADD COLUMN derived_from text CONSTRAINT alignments_derived_from_fk REFERENCES open5e.alignments (key) ON DELETE SET NULL;
ALTER TABLE open5e.armor            ADD COLUMN derived_from text CONSTRAINT armor_derived_from_fk REFERENCES open5e.armor (key) ON DELETE SET NULL;
ALTER TABLE open5e.backgrounds      ADD COLUMN derived_from text CONSTRAINT backgrounds_derived_from_fk REFERENCES open5e.backgrounds (key) ON DELETE SET NULL;
ALTER TABLE open5e.classes          ADD COLUMN derived_from text CONSTRAINT classes_derived_from_fk REFERENCES open5e.classes (key) ON DELETE SET NULL;
ALTER TABLE open5e.conditions       ADD COLUMN derived_from text CONSTRAINT conditions_derived_from_fk REFERENCES open5e.conditions (key) ON DELETE SET NULL;
ALTER TABLE open5e.creatures        ADD COLUMN derived_from text CONSTRAINT creatures_derived_from_fk REFERENCES open5e.creatures (key) ON DELETE SET NULL;
ALTER TABLE open5e.creaturesets     ADD COLUMN derived_from text CONSTRAINT creaturesets_derived_from_fk REFERENCES open5e.creaturesets (key) ON DELETE SET NULL;
ALTER TABLE open5e.creaturetypes    ADD COLUMN derived_from text CONSTRAINT creaturetypes_derived_from_fk REFERENCES open5e.creaturetypes (key) ON DELETE SET NULL;
ALTER TABLE open5e.damagetypes      ADD COLUMN derived_from text CONSTRAINT damagetypes_derived_from_fk REFERENCES open5e.damagetypes (key) ON DELETE SET NULL;
ALTER TABLE open5e.environments     ADD COLUMN derived_from text CONSTRAINT environments_derived_from_fk REFERENCES open5e.environments (key) ON DELETE SET NULL;
ALTER TABLE open5e.feats            ADD COLUMN derived_from text CONSTRAINT feats_derived_from_fk REFERENCES open5e.feats (key) ON DELETE SET NULL;
ALTER TABLE open5e.images           ADD COLUMN derived_from text CONSTRAINT images_derived_from_fk REFERENCES open5e.images (key) ON DELETE SET NULL;
ALTER TABLE open5e.itemcategories   ADD COLUMN derived_from text CONSTRAINT itemcategories_derived_from_fk REFERENCES open5e.itemcategories (key) ON DELETE SET NULL;
ALTER TABLE open5e.items            ADD COLUMN derived_from text CONSTRAINT items_derived_from_fk REFERENCES open5e.items (key) ON DELETE SET NULL;
ALTER TABLE open5e.itemsets         ADD COLUMN derived_from text CONSTRAINT itemsets_derived_from_fk REFERENCES open5e.itemsets (key) ON DELETE SET NULL;
ALTER TABLE open5e.languages        ADD COLUMN derived_from text CONSTRAINT languages_derived_from_fk REFERENCES open5e.languages (key) ON DELETE SET NULL;
ALTER TABLE open5e.magicitems       ADD COLUMN derived_from text CONSTRAINT magicitems_derived_from_fk REFERENCES open5e.magicitems (key) ON DELETE SET NULL;
ALTER TABLE open5e.rules            ADD COLUMN derived_from text CONSTRAINT rules_derived_from_fk REFERENCES open5e.rules (key) ON DELETE SET NULL;
ALTER TABLE open5e.rulesets         ADD COLUMN derived_from text CONSTRAINT rulesets_derived_from_fk REFERENCES open5e.rulesets (key) ON DELETE SET NULL;
ALTER TABLE open5e.services         ADD COLUMN derived_from text CONSTRAINT services_derived_from_fk REFERENCES open5e.services (key) ON DELETE SET NULL;
ALTER TABLE open5e.sizes            ADD COLUMN derived_from text CONSTRAINT sizes_derived_from_fk REFERENCES open5e.sizes (key) ON DELETE SET NULL;
ALTER TABLE open5e.skills           ADD COLUMN derived_from text CONSTRAINT skills_derived_from_fk REFERENCES open5e.skills (key) ON DELETE SET NULL;
ALTER TABLE open5e.species          ADD COLUMN derived_from text CONSTRAINT species_derived_from_fk REFERENCES open5e.species (key) ON DELETE SET NULL;
ALTER TABLE open5e.spells           ADD COLUMN derived_from text CONSTRAINT spells_derived_from_fk REFERENCES open5e.spells (key) ON DELETE SET NULL;
ALTER TABLE open5e.spellschools     ADD COLUMN derived_from text CONSTRAINT spellschools_derived_from_fk REFERENCES open5e.spellschools (key) ON DELETE SET NULL;
ALTER TABLE open5e.weaponproperties ADD COLUMN derived_from text CONSTRAINT weaponproperties_derived_from_fk REFERENCES open5e.weaponproperties (key) ON DELETE SET NULL;
ALTER TABLE open5e.weapons          ADD COLUMN derived_from text CONSTRAINT weapons_derived_from_fk REFERENCES open5e.weapons (key) ON DELETE SET NULL;
