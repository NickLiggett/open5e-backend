-- Multiclassing: a character can have several classes, each with its own level. `classes` is a JSON array of
-- {classKey, className, level}, first class first. When it isn't empty the character's `level` is the sum of these
-- levels, and class_key / class_name are the first class, so anything that only knows one class still reads sensibly.

ALTER TABLE open5e.player_characters ADD COLUMN classes jsonb NOT NULL DEFAULT '[]'::jsonb;

UPDATE open5e.player_characters
SET classes = jsonb_build_array(jsonb_build_object('classKey', class_key, 'className', class_name, 'level', level))
WHERE class_name IS NOT NULL;
