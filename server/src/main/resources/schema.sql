CREATE TABLE IF NOT EXISTS appointments (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    customer_name VARCHAR(200) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    duration_minutes SMALLINT NOT NULL CHECK (duration_minutes IN (15, 30, 45, 60)),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Upgrade the original ownerless test schema. The user approved deleting these test rows.
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS owner_id UUID REFERENCES auth.users(id) ON DELETE CASCADE;
DELETE FROM appointments WHERE owner_id IS NULL;
ALTER TABLE appointments ALTER COLUMN owner_id SET NOT NULL;

ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE appointments FROM anon, authenticated;

CREATE INDEX IF NOT EXISTS appointments_starts_at_idx ON appointments (starts_at);
CREATE INDEX IF NOT EXISTS appointments_owner_id_idx ON appointments (owner_id);
CREATE INDEX IF NOT EXISTS appointments_customer_name_idx
    ON appointments ((LOWER(customer_name)));
