# AkademiaPlus — Platform Core API

> Multi-tenant SaaS backend for educational institutions. Built with Java 24, Spring Boot 4, and defense-in-depth security.

[![SonarQube](https://github.com/ElatusDev/platform-core-api/actions/workflows/build.yml/badge.svg)](https://github.com/ElatusDev/platform-core-api/actions)

---

## What is this?

AkademiaPlus is a platform that lets educational institutions (academies, tutoring centers, schools) manage their students, courses, billing, and communications — all from a single multi-tenant system where each institution's data is completely isolated.

This repository contains the **Platform Core API**: the main backend service that powers the platform.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 24 |
| Framework | Spring Boot 4.0.3 |
| Database | MariaDB (multi-tenant, row-level isolation) |
| Cache | Redis |
| Security | JWT, AES-256-GCM field encryption, SHA-256 hashing |
| API Design | OpenAPI 3.x (contract-first, code-generated DTOs) |
| Payments | MercadoPago SDK |
| Build | Maven (multi-module) |
| CI/CD | GitHub Actions → SonarQube → Docker → AWS |
| Testing | JUnit 5, Mockito, AssertJ, Testcontainers |

---

## Project Structure

```
platform-core-api/
│
├── infra-common/           Foundation — persistence base classes, tenant context, ID generation
├── utilities/              Security services — encryption, hashing, PII normalization
├── multi-tenant-data/      JPA entity models (shared, no business logic)
├── security/               JWT authentication, authorization filters
│
├── user-management/        Domain — employees, students, tutors, collaborators
├── billing/                Domain — payments, memberships, payroll
├── course-management/      Domain — courses, schedules, events
├── tenant-management/      Domain — tenant creation and lifecycle
├── notification-system/    Domain — email, SMS, push (API specs defined)
├── pos-system/             Domain — point-of-sale (placeholder)
│
├── certificate-authority/  Standalone — Trust broker (JWKS registry for JWT public keys)
├── mock-data-service/      Standalone — test data generation with DataFaker
├── etl-service/            Standalone — ETL pipelines (placeholder)
├── audit-service/          Standalone — audit logging (placeholder)
│
├── application/            Main entry point — assembles all modules
├── db_init/                Database initialization scripts
└── .github/workflows/      CI/CD pipeline definitions
```

---

## Quick Start

### Prerequisites

- Java 24 (JDK)
- Maven 3.9+
- Docker & Docker Compose
- MariaDB (or use the Docker Compose setup)

### Build

```bash
# Full build with tests
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Build specific module
mvn clean install -pl utilities
```

### Run with Docker

```bash
# Start all services (API + MariaDB + Redis + Trust Broker + Mock Data)
docker compose -f docker-compose.dev.yml up --build
```

This starts:
- **Platform Core API** on `http://localhost:8080`
- **MariaDB** on `localhost:3307`
- **Redis** on `localhost:6379`
- **Trust Broker** (JWKS registry) on `http://localhost:8082`
- **Mock Data System** on `http://localhost:8180`

### Run Tests

```bash
# All tests
mvn test

# Specific module
mvn test -pl utilities

# Specific test class
mvn test -pl utilities -Dtest=HashingServiceTest
```

---

## Architecture Highlights

**Multi-tenancy** — Every entity carries a `tenantId` as part of its composite primary key. Hibernate filters enforce row-level isolation automatically. There is no way to accidentally query across tenants.

**Defense-in-depth security** — PII fields are encrypted at rest with AES-256-GCM. Hash columns enable indexed search without decryption. Email validation uses possessive-quantifier regex to prevent ReDoS. Hash comparisons use constant-time algorithms to prevent timing attacks.

**Clean Architecture** — Domain modules follow `interfaceadapters/` + `usecases/` structure. One use case class per operation. Controllers are thin delegates. Business logic never leaks into controllers or repositories.

**OpenAPI-first** — API contracts are defined as YAML specs. Maven generates controller interfaces and DTOs during build. Implementation classes implement the generated interfaces.

For the full architecture deep-dive, see [DESIGN.md](docs/design/DESIGN.md).

---

## Documentation

| Document | Purpose |
|----------|---------|
| [DESIGN.md](docs/design/DESIGN.md) | Architecture, module catalog, multi-tenancy model, security layers |
| [AI-CODE-REF.md](docs/directives/AI-CODE-REF.md) | Coding standards, review rules, detection patterns, test conventions |
| [SECURITY.md](docs/design/SECURITY.md) | Vulnerability disclosure policy |
| [docs/design/adr/](docs/design/adr/) | Architecture Decision Records for key technical choices |

---

## Maven Profiles

| Profile | Command | What it builds |
|---------|---------|---------------|
| Default | `mvn clean install` | Full platform (all modules) |
| `ca-service` | `mvn clean install -P ca-service` | Certificate Authority only |
| `mock-data-service` | `mvn clean install -P mock-data-service` | Mock data generator |

---

## Development Standards

This project enforces strict coding standards documented in [AI-CODE-REF.md](AI-CODE-REF.md). Key highlights:

- **Testing**: Given-When-Then pattern, `shouldDoX_whenGivenY()` naming, zero `any()` matchers
- **Security**: Specific exception catching, no swallowed exceptions, constant extraction for all strings
- **Quality**: Methods < 20 lines, cyclomatic complexity < 10, Javadoc on all public APIs
- **Data**: All IDs are `Long`, soft-delete only (no physical deletes), audit timestamps on everything

For architecture and system design, see [DESIGN.md](docs/design/DESIGN.md).

---

## Contributing

commit conventions, and PR checklist.

---

## License

Proprietary — © 2025 ElatusDev. All rights reserved.  
Unauthorized copying, distribution, or modification is strictly prohibited.
