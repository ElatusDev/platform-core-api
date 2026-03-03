# Agent Strategy — AkademiaPlus Platform Core API

**Version**: 1.1
**Date**: February 27, 2026
**Scope**: Claude Code agent operations for `platform-core-api`
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`

---

## 1. Strategic Overview

This document defines the execution model, prompt architecture, and orchestration strategy for AI-assisted development of the AkademiaPlus platform. It governs how Claude Code interacts with the codebase across 12 beta-maturity waves, ensuring every agent session produces deterministic, verifiable, and convention-compliant output.

### 1.1 Current State (updated 2026-02-27)

| Dimension | Status |
|-----------|--------|
| Prompts written | 5 of 10 (Wave 0, 1, 3, 6, auth-bootstrap) |
| Prompts needed | 1 (Wave 7 — Flyway) |
| Waves complete | 8 (Waves 0, 1, 2, 3, 4, 5, 8, 10) |
| Waves in progress | 2 (Wave 9 — Security ~4h, Wave 11 — API Docs ~4h) |
| Waves pending | 2 (Wave 6 — E2E ~6h, Wave 7 — Flyway ~8h) |
| Estimated remaining effort | ~20h across remaining waves |
| Critical path | Wave 6 (E2E, needs Docker) → 7 (Flyway, needs Docker) → Beta |
| Build | Java 24, Spring Boot 4.0.3, Hibernate 7.2.5, Jackson 3, 15 modules |

### 1.2 Agent Roles

The strategy defines three distinct agent modes:

**Architect Mode** — Planning, specification, ADR authoring. Reads the codebase, analyzes patterns, produces documentation. No source code modification.

**Executor Mode** — Phase-by-phase implementation following a `docs/prompts/*.md` execution prompt. Strict sequential discipline: read → implement → compile → test → commit.

**Auditor Mode** — Post-execution verification. Runs greps, dependency trees, test suites, and validates exit criteria. Reports violations without fixing them.

---

## 2. Prompt Architecture

### 2.1 Anatomy of an Execution Prompt

Every `docs/prompts/*.md` follows this invariant structure:

```
# Title — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: absolute path
**Prerequisite**: docs to read before starting
**Dependency**: prior prompts that MUST be complete

---

## EXECUTION RULES (immutable per-session)

1. Strictly sequential phases
2. Compile/test gate between phases
3. Copyright header on all new files
4. Javadoc on all public APIs
5. Test conventions: Given-When-Then, shouldDoX_whenY, zero any()
6. Commit after each phase with Conventional Commits

---

## Phase N: {Name}
### Step N.M: {Action}
{Exact file paths, code snippets, grep commands}
### Verify Phase N
{mvn compile/test command}
### Commit Phase N
{exact git commit message}
```

### 2.2 Prompt Design Principles

**Determinism**: Every prompt produces the same output regardless of which Claude Code session executes it. No decisions deferred to the agent — all file paths, class names, method signatures, and commit messages are pre-specified.

**Grep-before-write**: Before modifying any file, the prompt includes a `grep` or `find` command to discover the current state. This prevents the agent from assuming file contents that may have changed since the prompt was written.

**Compile gates**: No phase advances until `mvn compile` succeeds. This catches import errors, missing dependencies, and type mismatches immediately rather than accumulating them.

**Atomic commits**: One Conventional Commit per phase. If a phase fails mid-execution, the uncommitted work is the only thing that needs to be reverted.

### 2.3 Prompt Template (for writing remaining 6 prompts)

```markdown
# {Wave Name} — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api`
**Prerequisite**: Read `CLAUDE.md`, `AI-CODE-REF.md` before starting.
**Dependency**: {list of prior prompts that must be complete}

---

## EXECUTION RULES

1. Execute phases **strictly in order** (Phase 1 → 2 → ...).
2. Do NOT skip ahead. Each phase must compile before the next begins.
3. After EACH phase, run the specified verification command.
4. All new files MUST include the ElatusDev copyright header.
5. All `public` classes and methods MUST have Javadoc.
6. {Wave-specific rules}
```

---

## 3. Execution Orchestration

### 3.1 Wave Dependency DAG

```
Wave 0 (Dependency Upgrade) ✅  Boot 4.0.3, Java 24, Jackson 3
    │
    ▼
Wave 1 (Exception Advice) ✅
    │
    ├────────────────────┐
    ▼                    ▼
Wave 2 (Creation) ✅   Wave 3 (Delete) ✅
    │                    │
    └────────┬───────────┘
             ▼
Wave 4 (Component Tests) ✅
             │
             ▼
Wave 5 (Docker/CI) ✅  →  Wave 6 (E2E Tests) ⬜  →  Wave 7 (Flyway) ⬜
             │
             ▼
Wave 8 (Observability) ✅  →  Wave 9 (Security) 🔄  →  Wave 11 (API Docs) 🔄
             │
             ▼
Wave 10 (Placeholder) ✅   etl removed, audit stubbed
             │
             ▼
        [ BETA RELEASE ]
```

### 3.2 Session Planning Matrix

Each Claude Code session should target exactly one wave (or one phase within a large wave).

| Session Duration | Target Scope | Strategy |
|-----------------|--------------|----------|
| < 30 min | Single phase within a wave | Run prompt Phase N only |
| 30–90 min | Full wave (< 6 phases) | Run entire prompt |
| 90+ min | Large wave (10+ phases) | Split into 2–3 sessions at natural phase boundaries |

**Natural split points for large waves**:
- **Wave 1** (10 phases): Split at Phase 6 (after tests) and Phase 8 (after migration)
- **Wave 3** (7 phases): Split at Phase 1 (after infrastructure) and Phase 5 (after last module)
- **Wave 4** (7 phases): Split per module group — tenant/user, course/billing, notification/pos

### 3.3 Pre-Session Checklist

Before every Claude Code session:

```bash
# 1. Verify clean working tree
git -C /Volumes/ElatusDev/ElatusDev/AkademiaPlus/platform-core-api status

# 2. Verify build is green
mvn clean compile -DskipTests

# 3. Verify dependencies of current wave are met
# (check BETA-MATURITY-PLAN.md entry criteria)

# 4. Load context docs
cat docs/directives/CLAUDE.md docs/directives/AI-CODE-REF.md docs/design/DESIGN.md
```

### 3.4 Post-Session Verification Protocol

After every Claude Code session:

```bash
# 1. Full compile
mvn clean compile -DskipTests

# 2. Full test suite (or module-scoped)
mvn test -pl {modified-modules} -am

# 3. Convention compliance
grep -rn "any()" --include="*.java" */src/test/  # Must return 0 hits from new code
grep -rn "catch (Exception " --include="*.java" */src/main/  # Audit
grep -rn "// Arrange\|// Act\|// Assert" --include="*.java" */src/test/  # Must be 0

# 4. Copyright header check on new files
git diff --name-only --diff-filter=A | xargs head -3  # Verify header

# 5. Commit message audit
git log --oneline -5  # Verify Conventional Commits format
```

---

## 4. Prompt Authoring Plan (6 Remaining Prompts)

### 4.1 Priority Order

| Priority | Prompt | Depends On | Est. Effort to Write |
|----------|--------|-----------|---------------------|
| ~~P0~~ | ~~`delete-usecase-rollout.md`~~ | ~~Wave 1 complete~~ | ✅ Done (Wave 3 executed) |
| ~~P1~~ | ~~`component-test-rollout.md`~~ | ~~Waves 1, 2, 3 complete~~ | ✅ Done (Wave 4 executed) |
| ~~P2~~ | ~~`ci-fixes.md`~~ | ~~Wave 0 complete~~ | ✅ Not needed (Wave 5 executed directly) |
| **P3** | `flyway-baseline.md` | Wave 4 complete ✅ | ~2h |
| ~~P4~~ | ~~`observability-baseline.md`~~ | ~~Wave 5 complete~~ | ✅ Not needed (Wave 8 executed directly) |
| ~~P5~~ | ~~`security-hardening.md`~~ | ~~Wave 8 complete~~ | ✅ Not needed (Wave 9 partially executed directly) |

### 4.2 Prompt Spec: `component-test-rollout.md`

**Phases**: 8

**Phase 0 — Infrastructure Setup** (billing, notification-system, pos-system):
- Create test dependencies, `{Module}TestApp.java`, `AbstractIntegrationTest.java`
- Testcontainers MariaDB, Failsafe plugin, keystore

**Phases 1–6 — Per-Module Entity Tests**:
- Each entity gets `{Entity}ComponentTest.java`
- Test matrix: Create 201, Create conflict 409, GetById 200/404, GetAll 200, Delete 204/404/409
- Soft-delete verification via native SQL
- Tenant isolation: create under tenant A, verify invisible from tenant B

**Phase 7 — Refactor Existing Tests**: Merge old aggregate tests into per-entity files

### 4.3 Prompt Spec: `ci-fixes.md`

**Phases**: 3  
**Phase 0**: Audit Dockerfiles, docker-compose, GitHub Actions for version mismatches  
**Phase 1**: Java 25 alignment (Dockerfiles + CI workflows)  
**Phase 2**: docker-compose.qa.yml cleanup + SonarQube project key fix

### 4.4 Prompt Spec: `flyway-baseline.md`

**Phases**: 6  
**Phase 0**: Add `spring-boot-starter-flyway`  
**Phase 1**: Generate `V1__baseline.sql` from MariaDB  
**Phase 2**: Properties: `ddl-auto=none` + Flyway config  
**Phase 3**: Testcontainers → Flyway migration path  
**Phase 4**: `mvn verify`  
**Phase 5**: ADR-0008

### 4.5 Prompt Spec: `observability-baseline.md`

**Phases**: 5  
**Phase 0**: Actuator endpoints (health, liveness, readiness, metrics)  
**Phase 1**: JSON structured logging + MDC  
**Phase 2**: `CorrelationIdFilter` — UUID per request  
**Phase 3**: Micrometer + Prometheus + tenant metric tag  
**Phase 4**: Custom `DatabaseHealthIndicator`

### 4.6 Prompt Spec: `security-hardening.md`

**Phases**: 6  
**Phase 0**: Security headers (HSTS, CSP, X-Frame-Options)  
**Phase 1**: Bucket4j rate limiting (auth: 10/min/IP, global: 1000/min/tenant)  
**Phase 2**: Secrets cleanup (`.env.example`, remove plaintext from docker-compose)  
**Phase 3**: `@PostConstruct` env var validation  
**Phase 4**: Token refresh + logout endpoints  
**Phase 5**: ADRs 0010 + 0011

---

## 5. Agent Behavioral Constraints

### 5.1 Immutable Rules (apply to every session)

1. **Read before write**: Always read `AI-CODE-REF.md` and `DESIGN.md` before modifying any source file.
2. **Prototype bean enforcement**: Never use `new` for JPA entities. Always `applicationContext.getBean()`.
3. **Composite key awareness**: Every tenant-scoped query must include `tenantId`. Never query by entity ID alone.
4. **Test conventions are law**: Given-When-Then, `shouldDoX_whenY()`, zero `any()`, `doNothing()` for void stubs, exact parameter matching, `@Nested` + `@DisplayName`.
5. **Constants over literals**: Every string in production code → `public static final`. Tests reference constants directly.
6. **No documentation generation unless asked**: No README, summary, or explanation files as side-effects.
7. **Copyright header**: Required on every new `.java` file.

### 5.2 Decision Boundaries

**Agent decides autonomously**: Import ordering, private method extraction, variable naming (local scope), test helper placement.

**Agent must ask the developer**: New module dependencies not in prompt, OpenAPI contract changes, database schema changes, security config changes, architecture decisions.

### 5.3 Error Recovery Protocol

When compilation fails:
1. Read exact compiler error
2. Check generated-source issue → `mvn generate-sources -pl {module}`
3. Check missing dependency → verify pom.xml
4. Check type mismatch → read actual class (don't assume)
5. If 3 attempts fail on same error → STOP, report to developer

When tests fail:
1. Read failure output completely
2. Distinguish test logic error vs production code error
3. Fix production code, not the test (unless test is wrong)
4. Re-run failing test class → then full module suite

---

## 6. Knowledge Graph (Agent Context Loading)

When starting a session for a specific module, the agent loads context in this order:

```
Level 0 (always): CLAUDE.md → AI-CODE-REF.md → DESIGN.md
Level 1 (module): {module}/pom.xml → OpenAPI YAML → existing source structure
Level 2 (entity): DataModel class → CompositeId inner class → Repository interface
Level 3 (task):   Execution prompt → referenced spec docs → existing tests
```

### Module → Key Files Quick Reference

| Module | Entry Points |
|--------|-------------|
| `utilities` | `MessageService.java`, `EntityType.java`, `DeleteUseCaseSupport.java`, `BaseControllerAdvice.java`, `SequentialIDGenerator.java` |
| `multi-tenant-data` | Entity chain: `Auditable → SoftDeletable → TenantScoped → AbstractUser → Concrete` |
| `infra-common` | `TenantContextHolder.java`, `EntityIdAssigner.java`, `IdAssignationPreInsertEventListener.java` |
| `security` | `SecurityConfig.java`, `JwtTokenProvider.java`, `JwtRequestFilter.java` |
| `user-management` | 5 aggregates: employee, collaborator, adult-student, tutor, minor-student |
| `billing` | 6 aggregates: membership, compensation, payment×2, membership-assoc×2 |
| `course-management` | 3 aggregates: course, schedule, course-event |
| `tenant-management` | 3 entities: tenant, subscription, billing-cycle |
| `mock-data-system` | `MockDataOrchestrator.java`, DAG-based entity dependency ordering |

---

## 7. Execution Roadmap

### 7.1 Immediate Next Steps (in order)

| Step | Action | Mode | Output |
|------|--------|------|--------|
| ~~1~~ | ~~Execute Wave 1~~ | ~~Executor~~ | ✅ Done |
| ~~2~~ | ~~Execute Wave 3~~ | ~~Executor~~ | ✅ Done |
| ~~3~~ | ~~Execute Wave 4~~ | ~~Executor~~ | ✅ Done |
| ~~4~~ | ~~Execute Wave 0~~ | ~~Executor~~ | ✅ Done — Boot 4.0.3 + Java 24 + Jackson 3 |
| ~~5~~ | ~~Execute Wave 5~~ | ~~Executor~~ | ✅ Done — CI/CD green |
| ~~6a~~ | ~~Execute Wave 8~~ | ~~Executor~~ | ✅ Done — Health + metrics + correlation ID |
| ~~6b~~ | ~~Execute Wave 10~~ | ~~Executor~~ | ✅ Done — etl removed, audit 501 stub |
| 7 | Complete Wave 9 (rate limiting + refresh) | Executor | Rate limiting + token refresh |
| 8 | Execute Wave 6 (Docker required) | Executor | E2E suite passing |
| 9 | **Write** `flyway-baseline.md` + Execute Wave 7 (Docker required) | Architect+Executor | Flyway migrations |
| 10 | Complete Wave 11 (consumer docs) | Architect | Error codes + onboarding guide |

### 7.2 Estimated Timeline (at ~4h/day, from current state)

| Week | Waves | Key Milestone |
|------|-------|---------------|
| 1 | Wave 9 remainder + Wave 6 (E2E, needs Docker) | Rate limiting + E2E passing |
| 2 | Wave 7 (Flyway, needs Docker) | Database migrations |
| 3 | Wave 11 remainder (docs) | API docs + beta checklist |

---

## 8. Quality Gates

### 8.1 Per-Wave Gates

Every wave must pass ALL of these before the next wave begins:

- [ ] `mvn clean compile -DskipTests` — zero compilation errors
- [ ] `mvn test -pl {affected-modules} -am` — zero test failures
- [ ] `grep` validation for wave-specific invariants (from exit criteria)
- [ ] All commits follow Conventional Commits format
- [ ] No uncommitted changes in working tree

### 8.2 Beta Gate (cumulative)

The final beta gate is the checklist in `BETA-MATURITY-PLAN.md`. Every checkbox verified by running the specified command — no self-reported assertions.

---

## 9. Risk Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Spring Boot 4.0.2 breaking changes | Wave 0 blocks everything | Prompt includes troubleshooting table |
| Jackson 3 breaks generated OpenAPI code | Compile failures | Compatibility bridge; generated code excluded |
| `@SQLDelete` fix changes Hibernate behavior | Silent regression | Reflective test written BEFORE fix |
| Testcontainers port conflicts | Flaky CI | `@DynamicPropertySource` with random port |
| Wave 1 migration breaks E2E assertions | E2E fails on `$.code` | Wave 6 accounts for new codes |
| Long sessions lose context | Convention drift | Compile gate forces re-read on failure |

---

## Appendix A: File Map for Agent Context Loading

```
platform-core-api/
├── CLAUDE.md                                  ← Entry point → docs/directives/CLAUDE.md
├── .claude/
│   ├── settings.json
│   └── settings.local.json                    ← Agent permissions
├── docs/
│   ├── MANIFEST.md                            ← Documentation status tracker
│   ├── directives/
│   │   ├── CLAUDE.md                          ← L0: Full project context & coding directives
│   │   ├── AI-CODE-REF.md                     ← L0: Coding standards (956 lines)
│   │   └── AGENT-STRATEGY.md                  ← This document
│   ├── design/
│   │   ├── DESIGN.md                          ← L0: Architecture, module catalog
│   │   ├── beta-maturity-design.md            ← Master execution plan
│   │   ├── delete-usecase-strategy.md         ← Wave 3 spec
│   │   ├── exception-advice-strategy.md       ← Wave 1 spec
│   │   ├── adr/                               ← Architecture Decision Records (6 active)
│   │   └── completed/                         ← Superseded designs
│   ├── prompts/
│   │   ├── pending/                           ← Ready-to-execute Claude Code prompts
│   │   └── completed/                         ← Executed prompts (historical)
│   └── workflows/
│       ├── pending/                           ← Ready-to-execute step-by-step plans
│       └── completed/                         ← Executed workflows (historical)
└── {14 Maven modules}                         ← Source code
```

## Appendix B: Conventional Commits Reference

```
Type        When to use
────        ───────────
feat        New functionality (use case, endpoint, entity)
fix         Bug fix (runtime error, logic error, data corruption)
refactor    Code restructuring without behavior change
test        Adding or fixing tests only
build       Dependency, plugin, or build configuration changes
docs        Documentation only (MD files, Javadoc, ADRs)
ci          CI/CD pipeline changes (GitHub Actions, Docker)
perf        Performance improvement
chore       Maintenance (cleanup, formatting, tooling)

Scope       Module or concern
─────       ─────────────────
security, user, billing, course, tenant, notification, pos,
utilities, infra, multi-tenant, ca, mock-data, etl, audit,
build, api, test, docs, docker
```
