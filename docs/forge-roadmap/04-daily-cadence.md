# 04 — Daily Execution Cadence

A repeatable weekday loop. Pick the variant matching the day's available time. The order
(Learn → Build → Review → Doc) front-loads thinking when you're fresh and ends with cheap wins.

> Both devs run their own daily loop independently, then sync at the weekly pair-design session and on
> PR reviews. The phase files tell you *what* to build each week; this file tells you *how to spend a
> day*.

---

## 2-hour/day version

| Time | Block | What |
|---|---|---|
| 0:00–0:15 | **Standup-with-self** | Read yesterday's notes; set today's one outcome |
| 0:15–0:35 | **Learn** | One focused topic from `05-learning-roadmap.md` tied to today's task (video at 1.5x or docs) |
| 0:35–1:40 | **Implement** | The day's task (TDD where possible) |
| 1:40–1:55 | **Review/commit** | Open a PR, or review your partner's PR |
| 1:55–2:00 | **Doc** | One-line CHANGELOG / note for tomorrow |

## 3-hour/day version

| Time | Block | What |
|---|---|---|
| 0:00–0:15 | **Standup-with-self** | Set today's outcome |
| 0:15–0:45 | **Learn** | Deeper: one concept + take notes |
| 0:45–2:15 | **Implement** | One meaningful slice end-to-end |
| 2:15–2:40 | **Review** | Review your partner's PR thoroughly — *this is where you learn the other half of the system* |
| 2:40–2:55 | **Test/verify** | Run it; check traces/metrics |
| 2:55–3:00 | **Doc** | Notes, ADR stub if a decision was made |

## Weekend version (one 3–5 hr block)

| Time | Block | What |
|---|---|---|
| 0:00–0:30 | **Pair-design** | Both devs; the week's design conversation (the "Pair-design session" in the phase file) |
| 0:30–1:00 | **Learning deep-dive** | A heavier topic (concurrency, RAG, OTel) — watch + sketch |
| 1:00–3:30 | **Build sprint** | The week's hardest component; pair-program the gnarly part |
| 3:30–4:15 | **Integration + demo prep** | Wire pieces; record a short demo clip |
| 4:15–5:00 | **Docs + retro** | Update README/ADR/OWNERSHIP; plan next week |

---

## Weekly rhythm (how the daily loops add up)

```
Mon  Tue  Wed  Thu  Fri        Sat or Sun
 │    │    │    │    │              │
 └────┴────┴────┴────┘              └── weekend block:
 weekday loops (2–3 hr):                pair-design (next week) +
 implement your split, review           build sprint + demo + retro
 partner's PR, learn prereqs
```

- **Pair-design** happens once per week (weekend block or a dedicated weekday slot) **before**
  splitting implementation.
- **Cross-review** happens continuously — never merge your own PR without the partner's review.
- **Demo** is recorded at the end of each phase (Weeks 4, 8, 14) and ideally a quick clip each week.

---

## Cadence guardrails

1. **Protect the weekly demo deliverable over polish.** If a week slips, cut scope, keep the demo.
2. **Do the prereq learning *first*.** Each week in the phase files lists "Prereq learning" — don't
   start the build before watching/reading it; you'll build the wrong thing slower.
3. **Keep a `DECISIONS.md` / ADR folder.** Every real decision = a future interview story.
4. **Timebox rabbit holes.** If stuck >45 min solo, bring it to the partner or park it as a task.
5. **End every session with a one-liner for tomorrow** so you restart with zero ramp-up.
