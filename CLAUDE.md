# digital-asset-service

A tokenized deposit ("EURT") platform built as Java microservices.

## Architecture
- **asset-service**: wallets, mint / burn / transfer, balances and ledger history.
- Target design: every state change emits a Kafka event via a transactional outbox; a separate **audit-service**
  consumes these events and keeps an audit trail.

## Repo layout
- Monorepo; each service is an independent Maven project in its own folder with its own Maven Wrapper and build.
- Base package: `io.github.cigdemturkmen.digitalasset.<service>` (groupId `io.github.cigdemturkmen`).
- Line endings are normalized to LF via `.gitattributes` (Windows dev machines, Linux CI/containers).

## Stack
Java 21, Spring Boot 4.1, Maven Wrapper, Spring Web MVC, Spring Data JPA (Hibernate), PostgreSQL 18, Flyway,
springdoc-openapi (Swagger UI), Actuator, JUnit 5 + AssertJ.

## Commands
- Docker must be running: `spring-boot:run` and the tests start `asset-service/compose.yaml` (PostgreSQL)
  through Spring Boot's Docker Compose support; connection details come from the container, not application.yml.
- Build and test: `cd asset-service && ./mvnw test`
- Run locally: `./mvnw spring-boot:run`, then:
  - http://localhost:8080/actuator/health (health check)
  - http://localhost:8080/swagger-ui.html (Swagger UI: try the API in the browser)
  - http://localhost:8080/v3/api-docs (OpenAPI document as JSON)
- Query the local database: `docker compose exec postgres psql -U asset -d assets` (from `asset-service/`)

## asset-service design
- Package-by-feature: `wallet` (entity, service, exceptions), `wallet.api` (controllers + record DTOs), `ledger`.
- Schema is owned by Flyway (`src/main/resources/db/migration`); Hibernate runs with `ddl-auto: validate`.
- Money is `BigDecimal` / `NUMERIC(19,4)`, compared with `compareTo`, never `double`.
- `wallets.balance` is a cached projection; `ledger_entries` is the append-only source of truth. Every balance change
  and its ledger entry are written in one `@Transactional` service method. DB CHECK constraints back up the rules.
- Timestamps are `Instant` truncated to microseconds (PostgreSQL precision).
- Controllers carry `@Tag` / `@Operation(summary)` for the generated OpenAPI document.

## Conventions
- Code, commit messages and docs in English.
- Git workflow: one branch per feature (`feature/<topic>`) with small, focused commits → Pull Request with a
  description of what, why and trade-offs → merge commit (no squash, so history stays readable) → tag on main
  for notable milestones.
- Tests: `@SpringBootTest` + `@AutoConfigureMockMvc` (package `org.springframework.boot.webmvc.test.autoconfigure`
  in Boot 4) with `MockMvcTester` and AssertJ.
