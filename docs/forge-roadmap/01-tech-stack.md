# 01 — Technology Selection

Classification: **[MUST]** learn now (core + high market demand) · **[GOOD]** learn when the phase
needs it · **[OPT]** optional / later / résumé garnish. Every choice is free unless noted.

For each tech: **why chosen · industry adoption · resume value · learning value · free alternative.**

---

## Backend

### Java 21 (LTS) — [MUST]
- **Why:** You already know it — leverage it. Virtual threads (Loom), records, pattern matching,
  sealed types are *exactly* what interviewers probe in experienced-Java rounds.
- **Adoption:** Universal in MNC backend.
- **Resume:** "Modern Java 21 (virtual threads, records)."
- **Learning:** High — modern concurrency + language features.
- **Free?** Yes (Temurin / OpenJDK).

### Spring Boot 3.x — [MUST]
- **Why:** Your strength; FORGE's API + orchestration layer. #1 enterprise Java framework.
- **Adoption:** Default for Java backends.
- **Resume:** Every Java role lists it.
- **Free?** Yes.

### Maven — [MUST]
- **Why:** The build tool you already know from Spring Boot — zero ramp-up. Declarative POM + strong
  conventions = fewer footguns than a flexible build script. Extremely common in enterprise/MNC Java
  shops; résumé value at least equal to Gradle's.
- **Free alt:** Gradle (Kotlin DSL) — faster incremental builds / "modern" signal, but a learning
  curve if you don't already use it. Either is a fine ADR-0001 coin-flip; this plan uses Maven.

### gRPC — [GOOD] (Phase 4)
- **Why:** Typed contract between the FORGE API and the sandbox-executor service when you split them.
- **Resume:** High for distributed-systems roles.
- **Free alt:** REST (use it for v1).

---

## Frontend (kept deliberately thin)

### React + TypeScript + Vite — [GOOD]
- **Why:** A small dashboard (job list, run timeline, logs, repo-map viewer) makes FORGE demo-able
  and screenshot-able. TS is the market default.
- **Resume:** "Built a React+TS dashboard."
- **Free alt:** see below.

### Tailwind CSS — [OPT]
- **Why:** Decent-looking UI with zero design skill.

### htmx + Thymeleaf (server-rendered) — [OPT]
- **Why:** *Zero-JS-build alternative* — render from Spring if you want to avoid a frontend stack
  early. Lower resume value, faster to ship.

> **Anti-silo reminder:** the dashboard is small. Both devs touch it during their UI rotation week —
> it is not "the frontend person's job."

---

## Database

### PostgreSQL 16 — [MUST]
- **Why:** The industry-default SQL DB. JSONB, advisory locks, `LISTEN/NOTIFY`, `SKIP LOCKED`, robust
  transactions — FORGE uses all of them for job state and the queue.
- **Adoption:** Enormous.
- **Resume + learning:** Very high (you'll go deep on locking + MVCC).
- **Free?** Yes (Docker).

### Spring Data JPA / Hibernate — [MUST]
- **Why:** Your ORM path; interview-relevant (N+1, fetch strategies, transactions, optimistic locks).

### Flyway — [GOOD]
- **Why:** Versioned DB migrations = hygiene + a clean ADR story.

### Redis — [GOOD] (Phase 2+)
- **Why:** Distributed locks, caching repo maps, rate-limit token buckets, later a Streams-based
  queue.
- **Adoption:** Huge. **Free?** Yes (Docker).

---

## Containers & Sandboxing

### Docker — [MUST]
- **Why:** Baseline, and FORGE *runs untrusted generated code in containers* — sandboxing is the core
  differentiator and a great security story.

### Docker Compose — [MUST]
- **Why:** Local multi-service orchestration for all of Phases 1–3.

### Kubernetes (k3s / kind locally) — [GOOD] (Phase 4)
- **Why:** The scaling story. k3s/kind run locally at $0. Big resume value — *after* the core works.

### gVisor / Firecracker — [OPT] (Phase 4+)
- **Why:** Hardened sandbox isolation; advanced security garnish.

---

## CI/CD

### GitHub Actions — [MUST]
- **Why:** Free for public repos; FORGE's own CI (build, test, lint, scan). Doing it *well* (matrix,
  caching, required checks) is the differentiator.

### Docker build + GHCR — [GOOD]
- **Why:** Push images to GitHub Container Registry, free.

### Self-hosted runner (old PC/laptop) — [OPT]
- **Why:** Zero-cost CI for heavy jobs.

---

## Cloud (AWS)

### LocalStack — [MUST] (early)
- **Why:** Emulates AWS (S3, SQS, …) locally → learn AWS APIs at **$0** before touching a real
  account. Free (community edition).

### AWS S3 — [GOOD]
- **Why:** Artifact/log storage. Generous free tier.

### AWS ECS Fargate *or* EKS — [GOOD] (time-boxed)
- **Why:** One real cloud deployment near placement time for the resume bullet. Fargate =
  cheaper/simpler; EKS = the k8s story. **Costs money — deploy, screenshot, tear down.**

### AWS RDS / SQS — [OPT]
- **Why:** Managed Postgres/queue; swap in only to demonstrate cloud-native. Use sparingly (cost).

### Terraform — [GOOD]
- **Why:** IaC = strong DevOps signal; provisions your AWS bits reproducibly. Works against
  LocalStack too. Free.

---

## Observability

### OpenTelemetry (traces + metrics + logs) — [MUST]
- **Why:** The vendor-neutral industry standard. FORGE's multi-step pipelines *need* tracing. Top-tier
  resume/interview topic right now.

### Prometheus + Grafana — [MUST]
- **Why:** The de-facto OSS metrics + dashboard stack.

### Jaeger (start) / Grafana Tempo — [GOOD]
- **Why:** Trace storage/visualization. Jaeger is the simplest free start.

### Loki — [OPT]
- **Why:** Log aggregation to complete the Grafana single-pane.

### Structured logging (SLF4J + Logback JSON) — [MUST]
- **Why:** Correlation IDs across job steps.

---

## AI / LLM

### Ollama — [MUST]
- **Why:** Run Qwen2.5-Coder / DeepSeek-Coder / Llama locally at $0. Teaches self-hosting,
  quantization, context limits.

### Spring AI — [GOOD]
- **Why:** Spring's LLM abstraction (chat, embeddings, RAG, tool-calling); keeps you in Java with
  swappable providers (Ollama ↔ OpenAI-compatible ↔ Anthropic).

### LangChain4j — [GOOD]
- **Why:** Java-native agent/RAG framework; alternative/complement to Spring AI. **Pick one as
  primary** (recommend Spring AI to stay in the Spring ecosystem).

### Claude Haiku / Gemini Flash API — [GOOD]
- **Why:** The tiny paid fallback for quality-critical code-gen; teaches prod API discipline (rate
  limits, cost caps, prompt caching). ~$5–20/mo; Gemini Flash has a free tier.

### JavaParser — [MUST]
- **Why:** Deterministic AST parsing — the *non-AI* foundation of repo understanding. Huge "I didn't
  just call an LLM" story.

---

## Vector Database

### pgvector — [MUST]
- **Why:** Vector search *inside the Postgres you already run* → zero new infra, real RAG. Best
  cost/learning ratio.

### Qdrant — [GOOD]
- **Why:** Dedicated vector DB if you outgrow pgvector; nice Docker + resume keyword.

### Chroma / FAISS — [OPT]
- **Why:** Lightweight local experimentation.

---

## Messaging / Queue

### Postgres-backed queue (`SKIP LOCKED`) — [MUST] (v1)
- **Why:** Start here — teaches queue semantics with zero extra infra and is a *great* interview
  deep-dive.

### Redis Streams — [GOOD]
- **Why:** Step up: consumer groups, acks.

### Apache Kafka — [GOOD] (Phase 4)
- **Why:** The distributed-log resume keyword; add when real throughput justifies it.
- **Free alt:** Redpanda (lighter, Kafka-compatible).

### RabbitMQ — [OPT]
- **Why:** Classic AMQP broker alternative.

---

## Testing

### JUnit 5 + AssertJ — [MUST]
- **Why:** Java testing baseline.

### Testcontainers — [MUST]
- **Why:** Spin real Postgres/Redis/Docker in tests → realistic integration tests; very well-regarded,
  strong resume signal.

### Mockito — [MUST]
- **Why:** Mocking — use sparingly (mock collaborators, not the subject under test).

### WireMock — [GOOD]
- **Why:** Mock the LLM HTTP API for deterministic tests of AI paths.

### k6 / Gatling — [OPT] (Phase 4)
- **Why:** Load testing for the scaling phase.

---

## Code Quality

### Spotless (google-java-format) — [MUST]
- **Why:** Auto-formatting in CI = hygiene.

### Checkstyle / PMD / SpotBugs — [GOOD]
- **Why:** Static analysis — ironic + thematic for a code-analysis tool.

### SonarQube (community, local) — [GOOD]
- **Why:** Quality gates, coverage, smells; strong keyword, free in Docker.

### Trivy — [GOOD]
- **Why:** Container/dependency vulnerability scanning in CI = security signal.

---

## Monitoring / Alerting

### Prometheus Alertmanager — [GOOD]
- **Why:** Alert on FORGE's own SLOs (job failure rate, queue depth).

### Grafana dashboards + alerts — [MUST]
- **Why:** The visible, screenshot-able observability artifact.

### Uptime Kuma (self-host) — [OPT]
- **Why:** Liveness monitoring.

---

## The spine (install these first)

Java 21 · Spring Boot 3 · PostgreSQL 16 (+pgvector) · Redis · Docker/Compose · GitHub Actions ·
OpenTelemetry + Prometheus + Grafana + Jaeger · Ollama (+Spring AI) · JavaParser · Testcontainers ·
LocalStack→AWS.

Everything else is staged in by phase — do **not** install Kafka/Kubernetes/Qdrant on day one.
