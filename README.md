# AkademiaPlus — Platform Core API

> Multi-tenant SaaS backend for educational institutions. Built with Java 21, Spring Boot 4, and defense-in-depth security.

[![SonarQube](https://github.com/ElatusDev/platform-core-api/actions/workflows/build.yml/badge.svg)](https://github.com/ElatusDev/platform-core-api/actions)

---

## What is this?

AkademiaPlus is a platform that lets educational institutions (academies, tutoring centers, schools) manage their students, courses, billing, and communications — all from a single multi-tenant system where each institution's data is completely isolated.

This repository contains the **Platform Core API**: the main backend service that powers the platform.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.0-M3 |
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
├── certificate-authority/  Standalone — TLS/PKI certificate service
├── mock-data-system/       Standalone — test data generation with DataFaker
├── etl-system/             Standalone — ETL pipelines (placeholder)
├── audit-system/           Standalone — audit logging (placeholder)
│
├── application/            Main entry point — assembles all modules
├── db_init/                Database initialization scripts
└── .github/workflows/      CI/CD pipeline definitions
```

---

## Quick Start

### Prerequisites

- Java 21 (JDK)
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
# Start all services (API + MariaDB + Redis + CA)
docker compose -f docker-compose.dev.yml up --build
```

This starts:
- **Platform Core API** on `https://localhost:8443`
- **MariaDB** on `localhost:3307`
- **Redis** on `localhost:6379`
- **Certificate Authority** on `https://localhost:8081`

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

For the full architecture deep-dive, see [DESIGN.md](DESIGN.md).

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

For architecture and system design, see [DESIGN.md](DESIGN.md).

---

## Contributing

1. Read [AI-CODE-REF.md](AI-CODE-REF.md) before writing any code
2. Follow the existing module structure and naming conventions
3. Add comprehensive tests using the Given-When-Then pattern
4. Ensure all public APIs have Javadoc with `@param`, `@return`, `@throws`
5. Include the ElatusDev copyright header on all new files
6. Run `mvn test` before pushing

---

## License

Proprietary — © 2025 ElatusDev. All rights reserved.  
Unauthorized copying, distribution, or modification is strictly prohibited.
