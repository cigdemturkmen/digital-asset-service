# digital-asset-service

A learning/portfolio project: a tokenized deposit (e.g. "EURT") platform built as Java microservices.
Personal context and collaboration preferences live in `CLAUDE.local.md` (git-ignored); read it if present.

## Concept
- **asset-service**: wallets, mint / burn / transfer, balances and transaction history.
- Every state change emits a Kafka event (via transactional outbox); a separate **audit-service** consumes
  these events and keeps an audit trail.

## Repo layout
- Monorepo; each service is an independent Maven project in its own folder (`asset-service/`, later `audit-service/`)
  with its own Maven Wrapper, Dockerfile and build.
- Base package: `io.github.cigdemturkmen.digitalasset.<service>` (groupId `io.github.cigdemturkmen`).
- Line endings are normalized to LF via `.gitattributes` (Windows dev machines, Linux CI/containers).

## Stack
Java 21, Spring Boot 4.1.1 (3.x is no longer offered by Initializr and 3.5 OSS support has ended), Maven Wrapper,
YAML config, Actuator (health/info + liveness/readiness probes). Planned: PostgreSQL + Flyway, Kafka,
Docker Compose, JUnit 5 + Testcontainers, GitHub Actions.

## Commands
- Docker Desktop must be running: `spring-boot:run` and the tests start `asset-service/compose.yaml` (PostgreSQL 18)
  through Spring Boot's Docker Compose support; connection details come from the container, not application.yml.
- Build and test a service: `cd asset-service && ./mvnw test`
- Run locally: `./mvnw spring-boot:run`, then:
  - http://localhost:8080/actuator/health (health check)
  - http://localhost:8080/swagger-ui.html (Swagger UI: try the API in the browser)
  - http://localhost:8080/v3/api-docs (OpenAPI document as JSON)
- OpenAPI via springdoc-openapi 3.1.x (Boot 4 line); controllers carry `@Tag` / `@Operation(summary)`.

## asset-service design
- Package-by-feature: `wallet` (entity, service, exceptions), `wallet.api` (controllers + record DTOs), `ledger`.
- Schema is owned by Flyway (`src/main/resources/db/migration`); Hibernate runs with `ddl-auto: validate`.
- Money is `BigDecimal` / `NUMERIC(19,4)`, compared with `compareTo`, never `double`.
- `wallets.balance` is a cached projection; `ledger_entries` is the append-only source of truth. Every balance change
  and its ledger entry are written in one `@Transactional` service method. DB CHECK constraints back up the rules.
- Timestamps are `Instant` truncated to microseconds (PostgreSQL precision).

## Conventions
- Code, commit messages and README in English.
- Workflow: each feature on its own branch (`feature/<topic>`, e.g. `feature/domain-api`) with small, focused commits
  → Pull Request with a detailed description (what, why, trade-offs, deliberately deferred) → merge commit (no squash,
  so history stays readable) → descriptive tag on main (e.g. `domain-api-happy-path`). No internal step numbers in names.
- Tests: `@SpringBootTest` + `@AutoConfigureMockMvc` (package `org.springframework.boot.webmvc.test.autoconfigure`
  in Boot 4) with `MockMvcTester` and AssertJ.

## Roadmap
- [x] 0. Environment: JDK 21, Docker Desktop, Git, IDE
- [x] 1. Skeleton: Spring Boot + Java 21, Maven Wrapper, git, health endpoint
- [x] 2. Domain + REST API: wallet, mint, burn, transfer; PostgreSQL + Flyway (branch `feature/domain-api`)
- [ ] 3. Robustness: validation, idempotency key, optimistic locking, ProblemDetail errors, OpenAPI/Swagger
- [ ] 4. Kafka: event publishing via transactional outbox + separate audit-service consumer
- [ ] 5. Dockerfiles + Docker Compose (app, Postgres, Kafka, audit-service)
- [ ] 6. Tests: JUnit 5 + Testcontainers (real Postgres/Kafka)
- [ ] 7. GitHub Actions CI: build, test, Docker image
- [ ] 8. (bonus) ERC-20 mint/burn on a local Anvil chain via web3j; Kubernetes manifests
- [ ] 9. README, architecture diagram
