# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Conventional Commits](https://www.conventionalcommits.org/).

## [Unreleased]

### Added
- **observability**: `CorrelationIdFilter` — generates/propagates `X-Correlation-Id` via MDC for distributed tracing
- **observability**: `ObservabilityConfig` — per-request tenant ID tagging on Prometheus metrics via `ObservationFilter`
- **observability**: Micrometer Prometheus registry for `/actuator/prometheus` metrics endpoint
- **observability**: ECS structured JSON logging for prod and QA profiles
- **docs**: Swagger UI with JWT Bearer auth via `OpenApiConfig` and `springdoc-openapi`
- **security**: HTTP security headers — HSTS, X-Content-Type-Options, X-Frame-Options (DENY), CSP (`default-src 'none'`)
- **devops**: `.env.example` documenting all required environment variables
- **audit-system**: 501 Not Implemented stub controller for `GET /v1/audit`
- **security**: JWKS registration runner for trust-broker integration (`7f7125f`)
- **ca**: JWKS registry endpoint and single HTTP port architecture (`1f0fb4e`)
- **tenant-management**: CRUD use cases and expanded tenant controller (`f70e22f`)
- **certificate-authority**: Controllers, use cases, and tests for CA module (`c98313f`)
- **notification-system**: `DeleteNotificationUseCase` (`653ff42`)
- **tenant-management**: Delete use cases for tenant entities (`38e4fbb`)
- **pos-system**: Delete use cases for StoreProduct and StoreTransaction (`5a55a93`)
- **course-management**: Delete use cases for Course, Schedule, CourseEvent (`9825a7a`)
- **billing**: Delete use cases for all 6 billing entities (`9948334`)
- **user-management**: Delete use cases for all 5 user entities (`b4a26f2`)
- **utilities**: `DeleteUseCaseSupport` and `TenantContextHolder.requireTenantId()` (`babc1a2`)
- **test**: Comprehensive component tests for all 19 entities across 6 modules (`225eb40`)
- **test**: Integration test infrastructure and creation use case fixes (`067eb37`)
- **devops**: E2E runner service in docker-compose (profile-gated), Newman environment file, `run-e2e.sh` convenience script
- **docs**: Add root `CLAUDE.md` as lightweight Claude Code entry point
- **docs**: Add `SECURITY.md` for responsible vulnerability disclosure
- **docs**: Add `CHANGELOG.md` following Keep a Changelog format
- **docs**: Add `CONTRIBUTING.md` with onboarding guide and workflow
- **docs**: Add `docs/adr/` with Architecture Decision Records

### Changed
- **deps**: Upgrade to Spring Boot 4.0.3, Java 24 (Corretto), Hibernate 7.2.5, Jackson 3
- **deps**: Migrate Jackson annotations to compatibility bridge (`com.fasterxml.jackson.annotation` retained)
- **deps**: Replace `IOException` with unchecked `JacksonException` in all Jackson-using code
- **deps**: Remove `findAndRegisterModules()` calls — Jackson 3 uses SPI auto-discovery
- **ci**: Update `build.yml` — Java 21→24, Zulu→Corretto distribution
- **ci**: Update `docker-deploy-aws.yml` — Java 21→24, Temurin→Corretto, port 8443→8080
- **devops**: Fix `docker-compose.qa.yml` port mapping from 8443 to 8080
- **observability**: Expand actuator endpoints per profile (prod: health/info/prometheus, dev: +metrics/mappings)
- **build**: Remove stale compiler plugin source/target from application module (inherits release 24 from parent)
- **build**: Remove `etl-system` module from parent POM (15 modules now)
- **audit-system**: Replace IntelliJ template `Main.java` with REST stub controller
- **build**: Migrate Docker infrastructure from mTLS to plain HTTP (`2ae4d18`)
- **build**: Modernize Docker and CI configuration (`8bdd0fd`)
- **ci**: Update deploy workflow for HTTP-only architecture (`31d77cc`)
- **deps**: Upgrade 30 dependencies across all tiers (`c731193`)
- **chore**: Rename Makani references to AkademiaPlus, update configs (`61c1659`)
- **openapi**: Add 409 Conflict response to all delete endpoints (`108d5b9`)
- **security**: Extend `BaseControllerAdvice`, fix `InvalidLoginException` to 401 (`dd83236`)
- **notification-system**: Migrate to generic exceptions (`c9e927a`)
- **pos-system**: Migrate to generic exceptions (`5f302ec`)
- **refactor**: Remove 20 deprecated per-entity exceptions and legacy MessageService methods (`e4bea44`)
- **docs**: Rename `CLAUDE.md` → `AI-CODE-REF.md` to avoid collision with Claude Code convention (`d27b57a`)
- **docs**: Rename `ARCHITECTURE.md` → `DESIGN.md` and `AI-code-ref.md` → `CLAUDE.md` (now `AI-CODE-REF.md`)
  with updated cross-references (`03569de`)

### Fixed
- **build**: Add actuator dependency and filter bypass for healthchecks (`c8b1bc4`)
- **build**: Fix Dockerfiles and compose for correct module scoping (`61b3c0c`)
- **build**: Replace runner script with self-contained compose stack (`95bfd44`)
- **multi-tenant-data**: Include entity ID in `@SQLDelete` WHERE clause for all entities (`48d844c`)
- **ca**: Remove `@Profile` from `@SpringBootApplication`, fix ObjectMapper injection (`4539e94`)
- **ca**: Bootstrap CA keystore before Spring SSL init via shell entrypoint (`11d0bab`)
- **docker**: Add `file:` URI prefix to `server.ssl.key/trust-store` JVM args (`b7b81a5`)
- **build**: Configure Lombok annotation processor for maven-compiler-plugin 3.14.0 — processors are
  no longer auto-discovered from the classpath in this version (`86b8f72`)
- **build**: Add missing Lombok dependency to utilities module (`86b8f72`)
