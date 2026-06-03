# FORGE — Roadmap & Execution Guide

> **FORGE** is an *autonomous backend engineering platform*: it understands Java repositories,
> builds architecture maps, plans engineering tasks, generates code changes, runs them in isolated
> sandboxes, runs tests, analyzes failures, retries, remembers past runs, and observes itself.

This directory is the complete, self-contained guide for **two engineers** to build FORGE as a
flagship personal project — to (a) get much stronger at backend / system-design / distributed
systems / DevOps / LLM engineering, and (b) land an MNC software-engineer role fast.

---

## The one principle that governs everything

> **Build deterministically first. AI enhances workflows; it does not replace architecture.**

FORGE must work end-to-end with the LLM **stubbed out**. Every "agent" capability is a thin,
swappable LLM layer over a boring, reliable, observable deterministic pipeline. This is both the
correct engineering choice *and* the strongest interview story: *"I built a reliable system and made
it intelligent"* beats *"I wrapped an LLM in a loop."*

---

## Locked decisions (the constraints this plan optimizes for)

| Decision | Choice | Why |
|---|---|---|
| Cloud | **AWS** (emulated locally via **LocalStack** for $0) | Highest market/resume value; learn the APIs free, deploy for real only to capture the bullet |
| LLM | **Local-first** (Ollama: Qwen2.5-Coder / DeepSeek-Coder) + tiny **~$5–20/mo** paid fallback (Gemini Flash / Claude Haiku) | Teaches self-hosting *and* production API discipline; near-zero cost |
| Timeline | **Phases 1–3 front-loaded.** Demo-able ~Week 8, full MVP ~Week 14. Scale after. | You start applying mid-build, not after |
| Resume focus | **Balanced** — solid README/ADRs + STAR stories, no doc-overhead drag | Visible progress without bureaucracy |
| Team | 2 devs, **equal ownership**, rotating lead, both do design+build+review+test+deploy+AI | No Frontend/Backend silo |

---

## How to use these docs (reading order)

**First sitting (both devs, ~1 hr):**
1. This `README.md` — vision, principle, decisions.
2. `03-phases-overview.md` — the 4 phases at a glance and what each proves.
3. `02-architecture.md` — what you're building and how it evolves.

**Before you start coding:**
4. `01-tech-stack.md` — what to install and why (Must/Good/Optional).
5. `07-cost-plan.md` — keep it at ~$0.
6. `04-daily-cadence.md` — pick your 2hr / 3hr / weekend rhythm.

**Each week, as you execute:**
7. The relevant phase file (`phase-0` → `phase-3`) — detailed step-by-step how-to + your Dev A/B
   split + who leads that week.
8. `05-learning-roadmap.md` — do the "prereq learning" listed at the top of each week *first*.
9. Update `OWNERSHIP.md` at the end of each week.

**While interviewing / updating your resume:**
10. `06-interview-resume.md` — per-phase bullets, STAR stories, MNC topics demonstrated.

**After the MVP:**
11. `08-scaling-path.md` — how FORGE evolves into a distributed, multi-agent, multi-tenant system.

---

## File index

| File | What's in it |
|---|---|
| `README.md` | This index: vision, principle, decisions, reading order, glossary |
| `01-tech-stack.md` | Every technology, classified Must/Good/Optional, with why + free alternatives |
| `02-architecture.md` | System / component / data-flow / deployment diagrams + phase-by-phase evolution |
| `03-phases-overview.md` | The 4 phases: goal, features, deliverables, learning, resume value |
| `phase-0-foundations.md` | **Week 1** — detailed how-to + Dev A/B split |
| `phase-1-repo-understanding.md` | **Weeks 2–4** — detailed how-to + Dev A/B split + rotating lead |
| `phase-2-execution-pipeline.md` | **Weeks 5–8** — detailed how-to + Dev A/B split + rotating lead |
| `phase-3-ai-augmentation.md` | **Weeks 9–14** — detailed how-to + Dev A/B split + rotating lead |
| `04-daily-cadence.md` | 2hr / 3hr / weekend repeatable schedules |
| `05-learning-roadmap.md` | 26 topics: why / free video / docs / practice / how it applies in FORGE |
| `06-interview-resume.md` | Per-phase resume bullets, STAR stories, MNC topics |
| `07-cost-plan.md` | Free infra, local setup, OSS LLMs, when to use cloud, $/mo per phase |
| `08-scaling-path.md` | Post-MVP evolution: gRPC → k8s → workflow engine → multi-repo → multi-agent → SaaS |
| `OWNERSHIP.md` | Living rotation tracker — who led/reviewed each week |
| `adr/` | Architecture Decision Records. `0001-build-tool-selection.md` is pre-filled (Maven) + serves as the template for ADR-0002…0006 |

---

## Milestones at a glance

| Milestone | Week | What you can say |
|---|---|---|
| Foundations | 1 | "CI/CD + containerized dev from day one" |
| **Phase 1 done** | 4 | "Static-analysis engine that maps Java repo architecture via AST" |
| **Phase 2 done** | 8 | "Concurrent, observable, sandboxed execution pipeline" — *start applying* |
| **Phase 3 / MVP done** | 14 | "Grounded LLM agent (RAG + memory + evals) over a deterministic core" |
| Scaling | 15+ | "And here's how I'd take it to k8s / multi-agent / SaaS" |

---

## Glossary

- **AST** — Abstract Syntax Tree; the parsed structure of source code (via JavaParser).
- **Repo map / architecture map** — packages, classes, methods, call graph, dependencies extracted
  from a repo.
- **Job** — one unit of work in FORGE (analyze a repo, or run a task), processed asynchronously.
- **State machine** — the deterministic lifecycle a Job moves through (CREATED → … → SUCCEEDED/FAILED).
- **Sandbox** — an isolated Docker container where untrusted generated code is built and tested.
- **Codemod** — a deterministic, AST-based code transformation (vs. an LLM-generated patch).
- **RAG** — Retrieval-Augmented Generation; fetch relevant code context (via vector search) to
  ground the LLM.
- **pgvector** — a Postgres extension giving you vector similarity search with no extra database.
- **Eval harness** — a fixed set of tasks scored for success-rate/latency/cost to measure whether a
  prompt/model change actually helped.
- **ADR** — Architecture Decision Record; a short doc capturing one decision + its tradeoffs. Each
  ADR is also a ready-made "tell me about a hard technical decision" interview story.
- **Lead / Non-lead** — weekly rotating roles. Lead owns the hardest component; non-lead owns the
  secondary component + drives review + docs. See `OWNERSHIP.md`.

---

## Ground rules for the two of you

1. **No silos.** Do not split into Frontend-dev and Backend-dev. The lead role rotates weekly; both
   touch every part of the system at least once as primary.
2. **Pair-design every week** (30–45 min) before splitting implementation.
3. **Cross-review every PR.** The non-lead reviews the lead's PR and vice-versa.
4. **Protect the weekly demo** over polish. If a week slips, cut polish, keep the demo.
5. **One ADR per real decision.** Cheap to write, gold in interviews.
6. **Keep it free.** Everything runs locally through Phase 3 (see `07-cost-plan.md`).
