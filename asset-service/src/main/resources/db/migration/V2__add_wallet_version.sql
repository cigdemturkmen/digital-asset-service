-- Optimistic locking: Hibernate increments this on every wallet update and adds "AND version = ?" to the
-- UPDATE, so a concurrent change makes the second writer fail instead of silently overwriting the first.
ALTER TABLE wallets ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
