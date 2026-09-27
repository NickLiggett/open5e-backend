-- Links between resources in the Open5e data are URLs on the Open5e server the data came from (the dump has
-- http://localhost:8000/v2/..., the public API https://api.open5e.com/v2/...). This API has the same endpoints, so
-- rewrite them to paths here: "http://any-host/v2/spells/srd_fireball/" becomes "/api/spells/srd_fireball".
-- Only whole JSON string values are rewritten, never links inside text. The importer does the same to new data
-- (ApiUrls).

SET LOCAL open5e.allow_default_content_changes = 'on';

DO $$
DECLARE
    col record;
BEGIN
    FOR col IN
        SELECT table_name, column_name FROM information_schema.columns
        WHERE table_schema = 'open5e' AND data_type = 'jsonb'
    LOOP
        EXECUTE format(
            $sql$UPDATE open5e.%1$I
                 SET %2$I = regexp_replace(%2$I::text,
                                           '"https?://[^/"]+/v2/([a-z]+)/([^/?#"]+)/?(\?[^"]*)?"',
                                           '"/api/\1/\2"', 'g')::jsonb
                 WHERE %2$I::text ~ '"https?://[^/"]+/v2/'$sql$,
            col.table_name, col.column_name);
    END LOOP;
END
$$;
