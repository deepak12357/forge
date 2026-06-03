# Phase 1 — Deterministic Repo Understanding (Weeks 2–4)

**Goal:** submit a Java repo URL → FORGE returns a structured **architecture map** (packages,
classes, methods, call graph, dependency edges). **100% deterministic — no AI.** This is FORGE's
foundation and already a strong portfolio piece.

> Standard week template: Objective · Prereq learning · Pair-design · How it's done · Work split ·
> Definition of Done · Demo · ADR.

---

## Week 2 — Repo ingestion + AST parse
- **Lead: Dev B** · **Non-lead: Dev A**
- **Objective:** clone a Git repo and parse its `.java` files into classes + methods, persisted in
  Postgres.
- **Prereq learning:** JavaParser/AST (javaparser.org + "JavaParser: Visited" book, free) · JPA
  relationships (Java Brains) · JGit basics.

### Pair-design session (both)
- Design the **repo-map data model**: `Repo`, `ClassNode`, `MethodNode` (fields, relationships,
  ownership). Decide IDs, what's stored vs derived.
- Decide **where clones live** (temp dir per repo, cleaned after parse) and size/time guards.
- Decide error handling: a file that fails to parse should be *skipped + logged*, not fatal.

### How it's done — step by step
1. **`POST /repos`** accepts `{ gitUrl }`; validate; persist a `Repo(status=INGESTING)` row; return
   its id. (Synchronous for Phase 1 — async comes in Phase 2.)
2. **Clone** with JGit into a temp workdir (shallow clone, `--depth 1`). Enforce a max repo size /
   timeout.
3. **Walk `.java` files**; for each, `StaticJavaParser.parse(file)` → a `CompilationUnit`.
4. **Visitor pass:** a `VoidVisitorAdapter` that visits `ClassOrInterfaceDeclaration` and
   `MethodDeclaration`; collect fully-qualified class names, method signatures, modifiers, line spans.
5. **Persist** `ClassNode` (per class) and `MethodNode` (per method, FK to class) via Spring Data JPA;
   Flyway `V2` creates the tables.
6. **Mark** `Repo(status=PARSED)`; record counts (classes, methods, parse failures).
7. **Clean up** the temp clone in a `finally` block.

### Work split
- **Dev B (lead):** the JavaParser visitor + AST extraction logic + clone/cleanup; the trickiest part.
- **Dev A:** `RepoController` (`POST /repos`), DTOs + validation, JPA entities + Flyway V2, status
  transitions.
- **Shared / handoff:** agree the entity↔visitor contract (what the visitor returns, how the service
  maps it to entities). Dev A reviews Dev B's parser PR; Dev B reviews Dev A's API/persistence PR.

### Definition of Done
- [ ] `POST /repos` with a small real OSS repo → rows in `class_node` / `method_node`.
- [ ] Parse failures are skipped + counted, never crash ingestion.
- [ ] Temp clones are cleaned up.
- [ ] Integration test (Testcontainers) ingests a fixture repo and asserts class/method counts.

### Demo deliverable
Ingest a real small Spring repo; `SELECT` the parsed classes/methods; show the counts.

---

## Week 3 — Architecture map (call graph + dependency edges)
- **Lead: Dev A** · **Non-lead: Dev B**
- **Objective:** build a queryable graph — method **call edges** and package **dependency edges** —
  and expose it via `GET /repos/{id}/map`.
- **Prereq learning:** call-graph concepts · JavaParser symbol resolution
  (`JavaSymbolSolver`) · graph modeling in SQL · System Design intro (Gaurav Sen).

### Pair-design session (both)
- Decide call-graph fidelity for v1: **resolved calls where the symbol solver can resolve them**;
  unresolved calls recorded as best-effort (by name) or skipped — pick and document.
- Design the `Edge` model: `(fromNodeId, toNodeId, type=CALL|DEPENDS_ON)`.
- Design the **map response shape**: `{ nodes: [...], edges: [...] }` suitable for a graph viewer.

### How it's done — step by step
1. **Enable `JavaSymbolSolver`** with a `CombinedTypeSolver` (ReflectionTypeSolver + a
   JavaParserTypeSolver pointed at the cloned source) so method calls resolve to declarations.
2. **Second visitor pass** over `MethodCallExpr`: resolve each call → caller method + callee method;
   emit a `CALL` edge. On `UnsolvedSymbolException`, log + skip (or store by-name) per the design
   decision.
3. **Package dependencies:** derive `DEPENDS_ON` edges from imports / cross-package calls.
4. **Persist** `Edge` rows (Flyway `V3`); index `from_node_id` / `to_node_id` for traversal.
5. **`GET /repos/{id}/map`:** assemble nodes + edges into the response shape; paginate or cap for huge
   repos.
6. **Guard N+1:** fetch nodes and edges in bounded queries, not per-node lookups (this is the JPA
   interview lesson — do it deliberately and note it).

### Work split
- **Dev A (lead):** symbol resolution setup + the call-graph builder (the hard, gnarly part).
- **Dev B:** `Edge` entity + Flyway V3 + indexes, the `GET /map` endpoint + serialization + paging.
- **Shared / handoff:** pair on symbol-resolution edge cases (overloads, generics, unresolved). Dev B
  reviews the call-graph PR; Dev A reviews the endpoint PR.

### Definition of Done
- [ ] `GET /repos/{id}/map` returns nodes + edges for an ingested repo.
- [ ] Call edges resolve for the common case; unresolved handled gracefully.
- [ ] No N+1 on map assembly (verified — e.g. via SQL logging / a count assertion).
- [ ] Integration test asserts a known call edge exists in a fixture repo.

### Demo deliverable
Return the JSON map for a real repo; optionally pipe nodes/edges through Graphviz for a visual.

---

## Week 4 — Dashboard v1 + Phase 1 polish
- **Lead: Dev B** · **Non-lead: Dev A**
- **Objective:** make Phase 1 demo-able in a browser and harden the whole phase.
- **Prereq learning:** React + TS basics + Vite (or htmx if going server-rendered) · a graph viz lib
  (e.g. a force-directed graph component).

### Pair-design session (both)
- Decide UI scope (deliberately small): a **repo list** page + a **map view** (tree or
  force-directed graph) for a selected repo.
- Decide frontend approach: React+TS+Vite **or** htmx+Thymeleaf (lower overhead). Record in an ADR if
  it's a real tradeoff for you.
- Decide what "polish" means here: integration-test coverage, README architecture section, ADR-0002.

### How it's done — step by step
1. **Scaffold the dashboard** (Vite + React + TS, or a Thymeleaf page). Wire it to call
   `GET /repos` and `GET /repos/{id}/map`.
2. **Repo list page:** table of ingested repos with status + counts; a "view map" link.
3. **Map view:** render nodes/edges (a graph component, or a collapsible package→class→method tree).
   Keep it readable for medium repos.
4. **CORS / serving:** serve the dashboard (dev proxy or static build served by Spring) — pick the
   simplest path.
5. **Hardening:** add integration tests for the full ingest→map flow (Testcontainers); handle the
   "repo not found / not parsed yet" states in the UI.
6. **Docs:** README architecture section + a Phase 1 demo GIF/video; **ADR-0002** (repo-map data
   model).

### Work split
- **Dev B (lead):** the dashboard (list + map view) and wiring it to the API.
- **Dev A:** end-to-end integration tests, error/empty states on the API side, README + ADR-0002.
- **Shared / handoff:** record the Phase 1 demo together. Dev A reviews the dashboard PR; Dev B
  reviews the tests/docs PR.

### Definition of Done
- [ ] Browser: list repos → click one → see its architecture map.
- [ ] Full ingest→map flow covered by an integration test.
- [ ] README has an architecture section; ADR-0002 written.
- [ ] **Phase 1 demo video recorded** (URL in → map rendered).
- [ ] `OWNERSHIP.md` rows for weeks 2–4 filled in.

### Demo deliverable
**Phase 1 demo:** paste a real OSS Spring repo URL → watch FORGE produce and render its architecture
map. *Résumé bullet #1 is now ready (see `06-interview-resume.md`).*

### ADR
- **ADR-0002 — Repo-map data model** (nodes/edges schema, call-graph fidelity decision, why).

---

## End-of-phase checklist
- [ ] Deterministic ingest → AST parse → call graph → queryable map, all persisted.
- [ ] REST API + a browser dashboard to view maps.
- [ ] Integration-tested with Testcontainers.
- [ ] Phase 1 demo recorded; ADR-0001/0002 done.

> Phase 2 turns this from a "tool that reads code" into a "system that *runs* code" — async queue,
> worker pool, Docker sandbox, and full observability. That's where the system-design story lands.
