-- Initiative trackers a DM has put aside to come back to (a different campaign, say). The tracker in use stays in
-- user_tracker_states; opening a saved one swaps it in, and the one that was in use is put here. Like the tracker
-- state, what is kept is a JSON object the app shapes, and it is the user's alone. Names are labels, not keys.
-- Deleting a user deletes them.

CREATE TABLE open5e.user_saved_trackers (
    id         bigserial   NOT NULL,
    user_id    bigint      NOT NULL,
    name       text        NOT NULL,
    state      jsonb       NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_saved_trackers_pkey PRIMARY KEY (id),
    CONSTRAINT user_saved_trackers_user_id_fk FOREIGN KEY (user_id) REFERENCES open5e.users (id) ON DELETE CASCADE
);

CREATE INDEX user_saved_trackers_user_id_idx ON open5e.user_saved_trackers (user_id);
