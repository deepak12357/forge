# OWNERSHIP — Rotation Tracker

> Fill this in at the end of each week. It keeps the rotation honest (neither dev specializes) and is
> a great "we ran this like a real team" artifact to mention in interviews.

## Roles
- **Lead** — owns the week's hardest / most central component. Drives the build of the core piece.
- **Non-lead** — owns the secondary component + tests, **drives that week's PR review**, and writes
  that week's docs/ADR.
- Both attend the **pair-design** session. Both write tests. Both deploy.

## Planned rotation (lead alternates every week)

| Week | Phase | Planned Lead | Planned Non-lead |
|---|---|---|---|
| 1  | 0 | Dev A | Dev B |
| 2  | 1 | Dev B | Dev A |
| 3  | 1 | Dev A | Dev B |
| 4  | 1 | Dev B | Dev A |
| 5  | 2 | Dev A | Dev B |
| 6  | 2 | Dev B | Dev A |
| 7  | 2 | Dev A | Dev B |
| 8  | 2 | Dev B | Dev A |
| 9  | 3 | Dev A | Dev B |
| 10 | 3 | Dev B | Dev A |
| 11 | 3 | Dev A | Dev B |
| 12 | 3 | Dev B | Dev A |
| 13 | 3 | Dev A | Dev B |
| 14 | 3 | Dev B | Dev A |

Result: 7 lead-weeks each. Because the *secondary* component also rotates, both devs touch queueing,
sandboxing, observability, RAG, agents, and the dashboard at least once as primary.

## Actual log (fill in as you go)

| Week | Lead (actual) | Non-lead (actual) | Core component built | PR(s) + reviewer | Demo recorded? | ADR(s) | Notes |
|---|---|---|---|---|---|---|---|
| 1  |  |  |  |  | ☐ |  |  |
| 2  |  |  |  |  | ☐ |  |  |
| 3  |  |  |  |  | ☐ |  |  |
| 4  |  |  |  |  | ☐ |  |  |
| 5  |  |  |  |  | ☐ |  |  |
| 6  |  |  |  |  | ☐ |  |  |
| 7  |  |  |  |  | ☐ |  |  |
| 8  |  |  |  |  | ☐ |  |  |
| 9  |  |  |  |  | ☐ |  |  |
| 10 |  |  |  |  | ☐ |  |  |
| 11 |  |  |  |  | ☐ |  |  |
| 12 |  |  |  |  | ☐ |  |  |
| 13 |  |  |  |  | ☐ |  |  |
| 14 |  |  |  |  | ☐ |  |  |

## ADR log (one row per real decision)

| ADR | Title | Week | Author | Decision (1 line) |
|---|---|---|---|---|
| 0001 | Build tool selection | 1 |  | Use Maven (familiarity, MNC ubiquity) — see `adr/0001-build-tool-selection.md` |
| 0002 | Repo-map data model | 3 |  |  |
| 0003 | Postgres SKIP LOCKED queue vs Redis/Kafka | 5 |  |  |
| 0004 | Sandbox isolation model | 6 |  |  |
| 0005 | AI architecture (swappable LlmClient) | 14 |  |  |
| 0006 | Eval methodology | 14 |  |  |
