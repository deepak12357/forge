# 06 — Interview & Resume Strategy

> **Framing rule:** lead each résumé bullet with the *engineering* (concurrency, isolation,
> observability, distributed design) and mention AI second. Interviewers are flooded with "I built an
> AI wrapper" — you built a *reliable system that happens to use AI*. That ordering is your edge.

---

## After Phase 1 (Week 4)

**Résumé bullets**
- Built a Java static-analysis engine (JavaParser/AST) that ingests Git repositories and produces a
  queryable architecture + call-graph map, persisted in PostgreSQL.
- Modeled a code graph (classes, methods, call/dependency edges) with Spring Data JPA; exposed it via
  a REST API and a lightweight dashboard.

**STAR stories**
- *Ownership:* "I set up the repo, CI, and conventions, then built the AST parser…"
- *Data modeling:* "I had to represent a call graph in a relational DB — here's how I modeled
  nodes/edges and avoided N+1 on traversal."
- *Clean layering:* controller → service → analyzer → repository separation.

**MNC topics demonstrated:** OOP design · JPA/ORM · AST/static analysis · testing (Testcontainers).

---

## After Phase 2 (Week 8) — *your strongest interview asset*

**Résumé bullets**
- Designed a concurrent, fault-tolerant execution pipeline (PostgreSQL `SKIP LOCKED` job queue,
  virtual-thread worker pool, explicit state machine) that runs untrusted code in resource-limited
  Docker sandboxes.
- Instrumented the system end-to-end with OpenTelemetry (one distributed trace per job) and
  Prometheus/Grafana dashboards; added bounded retries with backoff and graceful shutdown.

**STAR stories**
- *Concurrency:* "Two workers must never process the same job — I made job claiming atomic with a
  transactional `SKIP LOCKED` query; here's the test that proves exactly-once under contention."
- *System design tradeoff:* "Why a Postgres-backed queue over Redis/Kafka for v1" (ADR-0003).
- *Security:* "Running untrusted generated code safely — `--network=none`, CPU/mem/time limits,
  read-only mounts, no host access" (ADR-0004).
- *Observability:* "I can click any job and see its full trace across analyze→plan→codegen→execute,
  plus success-rate and queue-depth SLOs in Grafana."
- *Reliability:* bounded retry + backoff + idempotency.

**MNC topics demonstrated:** System Design · Concurrency · Distributed Systems · Observability ·
Infrastructure · Security. *This phase alone supports a full system-design round — start applying.*

---

## After Phase 3 (Week 14)

**Résumé bullets**
- Layered an LLM agent (RAG over pgvector, run-memory, eval-driven prompt tuning, cost-capped
  hybrid local/cloud inference) onto the deterministic pipeline; all AI output validated by real
  compile+test execution with automated self-correction.
- Built an evaluation harness measuring task success-rate, latency, and cost across local (Ollama)
  and hosted models to drive prompt/model decisions with data.

**STAR stories**
- *AI systems judgment:* "Why I built deterministic-first and made AI swappable — the whole pipeline
  still passes with `ai.enabled=false`." (Your single best architecture-judgment story.)
- *RAG:* "Naive whole-repo prompts were noisy and blew the context window; I embedded
  classes/methods into pgvector and retrieved top-k relevant context — measured the quality lift in
  the eval harness."
- *Evaluation:* "How do I know a prompt change helped? Fixed task set, success-rate table."
- *Cost/rate discipline:* "Monthly cost cap, token budget, prompt caching, rate limiting, graceful
  degradation to the local model."

**MNC topics demonstrated:** AI Systems · RAG/LLMOps · Evaluation · plus everything from Phase 2.

---

## After Phase 4+ (post-placement)

**Résumé bullets**
- Split the monolith into API + executor services over gRPC; deployed to Kubernetes with
  queue-depth-driven autoscaling (HPA).
- Introduced a distributed queue (Kafka/SQS) and multi-repo, multi-agent orchestration.

**STAR stories:** scalability · microservice boundaries · k8s ops · distributed workflow design.

**MNC topics demonstrated:** Distributed Systems at scale · Cloud-native · DevOps.

---

## Cross-cutting interview prep

1. **Keep ADRs.** Each ADR = a ready-made "tell me about a hard technical decision" answer. Aim for
   one per real decision (queue choice, sandbox isolation, AI architecture, eval methodology).
2. **Be able to whiteboard FORGE's architecture from memory** in 5 minutes (practice the diagram in
   `02-architecture.md`).
3. **Rehearse one STAR story per phase** until it's crisp (Situation → Task → Action → Result, with a
   metric in the Result).
4. **Have a 2–4 min demo video per phase** linked from your résumé/GitHub — cheaper and often more
   convincing than a live deployment.
5. **Know your numbers:** worker pool size, jobs/min throughput, p50/p95 job latency, eval
   success-rate, monthly cost. Metrics make stories credible.
6. **README sells the repo.** A reviewer who opens your GitHub should understand FORGE in 60 seconds
   from the README + architecture diagram.

---

## Résumé "Projects" section (template)

> **FORGE — Autonomous Backend Engineering Platform** *(Java 21, Spring Boot, PostgreSQL, Docker,
> OpenTelemetry, Ollama/Spring AI)* — github.com/<you>/forge
> - Built a deterministic pipeline that maps Java repos (JavaParser AST), plans tasks, runs untrusted
>   code in resource-limited Docker sandboxes, and self-corrects on test failure.
> - Concurrent worker pool over a PostgreSQL `SKIP LOCKED` queue with exactly-once job processing,
>   bounded retries, and full OpenTelemetry tracing + Grafana SLO dashboards.
> - Layered a swappable LLM agent (RAG via pgvector, run-memory, eval harness, cost-capped hybrid
>   inference) over the deterministic core — AI validated by real test execution, not trusted blindly.
