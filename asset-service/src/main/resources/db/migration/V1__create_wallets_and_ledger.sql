CREATE TABLE wallets (
    id          UUID          PRIMARY KEY,
    owner_name  VARCHAR(200)  NOT NULL,
    balance     NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL,
    -- Last line of defence: even a bug in the application can't produce a negative balance.
    CONSTRAINT ck_wallets_balance_non_negative CHECK (balance >= 0)
);

-- Append-only record of every balance change. Rows are never updated or deleted.
CREATE TABLE ledger_entries (
    id             UUID          PRIMARY KEY,
    wallet_id      UUID          NOT NULL REFERENCES wallets (id),
    type           VARCHAR(20)   NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    balance_after  NUMERIC(19,4) NOT NULL,
    -- Links the two legs (TRANSFER_OUT / TRANSFER_IN) of the same transfer; NULL for mint/burn.
    transfer_id    UUID,
    created_at     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_ledger_entries_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_ledger_entries_type CHECK (type IN ('MINT', 'BURN', 'TRANSFER_IN', 'TRANSFER_OUT'))
);

CREATE INDEX ix_ledger_entries_wallet_id_created_at ON ledger_entries (wallet_id, created_at DESC);
