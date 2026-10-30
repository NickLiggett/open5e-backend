-- The rest of a character sheet: abilities, skills, speed, senses, defenses, proficiencies, personal details, the text
-- boxes and the lists of attacks, features and spells. The app shapes it and works the totals out; the backend keeps it
-- as a JSON object of limited size, so a new sheet field needs no migration. Empty until someone fills it in.

ALTER TABLE open5e.player_characters ADD COLUMN sheet jsonb NOT NULL DEFAULT '{}'::jsonb;
