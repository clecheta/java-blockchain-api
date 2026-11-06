CREATE TABLE ledger_wallet (
    uid_hash VARCHAR(64) PRIMARY KEY,
    public_key TEXT NOT NULL UNIQUE,
    private_key TEXT NOT NULL,
    balance NUMERIC NOT NULL,
    starting_balance NUMERIC NOT NULL,
    nonce BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ledger_block (
    hash VARCHAR(128) PRIMARY KEY,
    chain_type VARCHAR(32) NOT NULL,
    block_number BIGINT NOT NULL,
    previous_hash VARCHAR(128) NOT NULL,
    nonce BIGINT NOT NULL,
    mined_at TIMESTAMPTZ NOT NULL,
    data_json TEXT,
    CONSTRAINT uk_ledger_block_chain_number UNIQUE (chain_type, block_number)
);

CREATE INDEX ix_ledger_block_chain_height
    ON ledger_block (chain_type, block_number);
CREATE INDEX ix_ledger_block_mined_at
    ON ledger_block (mined_at DESC);

CREATE TABLE funding_record (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    uid_hash VARCHAR(64) NOT NULL REFERENCES ledger_wallet(uid_hash),
    amount NUMERIC NOT NULL CHECK (amount > 0),
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

CREATE TABLE api_idempotency (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    operation VARCHAR(32) NOT NULL,
    subject VARCHAR(128) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
