CREATE TABLE accounts(
    id              UUID PRIMARY KEY,
    owner           TEXT NOT NULL,
    currency        CHAR(3) NOT NULL,
    type            TEXT NOT NULL CHECK(type IN ('CUSTOMER', 'SYSTEM')),
    allow_negative  BOOLEAN NOT NULL DEFAULT FALSE,
    balance         BIGINT NOT NULL DEFAULT 0,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (allow_negative OR balance >= 0)
);

CREATE TABLE transactions(
    id              UUID PRIMARY KEY,
    idempotency_key TEXT NOT NULL UNIQUE,
    request_hash    TEXT NOT NULL,
    type            TEXT NOT NULL CHECK(type IN('DEPOSIT', 'TRANSFER', 'REVERSAL')),
    reverses_id     UUID REFERENCES transactions(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE entries(
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    account_id      UUID NOT NULL REFERENCES accounts(id),
    direction       TEXT NOT NULL CHECK(direction IN('DEBIT', 'CREDIT')),
    amount          BIGINT NOT NULL CHECK (amount > 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_entries_account ON entries(account_id, id);

CREATE TABLE outbox(
    id              UUID PRIMARY KEY,
    event_type      TEXT NOT NULL,
    aggregate_id    UUID NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ
);
CREATE INDEX idx_outbox_unpublished ON outbox(created_at) WHERE published_at IS NULL;

CREATE FUNCTION check_balanced() RETURNS trigger AS $$
DECLARE diff BIGINT;
BEGIN
    SELECT COALESCE(SUM(CASE WHEN direction='CREDIT' THEN amount ELSE -amount END), 0)
        INTO diff FROM entries WHERE transaction_id=NEW.transaction_id;
    IF diff <> 0 THEN
        RAISE EXCEPTION 'Transaction % is unbalanced (diff=%)', NEW.transaction_id, diff;
    END IF;
    RETURN NULL;
END; $$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_entries_balanced
    AFTER INSERT ON entries DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_balanced();

CREATE FUNCTION forbid_mutation() RETURNS trigger as $$
BEGIN RAISE EXCEPTION 'entries are immutable'; END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_entries_immutable
    BEFORE UPDATE OR DELETE ON entries
    FOR EACH ROW EXECUTE FUNCTION forbid_mutation();