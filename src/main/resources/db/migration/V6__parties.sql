-- Parties. A DM adds users to their party; a user is in it only once they accept. The DM can then see (but not change)
-- the player characters those users own or play, to drop them into an initiative order. Until they accept, or after
-- either side ends it, the DM sees nothing of theirs.

CREATE TABLE open5e.party_members (
    dm_id      bigint      NOT NULL,
    user_id    bigint      NOT NULL,
    status     text        NOT NULL DEFAULT 'PENDING',
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT party_members_pkey PRIMARY KEY (dm_id, user_id),
    CONSTRAINT party_members_status_check CHECK (status IN ('PENDING', 'ACCEPTED')),
    CONSTRAINT party_members_not_self_check CHECK (dm_id <> user_id),
    CONSTRAINT party_members_dm_id_fk FOREIGN KEY (dm_id) REFERENCES open5e.users (id) ON DELETE CASCADE,
    CONSTRAINT party_members_user_id_fk FOREIGN KEY (user_id) REFERENCES open5e.users (id) ON DELETE CASCADE
);

CREATE INDEX party_members_user_id_idx ON open5e.party_members (user_id);
