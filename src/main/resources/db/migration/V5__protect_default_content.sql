-- Default content (documents with no owner, and everything in them) can't be changed through normal writes.
-- The app already refuses such writes; this is a backstop in case of a bug. An importer that refreshes default
-- content can allow changes for its own transaction with:
--   SET LOCAL open5e.allow_default_content_changes = 'on';

CREATE FUNCTION open5e.default_content_changes_allowed() RETURNS boolean LANGUAGE sql STABLE AS $$
    SELECT coalesce(current_setting('open5e.allow_default_content_changes', true), '') = 'on'
$$;

CREATE FUNCTION open5e.is_default_document(document_key text) RETURNS boolean LANGUAGE sql STABLE AS $$
    SELECT EXISTS (SELECT 1 FROM open5e.documents d WHERE d.key = document_key AND d.owner_id IS NULL)
$$;

-- Resources: no inserting into, changing or deleting from a default document.
CREATE FUNCTION open5e.protect_default_resources() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF open5e.default_content_changes_allowed() THEN
        RETURN coalesce(NEW, OLD);
    END IF;
    IF TG_OP IN ('UPDATE', 'DELETE') AND open5e.is_default_document(OLD.document_key)
       OR TG_OP IN ('INSERT', 'UPDATE') AND open5e.is_default_document(NEW.document_key) THEN
        RAISE EXCEPTION 'Default content in %.% can''t be changed', TG_TABLE_SCHEMA, TG_TABLE_NAME;
    END IF;
    RETURN coalesce(NEW, OLD);
END
$$;

-- Documents: default documents can't be changed or deleted, and no document can become default content.
CREATE FUNCTION open5e.protect_default_documents() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF open5e.default_content_changes_allowed() THEN
        RETURN coalesce(NEW, OLD);
    END IF;
    IF TG_OP IN ('UPDATE', 'DELETE') AND OLD.owner_id IS NULL
       OR TG_OP IN ('INSERT', 'UPDATE') AND NEW.owner_id IS NULL THEN
        RAISE EXCEPTION 'Default documents can''t be changed';
    END IF;
    RETURN coalesce(NEW, OLD);
END
$$;

CREATE TRIGGER protect_default_documents BEFORE INSERT OR UPDATE OR DELETE ON open5e.documents
    FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_documents();

CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.abilities        FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.alignments       FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.armor            FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.backgrounds      FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.classes          FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.conditions       FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.creatures        FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.creaturesets     FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.creaturetypes    FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.damagetypes      FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.environments     FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.feats            FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.images           FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.itemcategories   FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.items            FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.itemsets         FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.languages        FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.magicitems       FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.rules            FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.rulesets         FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.services         FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.sizes            FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.skills           FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.species          FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.spells           FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.spellschools     FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.weaponproperties FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
CREATE TRIGGER protect_default_content BEFORE INSERT OR UPDATE OR DELETE ON open5e.weapons          FOR EACH ROW EXECUTE FUNCTION open5e.protect_default_resources();
