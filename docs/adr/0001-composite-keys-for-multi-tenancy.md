# ADR-0001: Composite Keys for Multi-Tenancy

**Status**: Accepted
**Date**: 2025-01-01
**Deciders**: ElatusDev

## Context

AkademiaPlus is a multi-tenant SaaS platform where each educational institution's data must be
completely isolated. We needed a strategy that guarantees tenant isolation at the data model level,
not just at the application layer, so that even a bug in query logic cannot leak data across tenants.

## Decision

Use composite primary keys (`tenantId` + `entityId`) on all tenant-scoped entities, enforced by:

- `@IdClass` / `@EmbeddedId` at the JPA level
- Hibernate filter `tenantFilter` for automatic row-level isolation on all queries
- `TenantContextHolder` (thread-local) to inject the current tenant
- `EntityIdAssigner` via Hibernate `PreInsertEvent` listener for ID assignment
- `SequentialIDGenerator` producing per-tenant sequential IDs

Entity inheritance chain: `Auditable → SoftDeletable → TenantScoped → domain entities`

## Alternatives Considered

1. **Schema-per-tenant** — Each tenant gets a separate database schema. Rejected because it
   complicates connection pooling, migrations, and cross-tenant admin queries. Doesn't scale
   well past ~100 tenants.

2. **Discriminator column (single ID)** — Standard auto-increment PK with a `tenant_id` column
   and application-level filtering. Rejected because a missed WHERE clause leaks data. The
   composite key makes it structurally impossible to reference a row without knowing the tenant.

3. **Database-per-tenant** — Full isolation but extreme operational overhead for provisioning,
   migrations, backups, and monitoring. Rejected for cost and complexity at our scale.

## Consequences

### Positive
- Tenant isolation is enforced at the database schema level — cannot be bypassed by query bugs
- Sequential IDs per tenant (no global sequence contention)
- Hibernate filter ensures isolation even in custom queries

### Negative
- Composite keys add complexity to JPA mappings and relationship annotations
- All repository methods must account for the composite key type
- Join queries are slightly more verbose

### Neutral
- All new entities must extend `TenantScoped` and use `@IdClass` — this is a hard rule
