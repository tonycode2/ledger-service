ALTER TABLE transactions
  ADD CONSTRAINT uq_transactions_reverses_id UNIQUE (reverses_id);

ALTER TABLE transactions
  ADD CONSTRAINT chk_reversal_consistency
  CHECK ((type = 'REVERSAL') = (reverses_id IS NOT NULL));