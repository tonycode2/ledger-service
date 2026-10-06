INSERT INTO accounts(id, owner, currency, type, allow_negative)
VALUES(gen_random_uuid(), 'EXTERNAL_FOUNDS', 'USD', 'SYSTEM', TRUE);