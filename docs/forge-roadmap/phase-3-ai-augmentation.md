# Phase 3 — AI Augmentation (Weeks 9–14)

**Goal:** plug LLMs into the **Planner / CodeGenerator / FailureAnalyzer** interfaces you built in
Phase 2 — *behind the same contracts*, so the whole pipeline still passes with `ai.enabled=false`.
Add RAG over the repo map, run-memory, an eval harness, and production cost/rate discipline.

**The non-negotiable rule of this phase:** AI is **additive and swappable**, never load-bearing.
Every LLM output is validated by real compile+test execution (your Phase-2 sandbox). This is your
strongest architecture-judgment interview story — protect it.

> Standard week template: Objective · Prereq learning · Pair-design · How it's done · Work split ·
> Definition of Done · Demo · ADR.

---

## Week 9 — Ollama + swappable LLM abstraction
- **Lead: Dev A** · **Non-lead: Dev B**
- **Objective:** run a local LLM and call it behind an interface, with the deterministic path still
  working when AI is off.
- **Prereq learning:** LLM Fundamentals (Karpathy "Intro to LLMs") · Ollama docs · Spring AI intro ·
  WireMock for mocking HTTP.

### Pair-design session (both)
- Design the **`LlmClient` interface** (`complete(prompt, opts) → text`, plus `embed(text) →
  vector`). Three impls: a **deterministic stub** (for tests + AI-off), an **Ollama** impl, and
  (later) a **paid-API** impl.
- Decide the **feature flag** `ai.enabled` and how the Planner/CodeGen pick stub-vs-rule-vs-AI.
- Decide model(s): which Ollama code model fits your RAM (see `07-cost-plan.md`).

### How it's done — step by step
1. **Run Ollama** locally; pull a code model (e.g. `qwen2.5-coder`). Add `ollama` to
   `docker-compose.yml` (or run natively if GPU/RAM is better that way).
2. **`LlmClient` interface** + **Ollama impl** via Spring AI's Ollama client (chat + embeddings).
3. **Stub impl:** deterministic canned responses for tests (so AI paths are testable without a model).
4. **Feature flag `ai.enabled`:** when false, Planner/CodeGen use the Phase-2 rule-based logic; when
   true, the AI impls. Wire selection in config.
5. **WireMock-based tests** for the (later) HTTP paid path; unit tests use the stub.
6. **Prove parity:** the same Week-7 deterministic task passes with `ai.enabled=false` AND
   `ai.enabled=true` (Ollama).

### Work split
- **Dev A (lead):** `LlmClient` interface + Ollama impl + Spring AI wiring (the integration core).
- **Dev B:** the stub impl + the `ai.enabled` flag plumbing + WireMock test harness.
- **Shared / handoff:** agree the `LlmClient` contract carefully (it's used everywhere downstream).
  Dev B reviews the Ollama PR; Dev A reviews the stub/flag PR.

### Definition of Done
- [ ] FORGE calls a local Ollama model successfully.
- [ ] `ai.enabled=false` → deterministic path still passes the Week-7 task (parity proven).
- [ ] AI paths are unit-testable via the stub (no live model needed in CI).

### Demo deliverable
Same task run twice: AI-off (rules) vs AI-on (Ollama) — both green.

---

## Week 10 — RAG over the repo map (pgvector)
- **Lead: Dev B** · **Non-lead: Dev A**
- **Objective:** retrieve the *relevant* slices of the repo for the LLM instead of dumping whole
  files.
- **Prereq learning:** Vector DBs/pgvector (github.com/pgvector) · RAG (Spring AI RAG docs) ·
  embeddings basics.

### Pair-design session (both)
- Decide **what to embed:** per-class and/or per-method summaries (signature + javadoc + key lines).
- Decide **chunking** strategy and the embedding model (Ollama embeddings or a small local model).
- Design the **retrieval contract:** `retrieve(query, k) → top-k code chunks` feeding prompt
  assembly.

### How it's done — step by step
1. **Enable pgvector** in Postgres (extension + Flyway `V9` adding a `vector` column / embeddings
   table).
2. **Embedding pass:** for each `ClassNode`/`MethodNode`, build a compact text representation, embed it
   via `LlmClient.embed`, store the vector + a FK back to the node.
3. **Retriever:** given a task, embed the query and run a top-k similarity search
   (`ORDER BY embedding <-> :q LIMIT k`); add an index (HNSW/IVFFlat) for speed.
4. **Prompt assembly:** build the Planner/CodeGen prompt from the retrieved chunks (bounded to a token
   budget) instead of the whole repo.
5. **Sanity compare:** measure retrieval quality (does it surface the right class for a task?) vs a
   naive whole-repo dump.

### Work split
- **Dev B (lead):** the embedding pipeline + pgvector schema + index (the retrieval core).
- **Dev A:** the retriever query + prompt assembly + the token-budget bounding.
- **Shared / handoff:** agree the chunk format + retrieval contract. Dev A reviews the embedding PR;
  Dev B reviews the retriever/prompt PR.

### Definition of Done
- [ ] Class/method embeddings stored in pgvector with a similarity index.
- [ ] `retrieve(query, k)` returns relevant chunks for a task.
- [ ] Prompts are built from retrieved context within a token budget (no whole-repo dumps).

### Demo deliverable
Show retrieved context for a task vs the naive full-repo prompt — the relevance difference.

---

## Week 11 — LLM Planner + CodeGen (execution-gated)
- **Lead: Dev A** · **Non-lead: Dev B**
- **Objective:** the LLM generates plans and patches, but **every patch is validated by compile+test**
  before it counts.
- **Prereq learning:** Prompt Engineering (Anthropic guide) · Agent Systems ("Building effective
  agents") · structured output / parsing model responses.

### Pair-design session (both)
- Design the **AI Planner prompt:** task + retrieved context → an ordered, parseable list of steps.
- Design the **AI CodeGen prompt:** step + relevant code → a patch (unified diff or full-file
  replacement). Decide the output format and how you parse/apply it.
- Define the **validation gate:** generated patch → apply → compile → test in the sandbox; reject +
  retry on invalid output. **Never trust unverified LLM output.**

### How it's done — step by step
1. **`AiPlanner implements Planner`:** prompt the LLM (with RAG context) for steps; parse into the
   same `Step` structure the rule-based planner produced (so downstream is unchanged).
2. **`AiCodeGenerator implements CodeGenerator`:** prompt per step for a patch; parse the diff; apply
   to the workdir.
3. **Validation gate:** the patched workdir goes through the Week-6 sandbox (compile + test). If it
   fails to apply/compile, treat as an invalid attempt → feed back / retry (bounded).
4. **Guardrails:** cap patch size, reject patches touching unexpected files, require the patch to be
   parseable before applying.
5. **Keep AI-off parity:** the rule-based impls still exist and still pass — the gate logic is shared.

### Work split
- **Dev A (lead):** `AiCodeGenerator` + the patch parse/apply + the validation gate (the core loop).
- **Dev B:** `AiPlanner` + prompt assembly with RAG context + step parsing + guardrails.
- **Shared / handoff:** iterate prompts together (fast feedback loop). Dev B reviews the CodeGen/gate
  PR; Dev A reviews the planner PR.

### Definition of Done
- [ ] An NL task → AI plan → AI patch → sandbox-validated → `SUCCEEDED` on a real example.
- [ ] Invalid/unparseable/failing patches are rejected and retried (bounded), never merged blindly.
- [ ] `ai.enabled=false` still passes the deterministic task (parity intact).

### Demo deliverable
A natural-language task producing a real, test-passing patch through the gated pipeline.

---

## Week 12 — LLM failure analysis + self-correction
- **Lead: Dev B** · **Non-lead: Dev A**
- **Objective:** close the agent loop — when tests fail, the LLM explains + proposes a fix that feeds
  the next attempt.
- **Prereq learning:** agent loop design (plan→act→observe→retry) · safe iteration caps · feeding
  errors back into prompts.

### Pair-design session (both)
- Design the **failure→fix loop:** structured failure (from the Week-7 deterministic analyzer) + code
  context → LLM explanation + a corrected patch → re-validate.
- Define **hard caps:** max self-correction iterations, time budget, and a clean give-up path
  (`FAILED` with the analysis recorded).
- Decide what context the LLM gets (the error, the relevant code, the previous patch) to avoid
  context bloat.

### How it's done — step by step
1. **`AiFailureAnalyzer implements FailureAnalyzer`:** take the structured failure + the failing code +
   the previous patch; prompt for (a) a diagnosis and (b) a revised patch.
2. **Self-correction loop:** revised patch → apply → sandbox validate; repeat until pass OR the cap is
   hit. Record every attempt (diagnosis, patch, verdict).
3. **Caps + safety:** enforce max iterations + wall-clock; ensure each attempt is independent/
   idempotent (no compounding state).
4. **Wire into the worker:** the `RETRYING` state now routes through the AI analyzer when
   `ai.enabled` (else the Phase-2 deterministic retry).
5. **Observability:** spans + metrics for self-correction (iterations per job, fix-success rate).

### Work split
- **Dev B (lead):** `AiFailureAnalyzer` + the self-correction loop + caps (the loop core).
- **Dev A:** wiring into the worker state machine + attempt-history persistence + the new
  spans/metrics.
- **Shared / handoff:** test on real failing tasks together. Dev A reviews the analyzer/loop PR; Dev B
  reviews the wiring/observability PR.

### Definition of Done
- [ ] A task that fails on the first attempt is **auto-corrected** to passing on a later attempt (real
  example).
- [ ] Hard iteration + time caps enforced; clean `FAILED` path with recorded analysis.
- [ ] Self-correction metrics visible in Grafana.

### Demo deliverable
A task that fails first, then FORGE diagnoses and fixes itself to green — the "wow" moment.

---

## Week 13 — Memory + eval harness
- **Lead: Dev A** · **Non-lead: Dev B**
- **Objective:** learn from past runs, and *measure* whether AI changes actually help.
- **Prereq learning:** Memory Systems (vector memory) · Evaluation Systems (Hamel Husain "evals") ·
  metrics for LLM quality.

### Pair-design session (both)
- Design the **memory model:** persist run outcomes (task, plan, patch, verdict, attempts) + an
  embedding; on a new task, recall similar past runs to seed the plan.
- Design the **eval harness:** a fixed task set with known-good expectations; a runner that executes
  all tasks and scores **success-rate / latency / cost** per model+prompt.
- Decide the eval task set (5–15 representative tasks) and the scoring definition (did tests pass?).

### How it's done — step by step
1. **`MemoryStore`:** Flyway `V13` for an outcomes table + embedding; write an outcome at the end of
   every job.
2. **Recall:** at planning time, embed the task, retrieve top-k similar past outcomes, include
   "previously, X worked / Y failed" in the planner prompt.
3. **`EvalRunner`:** load the fixed task set; run each through the full pipeline with a given
   model/prompt config; record pass/fail + latency + token cost.
4. **Eval report:** produce a table (model × prompt → success-rate, p50 latency, $/task). Re-runnable
   so you can compare changes.
5. **Use it:** run the eval across Ollama vs the paid models (Week 14) to justify config choices with
   data.

### Work split
- **Dev A (lead):** the `EvalRunner` + scoring + report (the measurement core).
- **Dev B:** the `MemoryStore` + recall integration into planning.
- **Shared / handoff:** agree the eval task set together. Dev B reviews the eval PR; Dev A reviews the
  memory PR.

### Definition of Done
- [ ] Run outcomes persisted + embedded; recall surfaces relevant past runs in planning.
- [ ] `EvalRunner` produces a success-rate/latency/cost table over the fixed task set.
- [ ] At least one documented case where memory/prompt change moved the eval number.

### Demo deliverable
The eval table (Ollama vs hosted models) + memory recall influencing a plan.

---

## Week 14 — Cost controls + Phase 3 / MVP polish
- **Lead: Dev B** · **Non-lead: Dev A**
- **Objective:** production AI discipline (cost cap, caching, rate limiting, graceful degradation) +
  the final MVP demo.
- **Prereq learning:** prompt caching · token budgeting · rate limiting (token bucket) · graceful
  degradation patterns.

### Pair-design session (both)
- Design the **paid-API fallback policy:** when to escalate from local to paid (quality threshold /
  task type), and the **hard monthly cost cap**.
- Design **cost/rate controls:** per-job token budget, prompt caching of stable prefixes, a Redis
  token-bucket rate limiter, and degradation back to local when the cap/limit is hit.
- Decide the final **MVP demo script** and the README/architecture refresh.

### How it's done — step by step
1. **Paid `LlmClient` impl** (Gemini Flash / Claude Haiku) behind the same interface; selection policy
   in config.
2. **Cost guard:** track `llm_cost_usd_total` + `llm_tokens_total` (Prometheus); refuse paid calls
   past the monthly cap → log + **degrade to Ollama**.
3. **Prompt caching:** cache stable prompt prefixes (system + repo context) to cut paid token cost;
   verify cache hits.
4. **Rate limiting:** a Redis token-bucket in front of the paid API to prevent burst overspend.
5. **Graceful degradation:** on cap/limit/provider-error, fall back to the local model without
   failing the job.
6. **Polish:** README + architecture refresh; **ADR-0005** (AI architecture / swappable `LlmClient`),
   **ADR-0006** (eval methodology); record the **MVP demo**.

### Work split
- **Dev B (lead):** paid impl + cost guard + caching + degradation (the cost-control core).
- **Dev A:** the rate limiter (Redis token bucket) + cost metrics/alert + the README/ADR refresh.
- **Shared / handoff:** record the MVP demo together. Dev A reviews the cost-guard PR; Dev B reviews
  the rate-limiter/docs PR.

### Definition of Done
- [ ] Paid fallback works AND is capped — overspend is refused and degrades to local cleanly.
- [ ] Prompt caching demonstrably reduces paid token cost (cache hits observed).
- [ ] Rate limiting prevents burst overspend.
- [ ] `llm_cost_usd_total` exposed in Prometheus with an alert on the budget.
- [ ] **MVP demo video recorded.**
- [ ] `OWNERSHIP.md` rows for weeks 9–14 + the ADR log filled in.

### Demo deliverable
**MVP demo:** NL task → RAG-grounded plan → AI patch → sandbox validation → self-correction on
failure → memory recall + eval table + the job's distributed trace. Toggle `ai.enabled=false` live to
show the deterministic core still works. *Full flagship project complete — all résumé bullets ready
(see `06-interview-resume.md`).*

### ADRs
- **ADR-0005 — AI architecture** (swappable `LlmClient`, why AI is additive not load-bearing,
  execution-gated outputs).
- **ADR-0006 — Eval methodology** (task set, scoring, how it drives model/prompt decisions).

---

## End-of-phase checklist (MVP complete)
- [ ] LLM Planner/CodeGen/FailureAnalyzer behind the Phase-2 interfaces; `ai.enabled=false` parity
  proven.
- [ ] RAG over the repo map via pgvector; memory recall from past runs.
- [ ] Self-correction loop with hard caps; every AI output execution-validated.
- [ ] Eval harness scoring success-rate/latency/cost across models.
- [ ] Production cost/rate discipline (cap, caching, rate limit, degradation).
- [ ] MVP demo recorded; ADR-0005/0006 done.

> Scaling (Phase 4+) is now optional and interview-driven — see `08-scaling-path.md`. You have a
> complete, credible, differentiated flagship project. Go interview.
