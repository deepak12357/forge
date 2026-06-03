# 02 — High-Level Architecture

## 1. System Architecture (target state, end of Phase 3)

```
                            ┌──────────────────────────┐
                            │   React + TS Dashboard    │
                            │ (jobs, runs, logs, map)   │
                            └────────────┬─────────────┘
                                         │ REST/JSON
                                         ▼
┌───────────────────────────────────────────────────────────────────────┐
│                        FORGE API (Spring Boot)                          │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌────────────┐             │
│  │ Job API  │  │ Repo API │  │ Plan API  │  │ Result API │  (controllers)│
│  └────┬─────┘  └────┬─────┘  └─────┬─────┘  └─────┬──────┘             │
│       └─────────────┴──────────────┴──────────────┘                    │
│                          │ enqueues / reads                             │
│            ┌─────────────▼─────────────┐                               │
│            │   Orchestrator / State     │  ← deterministic state machine│
│            │   Machine (job lifecycle)  │     (CREATED→PLANNED→...)      │
│            └─────────────┬─────────────┘                               │
└──────────────────────────┼────────────────────────────────────────────┘
              enqueue       │  poll (SKIP LOCKED) / Redis Streams
                            ▼
        ┌───────────────────────────────────────────────┐
        │              Worker Pool (executors)            │
        │  ┌──────────┐ ┌──────────┐ ┌───────────────┐   │
        │  │ Repo     │ │ Planner  │ │ CodeGen +     │   │
        │  │ Analyzer │ │ (det→AI) │ │ Patch Applier │   │
        │  │(JavaParser)│ └────┬────┘ └──────┬────────┘   │
        │  └────┬─────┘       │             │            │
        │       │      ┌──────▼─────────────▼────────┐   │
        │       │      │   Sandbox Executor          │   │
        │       │      │ (Docker: build, run, test)  │   │
        │       │      └──────────────┬──────────────┘   │
        │       │                     │ failure analysis  │
        │       │              ┌──────▼──────┐ (det→AI)   │
        │       │              │ Retry Loop  │            │
        │       │              └─────────────┘            │
        └───────┼─────────────────────┼──────────────────┘
                │                      │
   ┌────────────▼──────────┐  ┌────────▼───────────┐  ┌──────────────────┐
   │ PostgreSQL (+pgvector)│  │ Redis (queue/lock/ │  │ Ollama (local LLM)│
   │ jobs, plans, results, │  │ cache/rate-limit)  │  │ + paid API fallbk │
   │ repo maps, memory,    │  └────────────────────┘  └──────────────────┘
   │ embeddings            │
   └───────────────────────┘
                │ all components emit
                ▼
   ┌──────────────────────────────────────────────────────────┐
   │ OpenTelemetry → Prometheus (metrics) + Jaeger/Tempo       │
   │ (traces) + Loki (logs) → Grafana (dashboards/alerts)      │
   └──────────────────────────────────────────────────────────┘
```

## 2. Component Diagram (responsibilities)

- **FORGE API (Spring Boot):** REST surface; validates input; persists jobs; **never does heavy work
  inline** — it *enqueues*. Owns the Orchestrator state machine (the brain that advances jobs).
- **Repo Analyzer:** clones/reads a Java repo, parses with **JavaParser** into an AST, builds the
  repository architecture map (packages, classes, methods, call graph, dependencies). *No AI.*
- **Planner:** turns a task into ordered steps. **Deterministic rules first**; AI-augmented later.
- **CodeGen + Patch Applier:** produces a diff/patch and applies it to a working copy. Deterministic
  AST codemods first; LLM-generated patches later, *always* validated by compile+test.
- **Sandbox Executor:** runs build/run/test **inside a Docker container** with no host access,
  resource + time limits. The security heart of FORGE.
- **Failure Analyzer:** parses compiler/test output into structured failures (deterministic parsers),
  classifies them; AI explanation/fix-suggestion layered on top.
- **Retry Loop:** bounded, idempotent retries with backoff; feeds failure context back to the Planner.
- **Memory Store:** records run outcomes + embeddings (pgvector) so future runs recall "this worked /
  this failed."
- **Observability stack:** every component instrumented; **one trace per job** spans all steps.

## 3. Data Flow (one job, happy path)

```
User submits task (repo URL + instruction)
   → API validates, persists Job(state=CREATED), enqueues
   → Worker picks up (SKIP LOCKED), starts ROOT SPAN (trace_id = job_id)
   → Repo Analyzer: clone → JavaParser AST → repo map persisted   [state=ANALYZED]
   → Planner: task + repo map → ordered Steps persisted           [state=PLANNED]
   → CodeGen: per step → patch generated + applied to workdir      [state=PATCHED]
   → Sandbox Executor: docker build + test in container            [state=EXECUTED]
   → Result Collector: parse test results
        ├─ PASS → persist Result, embed outcome to memory          [state=SUCCEEDED]
        └─ FAIL → Failure Analyzer classifies
                   → Retry Loop (n<max): feed failure to Planner   [state=RETRYING]
                   → exhausted → persist failure + memory          [state=FAILED]
   → every step: spans + metrics + structured logs → OTel pipeline
```

## 4. Deployment Diagram (evolves)

```
Phase 1–3 (LOCAL, $0):                  Phase 4+ (cloud, time-boxed):
┌─────────────────────────┐            ┌────────────────────────────────┐
│  Developer laptop        │            │  AWS (ECS Fargate or EKS/k3s)  │
│  docker-compose up:      │            │  ┌──────────┐  ┌────────────┐  │
│   - forge-api            │            │  │ forge-api│  │ workers x N │  │
│   - worker(s)            │  ───────►  │  │ (svc)    │  │ (HPA)      │  │
│   - postgres+pgvector    │            │  └──────────┘  └────────────┘  │
│   - redis                │            │  RDS Postgres · ElastiCache    │
│   - ollama               │            │  S3 (artifacts) · SQS (queue)  │
│   - prometheus/grafana   │            │  Managed Grafana / self-host   │
│   - jaeger               │            │  Terraform-provisioned         │
└─────────────────────────┘            └────────────────────────────────┘
```

## 5. Architecture evolution by phase

- **Phase 1:** API + Postgres + Repo Analyzer (JavaParser) only. A monolith Spring Boot app. No
  queue, no AI — synchronous repo-map generation.
- **Phase 2:** Add async **queue + worker** (Postgres `SKIP LOCKED`), the **state machine**, the
  **sandbox executor (Docker)**, and full **observability** (OTel/Prometheus/Grafana). Still
  deterministic. *This is the "real engineer" inflection — concurrency, isolation, observability all
  land here.*
- **Phase 3:** Introduce **AI** — Ollama + Spring AI for the Planner / CodeGen / Failure-Analyzer,
  **RAG over the repo map (pgvector)**, and the **memory store**. AI is *swappable and stub-able*.
- **Phase 4+:** Split into services (API ↔ executor over gRPC), **Kubernetes**, a distributed queue
  (Kafka/SQS), multi-repo, multi-agent. (See `08-scaling-path.md`.)

## 6. Module layout (single Maven project, package-by-feature)

```
com.forge
├── api            // controllers, DTOs, exception handlers
├── job            // Job entity, state machine, queue, worker pool
├── repo           // RepoAnalyzer, JavaParser walk, repo-map entities
├── plan           // Planner (interface) + rule-based + AI impls
├── codegen        // Codemod (AST) + AI patch gen + patch applier
├── sandbox        // Docker executor, resource limits, result parser
├── failure        // failure parsing + classification + AI analysis
├── memory         // outcome store + embeddings (pgvector)
├── ai             // LlmClient interface + Ollama/stub/paid impls, RAG retriever
├── eval           // eval harness (Phase 3)
└── observability  // OTel config, metrics, structured logging
```

> Start as a **modular monolith** (one deployable, clean package boundaries). The boundaries above
> are exactly where you'll later cut services in Phase 4 — design them as if they were already
> separate (no cross-feature reach-ins; talk through interfaces).
