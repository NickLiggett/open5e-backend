-- Platinum pieces, beside the gold, silver and copper a character carries.

ALTER TABLE open5e.player_characters ADD COLUMN platinum integer NOT NULL DEFAULT 0;
