-- Things that belong to a user rather than to a document: their look-and-feel settings, their avatar picture, and where
-- they left their initiative tracker. One small table each, so that reading one (the tracker state, say) never loads
-- another (the avatar is bytes). Deleting a user deletes all three.

CREATE TABLE open5e.user_settings (
    user_id    bigint      NOT NULL,
    settings   jsonb       NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_settings_pkey PRIMARY KEY (user_id),
    CONSTRAINT user_settings_user_id_fk FOREIGN KEY (user_id) REFERENCES open5e.users (id) ON DELETE CASCADE
);

CREATE TABLE open5e.user_avatars (
    user_id      bigint      NOT NULL,
    content_type text        NOT NULL,
    image        bytea       NOT NULL,
    updated_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_avatars_pkey PRIMARY KEY (user_id),
    CONSTRAINT user_avatars_user_id_fk FOREIGN KEY (user_id) REFERENCES open5e.users (id) ON DELETE CASCADE
);

CREATE TABLE open5e.user_tracker_states (
    user_id    bigint      NOT NULL,
    state      jsonb       NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT user_tracker_states_pkey PRIMARY KEY (user_id),
    CONSTRAINT user_tracker_states_user_id_fk FOREIGN KEY (user_id) REFERENCES open5e.users (id) ON DELETE CASCADE
);
