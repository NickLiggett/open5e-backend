-- The encounters a user has saved on the Encounters page, to load again at the table: which creatures and how many, and
-- who the party is. Like the tracker state, the app shapes what is kept (a JSON object), and it is the user's alone.
-- Deleting a user deletes it.

CREATE TABLE open5e.user_encounter_states (
    user_id    bigint      NOT NULL,
    state      jsonb       NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_encounter_states_pkey PRIMARY KEY (user_id),
    CONSTRAINT user_encounter_states_user_id_fk FOREIGN KEY (user_id) REFERENCES open5e.users (id) ON DELETE CASCADE
);
