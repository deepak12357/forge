# 05 — Learning Roadmap

For every concept: **why it matters · free video · free docs · practice task · how it applies in
FORGE.** Channels named are stable, free, high-quality — search the exact title (links rot, channels
don't). Do the relevant topic *before* the week that needs it (each week in the phase files lists its
"Prereq learning").

> Recommended free YouTube channels referenced below: **Java Brains**, **Defog Tech**, **Dan Vega**,
> **Amigoscode**, **TechWorld with Nana**, **Hussein Nasser**, **Gaurav Sen**, **ByteByteGo**,
> **Andrej Karpathy**, **3Blue1Brown**, **freeCodeCamp**, **Thorben Janssen**, **Nicolai Parlog
> (nipafx / Inside Java)**.

---

## Java Advanced (records, sealed, pattern matching, virtual threads/Loom)
- **Why:** Modern-Java fluency is a top interview discriminator for experienced Java devs.
- **Video:** Nicolai Parlog (Inside Java); Java Brains "Java 21" topics.
- **Docs:** dev.java; OpenJDK JEPs — records (395), sealed (409), virtual threads (444).
- **Practice:** rewrite a small class hierarchy with sealed types + a pattern-matching switch.
- **In FORGE:** records for DTOs/AST nodes; virtual threads for the worker pool.

## Spring Boot 3
- **Why:** Your core framework; FORGE's API + orchestration.
- **Video:** Java Brains; Dan Vega; Amigoscode "Spring Boot".
- **Docs:** docs.spring.io/spring-boot; spring.io/guides.
- **Practice:** build a CRUD service with validation + a global exception handler.
- **In FORGE:** controllers, services, config, Spring AI.

## JPA / Hibernate
- **Why:** ORM mastery (lazy/eager, N+1, transactions) is heavily interviewed.
- **Video:** Java Brains "JPA"; Thorben Janssen (Hibernate).
- **Docs:** Spring Data JPA reference; Hibernate User Guide.
- **Practice:** model a graph (nodes+edges), tune fetch strategy, deliberately cause then fix an N+1.
- **In FORGE:** repo-map persistence, job state.

## PostgreSQL
- **Why:** The default SQL DB; advanced features power FORGE's queue + RAG.
- **Video:** Hussein Nasser (DB internals/indexing/locking).
- **Docs:** postgresql.org/docs — indexes, MVCC, `SELECT … FOR UPDATE SKIP LOCKED`, advisory locks,
  JSONB.
- **Practice:** implement a job queue with `FOR UPDATE SKIP LOCKED`.
- **In FORGE:** queue, state, pgvector RAG.

## Docker
- **Why:** Container baseline + the sandbox security story.
- **Video:** TechWorld with Nana "Docker Tutorial".
- **Docs:** docs.docker.com; Dockerfile best practices.
- **Practice:** containerize the app; run a build in a `--network=none`, resource-limited container.
- **In FORGE:** sandbox executor, local infra.

## Kubernetes
- **Why:** The scaling/DevOps resume keyword (Phase 4).
- **Video:** TechWorld with Nana "Kubernetes Full Course" (the canonical free one).
- **Docs:** kubernetes.io/docs; k3s.io; kind.sigs.k8s.io.
- **Practice:** deploy FORGE to local k3s/kind; add an HPA.
- **In FORGE:** Phase 4 deployment, worker autoscaling.

## GitHub Actions
- **Why:** CI/CD hygiene from day 1.
- **Video:** TechWorld with Nana "GitHub Actions"; freeCodeCamp CI/CD.
- **Docs:** docs.github.com/actions.
- **Practice:** matrix build + dependency caching + required checks + push image to GHCR.
- **In FORGE:** the project's own CI.

## System Design
- **Why:** *The* interview gate for mid/senior roles.
- **Video:** Gaurav Sen; ByteByteGo; Exponent "System Design Interview".
- **Docs:** github.com/donnemartin/system-design-primer; ByteByteGo newsletter (free posts).
- **Practice:** write FORGE's own design doc — you'll literally interview on this.
- **In FORGE:** every architectural decision → an ADR.

## Concurrency (threads, memory model, synchronization)
- **Why:** Worker-pool correctness; classic interview territory.
- **Video:** Defog Tech "Java Concurrency" (excellent, free); Java Brains.
- **Docs:** jenkov.com/java-concurrency; `java.util.concurrent` Javadoc.
- **Practice:** build a bounded producer/consumer with backpressure.
- **In FORGE:** worker pool, queue consumers.

## Executor Framework
- **Why:** The right way to run background work in Java.
- **Video:** Defog Tech "ExecutorService".
- **Docs:** `ExecutorService` / `ThreadPoolExecutor` Javadoc; Baeldung guides.
- **Practice:** tune a thread pool; observe queue saturation behavior.
- **In FORGE:** worker scheduling.

## CompletableFuture
- **Why:** Async composition; common interview ask.
- **Video:** Defog Tech "CompletableFuture" (the standard recommendation).
- **Docs:** `CompletableFuture` Javadoc; Baeldung guide.
- **Practice:** compose async steps with timeouts + fallbacks.
- **In FORGE:** pipeline step orchestration.

## Distributed Systems
- **Why:** Senior-level differentiator; informs queue/retry/idempotency design.
- **Video:** Martin Kleppmann lectures (free, Cambridge); Hussein Nasser; ByteByteGo.
- **Docs:** *Designing Data-Intensive Applications* (book — borrow/buy); Jepsen posts (free).
- **Practice:** make job processing idempotent + exactly-once-ish under retries.
- **In FORGE:** queue semantics, retry, eventual consistency of memory.

## Observability + OpenTelemetry
- **Why:** Mandatory for FORGE; hot resume topic.
- **Video:** Grafana YouTube; OpenTelemetry official talks; TechWorld with Nana
  "Prometheus/Grafana".
- **Docs:** opentelemetry.io/docs (Java); prometheus.io/docs; grafana.com/docs.
- **Practice:** instrument one job end-to-end; build a Grafana dashboard + one alert.
- **In FORGE:** trace-per-job, metrics, dashboards (Phase 2).

## Vector Databases
- **Why:** RAG foundation; current AI-infra keyword.
- **Video:** ByteByteGo "Vector DB" explainer; Qdrant channel.
- **Docs:** github.com/pgvector/pgvector; qdrant.tech/documentation.
- **Practice:** embed + top-k retrieve over the repo map.
- **In FORGE:** RAG + memory (Phase 3).

## RAG (Retrieval-Augmented Generation)
- **Why:** The dominant pattern for grounding LLMs in your data.
- **Video:** freeCodeCamp RAG courses; Spring AI / LangChain talks.
- **Docs:** Spring AI RAG docs; LangChain4j docs.
- **Practice:** compare naive-whole-repo-dump vs RAG retrieval quality on a FORGE task.
- **In FORGE:** Planner/CodeGen context.

## Prompt Engineering
- **Why:** Cheap, high-leverage quality gains.
- **Video:** Anthropic prompting talks; freeCodeCamp prompt-engineering.
- **Docs:** Anthropic prompt-engineering guide (docs.anthropic.com); OpenAI cookbook (free).
- **Practice:** iterate a code-gen prompt; measure via the eval harness.
- **In FORGE:** all LLM call sites; tracked in evals.

## LLM Fundamentals
- **Why:** Understand tokens, context windows, sampling, limits → better system decisions.
- **Video:** Andrej Karpathy "Intro to LLMs" + "Let's build GPT" (canonical, free); 3Blue1Brown
  transformers series.
- **Docs:** Hugging Face course (free); Ollama docs.
- **Practice:** run/quantize a local model; measure latency vs context size.
- **In FORGE:** model selection, cost/latency tradeoffs.

## Agent Systems / AI Agents
- **Why:** The "autonomous" in FORGE; in-demand and over-hyped — your *grounded* version stands out.
- **Video:** Anthropic "Building effective agents"; LangChain agent videos.
- **Docs:** Anthropic "Building effective agents" (engineering blog); LangChain4j agents; Spring AI
  tool-calling.
- **Practice:** build the plan→act→observe→retry loop with hard iteration caps.
- **In FORGE:** the Phase-3 agent loop (always execution-validated).

## Code Analysis / AST Parsing / JavaParser / Static Analysis
- **Why:** FORGE's deterministic core; a rare, impressive portfolio skill.
- **Video:** "JavaParser tutorial" talks; conference talks on AST transforms.
- **Docs:** javaparser.org — the book *"JavaParser: Visited"* is free online; SpotBugs/PMD docs.
- **Practice:** write a visitor that finds all methods missing a null-check; then a codemod that adds
  one.
- **In FORGE:** Repo Analyzer + Codemod (Phases 1–2).

## Memory Systems
- **Why:** Turns a stateless agent into a learning one; a differentiator.
- **Video:** LangChain "memory" talks; vector-memory explainers.
- **Docs:** Spring AI / LangChain4j memory abstractions; pgvector.
- **Practice:** store outcomes + recall similar past runs to seed a plan.
- **In FORGE:** Memory Store (Phase 3).

## Evaluation Systems
- **Why:** "How do you know your AI got better?" — separates engineers from prompt-tinkerers.
- **Video:** Anthropic/OpenAI eval talks; Hamel Husain "evals" writing (free).
- **Docs:** evals concepts (Anthropic/OpenAI cookbooks).
- **Practice:** fixed task set → success-rate/latency/cost table across models.
- **In FORGE:** Eval Harness (Phase 3, Week 13).

---

## Suggested learning sequence (mapped to phases)

| Phase | Learn before/during |
|---|---|
| 0 (Wk1) | Spring Boot 3, Maven (POM/lifecycle/plugins), GitHub Actions, Docker, Testcontainers |
| 1 (Wk2–4) | JavaParser/AST, JPA relationships, PostgreSQL basics, System Design intro |
| 2 (Wk5–8) | Concurrency, ExecutorService, CompletableFuture, PostgreSQL `SKIP LOCKED`, Docker isolation, OpenTelemetry, Prometheus/Grafana, Distributed Systems basics |
| 3 (Wk9–14) | LLM Fundamentals, Prompt Engineering, RAG, Vector DBs/pgvector, Agent Systems, Memory Systems, Evaluation Systems |
| 4+ | Kubernetes, gRPC, Kafka, advanced Distributed Systems |
