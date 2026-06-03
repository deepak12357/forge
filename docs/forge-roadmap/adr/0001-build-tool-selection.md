# ADR-0001 — Build Tool Selection

- **Status:** Accepted
- **Date:** <fill in when you start Week 1>
- **Deciders:** Dev A, Dev B
- **Phase / Week:** Phase 0, Week 1

> This ADR is pre-filled with the roadmap's recommendation. Confirm it together in the Week-1
> pair-design session, adjust if you disagree, set the date, and commit it to the real FORGE repo
> under `docs/adr/0001-build-tool-selection.md`. It also serves as the template for ADR-0002…0006.

## Context

FORGE is a Java 21 / Spring Boot 3 project built by two engineers (3 yrs each) with a Spring Boot
background, optimized for fast, low-friction progress toward a demo-able MVP and strong MNC résumé
value. We must choose a build tool before any code lands, because it shapes the project structure,
CI, and the local-dev experience for both developers.

The realistic options are **Maven** and **Gradle**. Both are free, mature, first-class with Spring
Boot, and recognized by employers.

## Decision

**Use Maven.**

## Rationale

- **Zero ramp-up.** Both developers already know Maven from prior Spring Boot work; learning budget
  is better spent on the project's actual hard topics (concurrency, AST parsing, observability, RAG).
- **Convention over configuration.** A declarative POM with a fixed lifecycle has fewer ways to go
  wrong than flexible Groovy/Kotlin build scripts — fewer build-debugging detours.
- **MNC ubiquity.** Maven is extremely common in enterprise Java shops; its résumé value is at least
  equal to Gradle's for our target roles.
- **Tooling parity.** Everything in the roadmap (Spotless, JaCoCo, Flyway, Testcontainers,
  JavaParser) has a well-supported Maven plugin or dependency.

We classified Gradle as a fine alternative (faster incremental builds, "modern" Kotlin DSL signal),
but its advantages do not outweigh the familiarity benefit for this team. This is acknowledged as a
near-coin-flip decision, not a strong technical mandate.

## Consequences

**Positive**
- Immediate productivity; no build-tool learning curve.
- Simple, conventional project layout that new readers (and interviewers) parse instantly.

**Negative / trade-offs**
- Slower clean builds and less granular incremental compilation than Gradle (negligible at our
  scale; revisit only if build time becomes a real pain in Phase 4+).
- XML is more verbose than the Kotlin DSL.

**Follow-ups / implementation notes**
- Commit the **Maven Wrapper** (`mvn wrapper:wrapper`) so CI and both laptops use an identical Maven
  version — the same "identical environment" guarantee Docker gives us for Postgres/Redis.
- CI runs `./mvnw -B spotless:check verify` with `setup-java` + `cache: maven`.
- Set Java 21 via `<java.version>21</java.version>` (picked up by `maven-compiler-plugin`).
- **Note:** this choice is for *FORGE itself*. FORGE's sandbox executor must still build/test
  **target repositories** that may use either Maven **or** Gradle — the result parser handles both
  (`target/surefire-reports` and `build/test-results`). See `phase-2-execution-pipeline.md` Week 6.

## Revisit if

- Clean/incremental build times become a recurring bottleneck after the service split (Phase 4+).
- A required tool/plugin has materially better Gradle support.
