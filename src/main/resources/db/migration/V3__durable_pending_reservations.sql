ALTER TABLE ledger_wallet
    ADD COLUMN reserved_balance NUMERIC NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_ledger_wallet_reserved_nonnegative
        CHECK (reserved_balance >= 0);

CREATE TABLE ledger_pending_transaction (
    transaction_id UUID PRIMARY KEY,
    sender_public_key TEXT NOT NULL REFERENCES ledger_wallet(public_key),
    amount NUMERIC NOT NULL CHECK (amount > 0),
    transaction_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_ledger_pending_transaction_created
    ON ledger_pending_transaction (created_at, transaction_id);

CREATE INDEX ix_ledger_pending_transaction_sender
    ON ledger_pending_transaction (sender_public_key);

CREATE INDEX ix_api_idempotency_velocity
    ON api_idempotency (operation, subject, created_at DESC);
