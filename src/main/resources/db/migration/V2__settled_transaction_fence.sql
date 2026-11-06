CREATE TABLE ledger_settled_transaction (
    transaction_id UUID PRIMARY KEY,
    block_hash VARCHAR(128) NOT NULL REFERENCES ledger_block(hash),
    settled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_ledger_settled_transaction_block
    ON ledger_settled_transaction (block_hash);
