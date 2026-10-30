-- What a character carries: coins on the character, and a list of items. An item line points at an Item or Magic item
-- by key when it was picked from the lists, but keeps its own name so it still reads right if that item is deleted or
-- was typed by hand (then item_key and kind are empty). Deleting the character deletes its lines.

ALTER TABLE open5e.player_characters
    ADD COLUMN gold   integer NOT NULL DEFAULT 0,
    ADD COLUMN silver integer NOT NULL DEFAULT 0,
    ADD COLUMN copper integer NOT NULL DEFAULT 0;

CREATE TABLE open5e.player_character_items (
    id           bigserial NOT NULL,
    character_id bigint    NOT NULL,
    position     integer   NOT NULL,
    item_key     text,
    kind         text,
    name         text      NOT NULL,
    quantity     integer   NOT NULL DEFAULT 1,
    equipped     boolean   NOT NULL DEFAULT false,
    attuned      boolean   NOT NULL DEFAULT false,
    notes        text,
    CONSTRAINT player_character_items_pkey PRIMARY KEY (id),
    CONSTRAINT player_character_items_character_fk FOREIGN KEY (character_id)
        REFERENCES open5e.player_characters (id) ON DELETE CASCADE,
    CONSTRAINT player_character_items_kind_check CHECK (kind IS NULL OR kind IN ('ITEM', 'MAGIC')),
    CONSTRAINT player_character_items_quantity_check CHECK (quantity BETWEEN 1 AND 9999)
);

CREATE INDEX player_character_items_character_idx ON open5e.player_character_items (character_id, position);
