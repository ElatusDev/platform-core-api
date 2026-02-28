# ADR-0003: AES-256-GCM Field Encryption with Hash Columns

**Status**: Accepted
**Date**: 2025-01-01
**Deciders**: ElatusDev

## Context

The platform stores PII (emails, phone numbers, names) for students and staff across multiple
tenants. Regulatory requirements and defense-in-depth principles demand that PII be protected
at rest, but we also need to search by email or phone without decrypting every row.

## Decision

Implement a dual-column strategy for PII fields:

- **Encrypted column**: AES-256-GCM with random IV per operation (via `AESGCMEncryptionService`)
- **Hash column**: SHA-256 salted hash for indexed lookups (via `HashingService`)
- **PII normalization**: Canonicalize inputs before encryption/hashing (`PiiNormalizer` with
  ReDoS-safe regex for emails, libphonenumber for phones)
- **Constant-time comparison**: All hash comparisons use timing-attack-resistant algorithms

Flow: `raw PII → PiiNormalizer → encrypt(normalized) + hash(normalized) → store both columns`

Search: `normalize(query) → hash(normalized) → WHERE hash_column = ?`

## Alternatives Considered

1. **Transparent Data Encryption (TDE)** — Database-level encryption. Rejected because it
   protects against disk theft but not against SQL injection or application-layer breaches.
   The data is plaintext in memory and in query results.

2. **Application-level encryption only (no hash)** — Encrypt everything, decrypt to search.
   Rejected because searching requires decrypting all rows — O(n) per query, unacceptable
   at scale.

3. **Deterministic encryption for search** — Same plaintext always produces same ciphertext.
   Rejected because it leaks frequency patterns (identical emails produce identical ciphertext),
   enabling statistical attacks.

## Consequences

### Positive
- PII is encrypted at rest with authenticated encryption (GCM provides integrity)
- Indexed search works without decryption via hash columns
- Random IV per operation means identical plaintexts produce different ciphertexts
- Normalization prevents duplicate entries due to case/format differences
- Constant-time hash comparison prevents timing attacks

### Negative
- Two columns per PII field (storage overhead)
- Hash columns enable existence checks but not range queries or partial matching
- Key management complexity (AES key rotation requires re-encryption)

### Neutral
- JPA `@Convert` (AttributeConverter) handles encryption/decryption transparently
- Hash columns require a custom Hibernate event or service-layer call on write
