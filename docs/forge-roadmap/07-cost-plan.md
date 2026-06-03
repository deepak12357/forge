# 07 — Cost Optimization Plan

**Principle:** everything runs locally for free through Phase 3. Spend money only to *prove* a cloud
résumé bullet near placement time, and time-box it.

---

## Free infrastructure / local-first

| Need | Free choice |
|---|---|
| Compute | Your laptops (+ an old PC as a self-hosted GitHub Actions runner / Ollama box if available) |
| DB / cache | PostgreSQL + Redis in Docker |
| LLM | Ollama (Qwen2.5-Coder / DeepSeek-Coder) |
| Vector DB | pgvector *inside the existing Postgres* (no new infra) |
| Observability | Prometheus + Grafana + Jaeger in Docker |
| CI/CD | GitHub Actions (free for public repos) + GHCR |
| AWS learning | LocalStack (community) emulates S3/SQS locally |
| Registry / secrets | GHCR + GitHub encrypted secrets |

Everything above is **$0**. The entire MVP (Phases 0–3) can be built without a cloud account.

---

## Open-source LLM options (by available RAM)

| RAM | Model suggestion |
|---|---|
| ≥16 GB | Qwen2.5-Coder 7B / DeepSeek-Coder 6.7B (good code quality) |
| 8–16 GB | 3B–7B **quantized (Q4)** variants |
| <8 GB | small 1.5–3B models for plumbing; lean on the paid fallback for quality |

**Paid fallback (only for quality-critical generation):** Gemini Flash (has a free tier) or Claude
Haiku (cheap). Always behind a **hard monthly cap + token budget + prompt caching + rate limiting**
(built in Week 14). Default everything to the local model; escalate to paid only when a task needs it.

---

## When to use cloud / when not to

**Use cloud when:**
- You need the *résumé bullet* — "deployed to AWS ECS/EKS, IaC with Terraform."
- You're demonstrating autoscaling / managed services for an interview.
- → Do it **once**, capture screenshots + commit the Terraform, then **tear it down**.

**Do NOT use cloud for:**
- Day-to-day development.
- LLM inference (use Ollama).
- Storage or "because it's there."
- → LocalStack + local Docker cover all learning at $0.

---

## Estimated monthly cost by phase

| Phase | Setup | Est. cost/mo |
|---|---|---|
| Phase 1 | Fully local | **$0** |
| Phase 2 | Fully local (Docker stack) | **$0** |
| Phase 3 | Local + tiny paid LLM fallback (capped) | **~$0–20** (Gemini Flash free tier can keep this ~$0; hard-cap at $20) |
| Phase 4 | LocalStack + a brief real-AWS proof (Fargate/EKS), torn down | **~$10–40** for a few days, then **$0** |
| Production-grade (always-on demo) | Small AWS: 1 small RDS, ECS Fargate, ALB, S3 | **~$40–90/mo** — *optional* |

---

## The cheap-but-convincing alternative to a live deployment

A live always-on cloud demo is **not required** for interviews. Cheaper and often more persuasive:

1. A polished **GitHub repo** with a great README + architecture diagram.
2. A **2–4 min demo video** per phase (screen recording of FORGE working + the Grafana traces).
3. The **Terraform + a teardown note** proving you *can* deploy to AWS, run once for screenshots.

Spin up live cloud only on demand (e.g. if an interviewer specifically wants a live URL).

---

## Cost guardrails to build into FORGE (Week 14)

- **Monthly spend cap** on the paid LLM provider — refuse calls past the cap, log + degrade to local.
- **Per-job token budget** — bound context size; truncate via RAG instead of dumping whole files.
- **Prompt caching** — cache stable prompt prefixes (system + repo context) to cut paid token cost.
- **Rate limiting** — token-bucket (Redis) in front of the paid API to avoid burst overspend.
- **Metric:** expose `llm_cost_usd_total` and `llm_tokens_total` in Prometheus; alert if cost rate
  exceeds budget.
