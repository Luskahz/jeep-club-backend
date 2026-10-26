-- Read-only BACK-526 preflight for an existing Registry (V4-V9).
-- Run only in an authorized window with writers stopped, before Flyway V10.
-- Blocking counts must all be zero. No rows, identifiers or personal data are returned.
SELECT 'BLOCK_INVALID_CURRENT' AS check_name, COUNT(*) AS invalid_count
FROM membership_members
WHERE REGEXP_LIKE(membership_number,'[^0-9]','c') OR NOT REGEXP_LIKE(membership_number,'[1-9]','c');

SELECT 'BLOCK_INVALID_RESERVATION' AS check_name, COUNT(*) AS invalid_count
FROM membership_number_claims WHERE member_id IS NULL AND
    (REGEXP_LIKE(membership_number,'[^0-9]','c') OR NOT REGEXP_LIKE(membership_number,'[1-9]','c'));

SELECT 'BLOCK_CANONICAL_COLLISION' AS check_name, COUNT(*) AS invalid_count
FROM (
    SELECT canonical_number FROM (
        SELECT TRIM(LEADING '0' FROM membership_number) AS canonical_number FROM membership_members
        UNION ALL
        SELECT TRIM(LEADING '0' FROM membership_number) FROM membership_number_claims WHERE member_id IS NULL
    ) occupied GROUP BY canonical_number HAVING COUNT(*)>1
) collisions;

SELECT 'BLOCK_CURRENT_CLAIM_MISSING' AS check_name, COUNT(*) AS invalid_count
FROM membership_members m LEFT JOIN membership_number_claims c
    ON c.member_id=m.id AND BINARY c.membership_number=BINARY m.membership_number
WHERE c.claim_key IS NULL;

-- Informational only: all these historical claims are snapshotted before release.
SELECT 'INFO_RETIRED_CLAIMS' AS check_name, COUNT(*) AS claim_count
FROM membership_number_claims c LEFT JOIN membership_members m
    ON c.member_id=m.id AND BINARY c.membership_number=BINARY m.membership_number
WHERE c.member_id IS NOT NULL AND m.id IS NULL;
