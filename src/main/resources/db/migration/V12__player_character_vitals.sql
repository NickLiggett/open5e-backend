-- What changes during play: current hit points (null = at the maximum, which the character carries as hit_points),
-- temporary hit points, heroic inspiration, conditions (a JSON array of the official condition names), exhaustion
-- (0 to 6) and death saving throws. `version` counts the saves of the rest of the character, so a screen that was
-- left open can be told its copy is out of date; changes to these live fields never touch it.

ALTER TABLE open5e.player_characters
    ADD COLUMN current_hp integer,
    ADD COLUMN temp_hp integer NOT NULL DEFAULT 0,
    ADD COLUMN heroic_inspiration boolean NOT NULL DEFAULT false,
    ADD COLUMN conditions jsonb NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN exhaustion integer NOT NULL DEFAULT 0,
    ADD COLUMN death_save_successes integer NOT NULL DEFAULT 0,
    ADD COLUMN death_save_failures integer NOT NULL DEFAULT 0,
    ADD COLUMN version integer NOT NULL DEFAULT 0;
