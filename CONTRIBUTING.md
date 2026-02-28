# Contributing to AkademiaPlus

## Before You Start

1. Read [AI-CODE-REF.md](AI-CODE-REF.md) — coding standards, review rules, detection patterns
2. Read [DESIGN.md](DESIGN.md) — architecture, module catalog, multi-tenancy model
3. Understand the module you're changing (check `.claude/CLAUDE.md` for the dependency graph)

## Development Setup

### Prerequisites

- Java 24 (JDK)
- Maven 3.9+
- Docker & Docker Compose
- MariaDB (or use Docker Compose)
- IntelliJ IDEA (recommended) with Lombok plugin enabled

### First Build

```bash
git clone git@github.com:ElatusDev/platform-core-api.git
cd platform-core-api
mvn clean install -DskipTests   # verify the build works
mvn test                         # run full test suite
```

## Git Workflow

### Branch Naming

```
feat/{module}/{short-description}    # new feature
fix/{module}/{short-description}     # bug fix
docs/{short-description}             # documentation only
refactor/{module}/{short-description} # code improvement, no behavior change
test/{module}/{short-description}    # adding or fixing tests
```

Examples: `feat/billing/membership-renewal`, `fix/security/jwt-refresh-race`, `docs/adr-multitenancy`

### Commit Convention

This project uses **Conventional Commits** strictly. Every commit message must follow:

```
type(scope): subject line in imperative mood (≤72 chars)

Optional body explaining WHY the change was made and WHAT it affects.
Wrap at 72 characters. Separate from subject with a blank line.
```

**Types**: `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci`, `perf`, `chore`

**Scopes** (match the module or concern):
`build`, `security`, `api`, `test`, `docs`, `user`, `billing`, `course`, `tenant`,
`notification`, `pos`, `ca`, `mock-data`, `etl`, `audit`, `infra`, `utilities`, `multi-tenant`

**Examples**:
```bash
feat(billing): add membership renewal endpoint
fix(security): prevent JWT refresh race condition under concurrent requests
refactor(utilities): extract hashing config to dedicated properties class
test(user): add edge cases for email normalization validation
docs(adr): record decision on composite key strategy for multi-tenancy
```

## Coding Standards (Summary)

The full standards are in [AI-CODE-REF.md](AI-CODE-REF.md). Here are the non-negotiable rules:

### Code

- Extract ALL string literals to `public static final` constants
- Methods must be < 20 lines with cyclomatic complexity < 10
- Catch specific exception types — never `Exception` or `Throwable`
- All IDs use `Long`, never `Integer`
- Javadoc required on all public classes, methods, and constants
- Include the ElatusDev copyright header on every new file

### Testing

- **Pattern**: Given-When-Then (never Arrange-Act-Assert)
- **Naming**: `shouldDoX_whenY()` with `@DisplayName("Should do X when Y")`
- **Organization**: `@Nested` classes with `@DisplayName`
- **Matchers**: ZERO `any()` — use exact values or `ArgumentCaptor`
- **Constants**: Reference implementation constants directly (`MyService.ERROR_MSG`)
- **Stubbing**: Stub with the EXACT parameters the implementation passes (read the impl first)

### Architecture

Each domain module follows Clean Architecture internally:
```
module/{aggregate}/
├── interfaceadapters/     # Controllers + Repositories
└── usecases/              # Business logic services
```

One use case class per operation. Controllers are thin delegates. Business logic never
leaks into controllers or repositories.

### Multi-Tenancy Awareness

- Every query must respect tenant isolation
- Never create a `TenantScoped` entity without composite key (`tenantId` + `entityId`)
- Always verify Hibernate's `tenantFilter` is active in new repository methods
- Entities never set their own IDs — `EntityIdAssigner` handles it

## Pull Request Checklist

Before submitting a PR, verify:

- [ ] Code compiles: `mvn clean install -DskipTests`
- [ ] Tests pass: `mvn test -pl {module}`
- [ ] New public APIs have Javadoc with `@param`, `@return`, `@throws`
- [ ] All string literals extracted to constants
- [ ] Tests follow Given-When-Then with proper naming
- [ ] No `any()` matchers anywhere in test code
- [ ] Copyright header present on all new files
- [ ] Commit messages follow Conventional Commits
- [ ] CHANGELOG.md updated under `[Unreleased]` section
- [ ] No tenant isolation bypass (if touching data layer)

## Updating the Changelog

When your PR is ready, add an entry under `[Unreleased]` in [CHANGELOG.md](CHANGELOG.md):

```markdown
### Added / Changed / Fixed / Removed
- **scope**: Brief description of the change (`commit-hash` or PR number)
```

Group entries by type (Added, Changed, Fixed, Removed) and prefix with the scope.

## Architecture Decision Records

For significant technical decisions, add an ADR in `docs/adr/`. Use the template at
`docs/adr/0000-adr-template.md` and follow the numbering sequence. See existing ADRs
for examples.

## Questions?

If something in the codebase is unclear, check these docs in order:
1. [AI-CODE-REF.md](AI-CODE-REF.md) — coding rules and patterns
2. [DESIGN.md](DESIGN.md) — architecture and system design
3. [.claude/CLAUDE.md](.claude/CLAUDE.md) — extended project memory and key decisions
