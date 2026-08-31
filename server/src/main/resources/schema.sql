CREATE TABLE IF NOT EXISTS appointments (
    id UUID PRIMARY KEY,
    customer_name VARCHAR(200) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    duration_minutes SMALLINT NOT NULL CHECK (duration_minutes IN (15, 30, 45, 60)),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS appointments_starts_at_idx ON appointments (starts_at);
CREATE INDEX IF NOT EXISTS appointments_customer_name_idx
    ON appointments ((LOWER(customer_name)));
