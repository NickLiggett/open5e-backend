-- The creature JSON columns were character varying; every other table uses jsonb. Converting them lets Postgres
-- reject malformed JSON and lets the entity map them straight onto records. Every existing value is valid JSON.

ALTER TABLE open5e.creatures
    ALTER COLUMN document                   TYPE jsonb USING document::jsonb,
    ALTER COLUMN type                       TYPE jsonb USING type::jsonb,
    ALTER COLUMN size                       TYPE jsonb USING size::jsonb,
    ALTER COLUMN speed                      TYPE jsonb USING speed::jsonb,
    ALTER COLUMN speed_all                  TYPE jsonb USING speed_all::jsonb,
    ALTER COLUMN languages                  TYPE jsonb USING languages::jsonb,
    ALTER COLUMN ability_scores             TYPE jsonb USING ability_scores::jsonb,
    ALTER COLUMN modifiers                  TYPE jsonb USING modifiers::jsonb,
    ALTER COLUMN saving_throws              TYPE jsonb USING saving_throws::jsonb,
    ALTER COLUMN saving_throws_all          TYPE jsonb USING saving_throws_all::jsonb,
    ALTER COLUMN skill_bonuses              TYPE jsonb USING skill_bonuses::jsonb,
    ALTER COLUMN skill_bonuses_all          TYPE jsonb USING skill_bonuses_all::jsonb,
    ALTER COLUMN resistances_and_immunities TYPE jsonb USING resistances_and_immunities::jsonb,
    ALTER COLUMN actions                    TYPE jsonb USING actions::jsonb,
    ALTER COLUMN traits                     TYPE jsonb USING traits::jsonb,
    ALTER COLUMN creaturesets               TYPE jsonb USING creaturesets::jsonb,
    ALTER COLUMN environments               TYPE jsonb USING environments::jsonb,
    ALTER COLUMN illustration               TYPE jsonb USING illustration::jsonb,
    ALTER COLUMN crossreferences            TYPE jsonb USING crossreferences::jsonb;
