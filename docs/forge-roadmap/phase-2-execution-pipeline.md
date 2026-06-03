# Phase 2 — Deterministic Execution Pipeline + Observability (Weeks 5–8)

**Goal:** submit a task → an **async pipeline** plans it (rule-based) → applies a **deterministic
codemod** → **runs build+tests in a Docker sandbox** → reports pass/fail → **bounded retry**. Fully
**observable** (traces, metrics, dashboards). Still no AI.

This is the most important phase for interviews: concurrency, isolation, reliability, and
observability all land here. Protect its demo above all.

> Standard week template: Objective · Prereq learning · Pair-design · How it's done · Work split ·
> Definition of Done · Demo · ADR.

---

## Week 5 — Async job queue + worker pool
- **Lead: Dev A** · **Non-lead: Dev B**
- **Objective:** move work off the request thread into a durable, concurrent job pipeline.
- **Prereq learning:** Concurrency + ExecutorService (Defog Tech) · Postgres `SKIP LOCKED` (PG docs) ·
  idempotency basics.

### Pair-design session (both)
- Agree the **Job state enum:** `CREATED → QUEUED → ANALYZING → PLANNING → PATCHING → EXECUTING →
  (SUCCEEDED | RETRYING | FAILED)`.
- Decide **queue mechanism:** Postgres `SELECT … FOR UPDATE SKIP LOCKED` for v1 (no extra infra).
- Decide **worker concurrency:** fixed pool size N (config); poll interval + empty-queue backoff.
- Define the **idempotency rule:** claiming a job (read + status flip) happens in **one transaction**
  so two workers never grab the same row.

### How it's done — step by step
1. **`Job` entity** (id, type, payload JSONB, status enum, attempts, created_at, claimed_at,
   worker_id) + Flyway `V5`.
2. **Claim query (native):** `SELECT * FROM job WHERE status='QUEUED' ORDER BY created_at FOR UPDATE
   SKIP LOCKED LIMIT 1`, then `UPDATE` status → `ANALYZING`, set claimed_at/worker_id — all in ONE
   `@Transactional` method.
3. **`Worker` loop:** claim → dispatch by `job.type` to a handler → on success advance state, on
   exception increment `attempts` + set `RETRYING`/`FAILED`. Sleep + backoff when the queue is empty.
4. **Pool startup:** a fixed set of workers via `Executors.newVirtualThreadPerTaskExecutor()` capped
   by a semaphore (discuss vs a fixed platform pool — note the tradeoff).
5. **API:** `POST /jobs` inserts a `QUEUED` row, returns `202` + job id; `GET /jobs/{id}` returns live
   state + attempts.
6. **Graceful shutdown:** stop claiming on SIGTERM, let in-flight jobs finish (Spring lifecycle hook
   / `@PreDestroy`).

### Work split
- **Dev A (lead):** the claim query + transactional claim method + worker loop + concurrency safety
  (the core).
- **Dev B:** `Job` entity + Flyway V5 + the Job REST API (POST/GET) + DTOs + validation.
- **Shared / handoff:** pair on the integration test (two+ workers, 100 jobs, assert each processed
  exactly once). Dev B reviews Dev A's PR focusing on the transaction boundary; Dev A reviews Dev B's
  API PR.

### Definition of Done
- [ ] 100 enqueued jobs each processed **exactly once** by a 4-worker pool (Testcontainers test).
- [ ] No double-claim under concurrency (assert via a unique processed-count).
- [ ] States advance and are observable via `GET /jobs/{id}`.
- [ ] Graceful shutdown drains in-flight jobs.

### Demo deliverable
Enqueue a batch; show states advancing concurrently in logs/dashboard.

### ADR
- **ADR-0003 — Postgres `SKIP LOCKED` queue vs Redis/Kafka for v1** (tradeoffs: infra simplicity,
  delivery semantics, scaling limits, when you'd switch).

---

## Week 6 — Docker sandbox executor
- **Lead: Dev B** · **Non-lead: Dev A**
- **Objective:** run a repo's build + tests **inside an isolated, resource-limited container** and
  capture the verdict.
- **Prereq learning:** Docker isolation flags (`--network=none`, `--memory`, `--cpus`, `--pids-limit`,
  read-only mounts, `--user`) · parsing Gradle/Maven test output.

### Pair-design session (both)
- Decide **how FORGE drives Docker:** shell out to the Docker CLI vs a Java Docker client library —
  pick the simpler-to-test one.
- Define the **sandbox contract:** input = a workdir + build command; output = exit code + captured
  stdout/stderr + parsed test results.
- Define **limits** (memory, CPU, pids, wall-clock timeout, no network) and the **threat model**
  (untrusted code must not touch the host or network).

### How it's done — step by step
1. **Build image:** a minimal JDK+build image (e.g. a Temurin + Maven base) used to run repos. (Note:
   the *target repos* you analyze may use Maven or Gradle — the result parser below handles both.)
2. **`SandboxExecutor`:** `docker run --rm --network=none --memory=… --cpus=… --pids-limit=…
   --read-only` with the repo workdir mounted (read-only where possible; a writable tmp for build
   output), running the build/test command. Enforce a wall-clock timeout (kill the container on
   overrun).
3. **Capture** exit code + stdout/stderr to a bounded buffer (cap size to avoid OOM on chatty
   builds).
4. **Result parser:** parse the test report (Gradle/Maven XML in `build/test-results` or surefire) →
   structured `{ passed, failed, errors, durations }`.
5. **Wire into the worker:** the `EXECUTING` state calls the sandbox; on pass → `SUCCEEDED`, on
   fail → hand to failure handling (Week 7).
6. **Safety checks:** verify no host network reachable from inside; verify the container can't write
   outside its sandbox.

### Work split
- **Dev B (lead):** the `SandboxExecutor` (docker invocation, limits, timeout, capture) — the security
  heart.
- **Dev A:** the test-result parser + the build image + wiring the executor into the worker state
  flow.
- **Shared / handoff:** pair on the threat-model review (try to break out of the sandbox). Dev A
  reviews the executor PR (focus: isolation flags); Dev B reviews the parser PR.

### Definition of Done
- [ ] A passing fixture repo → `SUCCEEDED`; a failing fixture repo → correct failure verdict.
- [ ] Container has **no network** and cannot write to the host (verified).
- [ ] Wall-clock timeout kills a runaway build.
- [ ] Test results parsed into structured pass/fail counts.

### Demo deliverable
Run a passing repo and a deliberately-failing repo through the sandbox; show correct verdicts +
parsed results.

### ADR
- **ADR-0004 — Sandbox isolation model** (limits, threat model, what's mounted, why this is safe to
  run untrusted code).

---

## Week 7 — Deterministic codemod + planner + bounded retry
- **Lead: Dev A** · **Non-lead: Dev B**
- **Objective:** generate + apply a patch deterministically, run it in the sandbox, and retry on
  failure with bounds.
- **Prereq learning:** JavaParser AST transforms (modifying + printing a `CompilationUnit`) ·
  retry/backoff patterns · idempotency under retry.

### Pair-design session (both)
- Pick a concrete **deterministic task** to support first (e.g. "add a null-check / `@Validated` to a
  target method", or "add a missing `@Override`") — small, verifiable, AST-doable.
- Design the **Planner interface** (`Plan plan(Task, RepoMap)`) so the rule-based impl now and the AI
  impl later are swappable behind it. Same for `CodeGenerator` and `FailureAnalyzer`.
- Define the **retry policy:** max attempts, backoff, what context feeds the next attempt, when to
  give up.

### How it's done — step by step
1. **`Planner` (rule-based):** map the task type → an ordered list of `Step`s (e.g. "locate method
   X", "inject null-check"). Persist the plan.
2. **`Codemod` (AST):** parse the target file, modify the `CompilationUnit` (e.g. add an `if (x ==
   null) throw …`), pretty-print, write back to the workdir.
3. **Apply → execute:** run the patched workdir through the Week-6 sandbox.
4. **`FailureAnalyzer` (deterministic):** parse compiler/test errors into a structured classification
   (compile error / test failure / timeout).
5. **Retry loop:** on failure and `attempts < max`, set `RETRYING`, optionally adjust the plan, and
   re-run with backoff; on exhaustion → `FAILED` with the recorded reason.
6. **Persist results** + per-attempt history (you'll visualize this and later feed it to AI/memory).

### Work split
- **Dev A (lead):** the AST codemod + patch applier (the core transform).
- **Dev B:** the rule-based Planner + the retry loop + the deterministic FailureAnalyzer + result
  persistence.
- **Shared / handoff:** agree the Planner/CodeGen/FailureAnalyzer **interfaces** (this contract is
  what Phase 3 plugs AI into). Dev B reviews the codemod PR; Dev A reviews the planner/retry PR.

### Definition of Done
- [ ] An end-to-end deterministic task: task → plan → patch → sandbox test → `SUCCEEDED`.
- [ ] A task that fails first runs the bounded retry, then `FAILED` with a recorded reason.
- [ ] Planner / CodeGenerator / FailureAnalyzer are **interfaces** with rule-based impls (AI-ready).
- [ ] Per-attempt history persisted.

### Demo deliverable
Run a deterministic "fix" task end-to-end through the full pipeline (plan → patch → sandbox → pass).

---

## Week 8 — Observability + Phase 2 polish
- **Lead: Dev B** · **Non-lead: Dev A**
- **Objective:** full tracing/metrics/dashboards so you can *see* a job flow through the pipeline;
  record the Phase 2 demo.
- **Prereq learning:** OpenTelemetry Java (opentelemetry.io) · Prometheus + Grafana (TechWorld with
  Nana) · structured logging / correlation IDs · SLO basics.

### Pair-design session (both)
- Decide the **trace model:** one **root span per job** (trace_id = job_id) with child spans per
  pipeline step (analyze / plan / codegen / execute / retry).
- Decide the **metrics:** queue depth, jobs in-flight, job duration histogram, success/failure
  counters, retry count, sandbox duration.
- Decide the **dashboard layout** + at least one **alert** (e.g. success-rate < threshold, queue depth
  too high).

### How it's done — step by step
1. **OTel SDK:** add the OpenTelemetry Java agent/SDK; export traces to **Jaeger** (or Tempo) and
   metrics to **Prometheus**. Add jaeger + prometheus + grafana to `docker-compose.yml`.
2. **Instrument the worker:** open a root span when a job is claimed; child spans around each step;
   record attributes (job type, attempts, verdict). Propagate context across the pipeline.
3. **Metrics:** register Micrometer/OTel meters for the metrics agreed above; expose `/actuator/
   prometheus`.
4. **Structured logging:** Logback JSON encoder; put `job_id` in the MDC so every log line for a job
   is correlated; surface the trace id.
5. **Grafana:** build a dashboard (queue depth, throughput, p50/p95 job latency, success rate,
   sandbox duration); add one Alertmanager/Grafana alert rule.
6. **Polish:** link the trace from `GET /jobs/{id}` (a Jaeger URL); README observability section;
   ADRs finalized; record the Phase 2 demo.

### Work split
- **Dev B (lead):** OTel tracing (spans, propagation) + Jaeger wiring (the instrumentation core).
- **Dev A:** Prometheus metrics + the Grafana dashboard + the alert + structured logging/correlation.
- **Shared / handoff:** record the Phase 2 demo together. Dev A reviews the tracing PR; Dev B reviews
  the metrics/dashboard PR.

### Definition of Done
- [ ] Click/look up any job → see its **full distributed trace** across all steps in Jaeger.
- [ ] Grafana shows queue depth, throughput, latency, and success rate; one alert fires correctly in
  a test.
- [ ] Every log line for a job carries its `job_id` (correlation works).
- [ ] **Phase 2 demo video recorded.**
- [ ] `OWNERSHIP.md` rows for weeks 5–8 filled in.

### Demo deliverable
**Phase 2 demo:** submit a task → watch concurrent sandboxed execution → open the job's trace
timeline in Jaeger → show the Grafana SLO dashboard. *Résumé bullets #2–3 ready; you can start
applying now (see `06-interview-resume.md`).*

---

## End-of-phase checklist
- [ ] Async queue + worker pool with exactly-once job processing.
- [ ] Docker sandbox running untrusted code under strict limits.
- [ ] Deterministic plan→codemod→execute→retry pipeline behind swappable interfaces.
- [ ] Full observability: trace-per-job, Prometheus metrics, Grafana dashboard + alert.
- [ ] Phase 2 demo recorded; ADR-0003/0004 done.

> Phase 3 plugs LLMs into the Planner/CodeGen/FailureAnalyzer interfaces you built in Week 7 — adding
> intelligence *without* removing the deterministic guarantees.
