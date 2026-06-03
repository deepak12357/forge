# Phase 0 — Foundations (Week 1)

**Goal:** a professional project skeleton — repo, CI, conventions, local infra — before any feature
code. Maximum hygiene, zero features. This is what separates a "side project" from something that
reads as production-grade in interviews.

> **Reading note:** each week below uses the standard template (Objective · Prereq learning ·
> Pair-design · How it's done · Work split · Definition of Done · Demo · ADR). Do the **Prereq
> learning** (from `05-learning-roadmap.md`) *before* the build.

---

## Week 1 — Project skeleton + CI + local infra
- **Lead: Dev A** · **Non-lead: Dev B**
- **Objective:** `docker-compose up` brings up local infra; the Spring Boot app boots and
  `/health` returns 200; a PR turns CI green (build + test + format).
- **Prereq learning:** Spring Boot 3 setup (Java Brains / Dan Vega) · Maven basics (POM, lifecycle,
  plugins) · GitHub Actions (TechWorld with Nana) · Docker Compose · Testcontainers intro.

### Pair-design session (both, ~30–45 min)
- Agree the **module layout** (`com.forge.{api,job,repo,...}` — see `02-architecture.md`).
- Agree **conventions:** branch naming, commit style, PR template, who merges, formatter
  (google-java-format via Spotless).
- Confirm **build tool**: Maven (this plan's default; record the choice + why in ADR-0001).
- Decide the **local infra** for Phase 0: just `postgres` + `redis` in `docker-compose.yml`
  (everything else added when its phase needs it).

### How it's done — step by step
1. **Create the repo** on GitHub (public — free CI). Add `.gitignore` (Java/Maven), `LICENSE`,
   `README.md`. Turn on **branch protection** for `main` (require PR + 1 review + green CI).
2. **Scaffold Spring Boot 3** via start.spring.io (select **Maven**): dependencies = Web, Validation,
   Spring Data JPA, PostgreSQL driver, Actuator, Testcontainers. Set Java 21 via
   `<java.version>21</java.version>` (the `maven-compiler-plugin` picks it up).
3. **Package skeleton:** create empty packages from the agreed module layout so structure is visible
   from commit #1.
4. **`docker-compose.yml`:** `postgres:16` (with a named volume) and `redis:7`. Add a
   `application-local.yml` pointing at them. Confirm `docker compose up -d` + app boot works.
5. **Health endpoint:** enable Actuator; verify `GET /actuator/health` → 200. (This is your liveness
   probe later.)
6. **First test:** a Testcontainers test that spins a real Postgres, runs a trivial repository
   save/find, asserts it works. This proves the test infra end-to-end.
7. **CI (`.github/workflows/ci.yml`):** on PR + push to `main` — checkout, `setup-java` (JDK 21,
   `cache: maven` for the `~/.m2` cache), `./mvnw -B spotless:check verify`. Make these **required
   checks** in branch protection.
8. **Spotless:** add the `spotless-maven-plugin` with google-java-format; `./mvnw spotless:apply`
   formats, `spotless:check` (bound to the `verify` phase) gates CI.
9. **README v1:** what FORGE is (one paragraph), how to run locally, the architecture diagram from
   `02-architecture.md`, and a link to this roadmap directory's intent.
10. **ADR-0001:** record the tech-stack decision + the build-tool choice + why (see `01-tech-stack.md`
    for the rationale to summarize).

### Work split
- **Dev A (lead):** Maven + Spring Boot scaffold (POM), package layout, Actuator health, the CI
  workflow, Spotless, branch protection.
- **Dev B:** `docker-compose.yml` (postgres+redis), `application-local.yml`, the first Testcontainers
  test, the PR template + `.gitignore`/`LICENSE`, README v1.
- **Shared / handoff:** pair on the *first PR* together so both see the full PR→review→CI→merge loop
  once. Dev B reviews Dev A's CI PR; Dev A reviews Dev B's compose+test PR. Co-author ADR-0001.

### Definition of Done
- [ ] `docker compose up -d` starts postgres + redis cleanly.
- [ ] App boots locally; `GET /actuator/health` returns 200.
- [ ] A PR shows CI **green** (spotless + build + test) and is merged via the protected-branch flow.
- [ ] The Testcontainers test passes in CI (not just locally).
- [ ] README v1 + ADR-0001 committed.
- [ ] `OWNERSHIP.md` Week 1 row filled in.

### Demo deliverable
Screen-record: open a PR → CI runs and goes green → merge → `docker compose up` → app boots →
`/actuator/health` 200. ~60–90 seconds.

### ADR
- **ADR-0001 — Tech stack & build tool selection** (what you chose and why; summarize from
  `01-tech-stack.md`).

---

## End-of-phase checklist
- [ ] Repo is public with branch protection + required CI checks.
- [ ] Local infra (postgres, redis) runs via Compose.
- [ ] App boots, health endpoint live, one real integration test green in CI.
- [ ] README + ADR-0001 in place.
- [ ] Both devs have completed the full PR→review→merge loop at least once.

> You now have a credible, professional skeleton. Phase 1 starts adding the first real capability:
> reading Java repositories.
