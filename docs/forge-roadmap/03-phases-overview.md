# 03 — Phases Overview

Phases 1–3 are deliberately small and **demo-able**. By the end of Phase 2 you have a screenshot-able,
architecturally-impressive system to talk about in interviews **even before any AI exists**. Phase 3
adds the AI wow-factor. Phases 4+ are the "and here's how I'd scale it" story.

The detailed week-by-week how-to + Dev A/B split for each phase lives in the `phase-*.md` files.

---

## Phase 0 — Foundations (Week 1) — *"Set up like a pro"*
- **Goal:** Repo, CI, conventions, local infra skeleton. Zero feature code, maximum hygiene.
- **Features:** none (infra only).
- **Deliverables:** GitHub repo · `docker-compose.yml` (postgres, redis) · GitHub Actions (build +
  test + spotless) · README + ADR-0001 (tech choices) · branch protection · Spring Boot skeleton
  boots, `/health` 200 · one Testcontainers test green in CI.
- **Architecture change:** establishes the monolith skeleton.
- **Learning:** Maven, Spring Boot 3 setup, GitHub Actions, Docker Compose, Testcontainers.
- **Resume value:** "Set up CI/CD with quality gates and containerized local dev from day one."

## Phase 1 — Deterministic Repo Understanding (Weeks 2–4) — *"FORGE can read Java"*
- **Goal:** Submit a Java repo URL → get a structured architecture map back. 100% deterministic.
- **Features:** repo clone (JGit) · **JavaParser** AST walk · package/class/method inventory + **call
  graph** + dependency edges · persist to Postgres · REST endpoints to fetch the map · simple
  dashboard tree/graph view.
- **Deliverables:** `POST /repos`, `GET /repos/{id}/map` · map persisted · unit + integration tests
  (Testcontainers) · demo: point it at a real OSS Spring repo, render the map.
- **Architecture change:** API + Postgres + Repo Analyzer; JPA entities `Repo`, `ClassNode`,
  `MethodNode`, `Edge`.
- **Learning:** AST parsing, graph modeling in SQL, JPA relationships, clean layering.
- **Resume value:** "Built a static-analysis engine that maps Java repo architecture via AST parsing."
  *Already a strong portfolio piece.*

## Phase 2 — Deterministic Execution Pipeline + Observability (Weeks 5–8) — *"FORGE runs & tests code, safely & observably"*
- **Goal:** Submit a task → async pipeline plans (rule-based) → applies a deterministic codemod →
  **runs build+tests in a Docker sandbox** → reports pass/fail → bounded retry. Full observability.
- **Features:** Postgres-backed **job queue (`SKIP LOCKED`)** · **worker pool** (virtual threads) ·
  **job state machine** · **Docker sandbox executor** (resource+time-limited, no host access) ·
  deterministic codemod · test-result parsing · bounded retry w/ backoff · **OpenTelemetry traces**
  (1/job) · **Prometheus metrics** (queue depth, latency, success rate) · **Grafana dashboard** ·
  structured logs w/ correlation IDs.
- **Deliverables:** `POST /jobs`, `GET /jobs/{id}` (live state + trace link) · Grafana dashboard
  screenshot · demo: task → sandboxed run → green tests → traced timeline.
- **Architecture change:** async queue + workers + state machine + sandbox + full OTel stack.
- **Learning:** concurrency (executors, virtual threads, `CompletableFuture`), queue semantics,
  idempotency, container isolation/security, distributed tracing, metrics, SLOs.
- **Resume value:** *The big one.* "Designed a concurrent, observable, fault-tolerant pipeline that
  runs untrusted code in isolated Docker sandboxes with tracing and retry." Carries a full
  system-design round.

## Phase 3 — AI Augmentation (Weeks 9–14) — *"FORGE gets intelligent"*
- **Goal:** Replace/augment the rule-based Planner, CodeGen, and Failure-Analyzer with LLMs *behind
  the same interfaces*, so the deterministic system still works with AI stubbed out.
- **Features:** Ollama + Spring AI · **LLM Planner** · **LLM CodeGen** (patch *always* gated by
  compile+test) · **RAG** over the repo map using **pgvector** · **LLM Failure-Analyzer** (explain +
  propose fix → feeds retry) · **memory store** (embed outcomes; recall similar past runs) · paid-API
  fallback with cost cap + prompt caching + rate limiting · **eval harness** (fixed task set scored
  for success-rate).
- **Deliverables:** AI on/off feature flag (deterministic fallback proven) · RAG retrieval endpoint ·
  eval report (success-rate table across models) · demo: NL task → AI plan → generated patch →
  sandbox-validated → self-corrected on failure.
- **Architecture change:** Ollama + Spring AI; pgvector; memory tables; eval module.
- **Learning:** LLM fundamentals, prompt engineering, RAG, embeddings/vector search, agent loop
  design, evaluation systems, cost/rate-limit/caching discipline.
- **Resume value:** "Layered an LLM agent (RAG + memory + eval-driven) onto a deterministic pipeline;
  AI is swappable and validated by real test execution — not a blind LLM wrapper." Differentiated.

## Phase 4+ — Scaling (post-placement)
- Services + gRPC, Kubernetes, distributed queue (Kafka/SQS), multi-repo, multi-agent, SaaS. See
  `08-scaling-path.md`. Do these *opportunistically* once you're interviewing.

---

## Milestone summary

| Phase | Weeks | Demo | When to apply |
|---|---|---|---|
| 0 | 1 | CI green + app boots | — |
| 1 | 2–4 | Repo → architecture map rendered | — |
| 2 | 5–8 | Task → sandboxed run → trace + Grafana | **Start applying** |
| 3 | 9–14 | NL task → AI plan → patch → self-correct → eval | Full MVP, all bullets ready |
| 4+ | 15+ | k8s / multi-agent / SaaS | Opportunistic, interview-driven |
