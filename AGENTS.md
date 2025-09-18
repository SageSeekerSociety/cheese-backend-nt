# Repository Guidelines

## Project Structure & Module Organization
Primary services live under `src/main/kotlin/org/rucca/cheese`, organized by domain (auth, analytics, etc.) and should mirror REST API groupings. Configuration, SQL, and i18n assets sit in `src/main/resources`. Tests and fixtures live in `src/test/kotlin`, where `utils/` offers reusable creators for data-heavy scenarios. API contracts reside in `design/API/NT-API.yml`; database DDL and migrations stay in `design/DB`. Supporting specs and architecture notes are in `docs/` and `specs/`, while helper scripts belong in `scripts/`.

## Build, Test, and Development Commands
- `docker compose up -d` starts Postgres, Redis, and Elasticsearch needed for local Spring Boot runs.
- `./mvnw clean verify` regenerates OpenAPI stubs, compiles Kotlin, runs unit/integration tests, and executes Spotless checks.
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` launches the application against your running services.
- `./mvnw spotless:apply` formats Kotlin/Java/SQL/Markdown before you push.
- `./mvnw jacoco:report` emits coverage HTML to `target/site/jacoco/index.html` for PR evidence.

## Coding Style & Naming Conventions
Kotlin code follows ktfmt Google style (4-space indents, 100-column wrap) enforced by Spotless. Package paths should remain lowercase, and classes/records use PascalCase; functions and variables stay camelCase. Prefer Spring constructor injection, and annotate nullable flows explicitly. Keep generated sources out of VCS—`target/` stays ignored.

## Testing Guidelines
Spring Boot + JUnit 5 drive tests; use `*Test.kt` suffixes and JUnit display names for clarity. Lightweight unit tests should isolate services with MockK (`springmockk`); integration tests can lean on `@SpringBootTest` with `@ActiveProfiles("test")` for the H2-backed profile. Ensure new features include regression coverage and update fixtures in `src/test/kotlin/org/rucca/cheese/utils` when data contracts change.

## Commit & Pull Request Guidelines
History mixes descriptive titles with Conventional Commit prefixes (`fix:`, `style:`); prefer imperative, issue-linked subject lines. Each PR should note environment impacts (new env vars, migrations, or API contract edits) and include `./mvnw clean verify` output or coverage links. Attach screenshots or sample payloads when endpoint behavior changes, and request at least one domain reviewer before merge. Remember: hooks install on `./mvnw initialize`, so run it once per clone to keep checks consistent.

## Environment & Configuration Tips
Copy `sample.env` to `.env` and align values with the compose stack; secrets must remain out of git. `postgres-init.sh` seeds baseline schemas when containers first boot. Keep Flyway migrations strictly additive and versioned, and document any configuration toggles in `docs/` so operators can mirror them in staging.
