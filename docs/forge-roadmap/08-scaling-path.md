# 08 — Industry Upgrade Path (post-MVP scaling)

The MVP (end of Phase 3) is a **modular monolith**. Each step below is an independent, interview-driven
upgrade — do them opportunistically once you're already interviewing. Each is one résumé bullet and
one system-design story.

---

## 8.1 Service split (gRPC)

Extract the **Sandbox Executor** into its own service; API ↔ Executor talk over gRPC. Teaches service
boundaries, typed contracts, backpressure.

```
[API + Orchestrator] --gRPC--> [Executor Service] --docker--> [Sandboxes]
```

- **Why first:** the executor is the natural seam (heavy, isolatable, independently scalable).
- **Learning:** protobuf, gRPC streaming, service contracts, deadlines/cancellation.

## 8.2 Kubernetes-native execution

Run on k3s/kind (free local) → EKS (cloud). Workers as a Deployment with an **HPA** scaling on queue
depth; sandboxes as ephemeral Jobs/Pods.

```
        ┌──────── k8s cluster ─────────┐
 API ──►│ Deployment: workers (HPA)     │──► Job pods (sandboxes, gVisor)
        │ Deployment: api               │
        │ StatefulSet: postgres/redis    │
        └───────────────────────────────┘
```

- **Learning:** k8s objects, HPA, resource quotas, ephemeral Jobs, gVisor hardening.

## 8.3 Distributed workflow engine

Replace the hand-rolled state machine with a durable workflow engine (e.g. **Temporal** OSS) or a
Kafka-driven saga. Gives durable, resumable, observable execution for free.

```
[Workflow Engine] orchestrates: Analyze → Plan → CodeGen → Execute → Evaluate → Retry
   (durable, resumable, observable steps)
```

- **Learning:** durable execution, sagas, idempotency at scale, event-driven design.

## 8.4 Multi-repository support

Tenant model: many repos, isolated workspaces, per-repo memory namespaces in pgvector, fairness in
the queue (per-tenant rate limits).

- **Learning:** multi-tenancy, isolation, fair scheduling, noisy-neighbor handling.

## 8.5 Multi-agent architecture

Specialized agents (Planner / Coder / Critic / Fixer) coordinating via a shared blackboard
(Postgres/Redis) under a supervisor — *each still execution-validated*.

```
        ┌──────── Supervisor ────────┐
        ▼          ▼          ▼       ▼
   [Planner]   [Coder]    [Critic]  [Fixer]
        └──── shared state (blackboard) ────┘  → Sandbox-validated outputs
```

- **Learning:** agent orchestration, role specialization, conflict resolution — the hot 2025–26 topic,
  done *without* losing reliability.

## 8.6 Enterprise SaaS platform

Multi-tenant auth (OIDC), per-tenant quotas/billing metering, audit logs, RBAC, secrets management,
horizontal scale on EKS, Kafka for events, S3 for artifacts.

- **Learning:** auth/authz, metering/billing, audit, the full "I can build a product" capstone.

---

## Evolution summary

```
Monolith
  → service split (gRPC)
    → Kubernetes + HPA
      → durable workflow engine
        → multi-repo (multi-tenant)
          → multi-agent
            → multi-tenant SaaS
```

Each arrow is a deliberate, justifiable step — never adopt the next layer until the current one's
limits are real (and you can articulate *why* in an interview). Premature Kafka/k8s/multi-agent is an
anti-pattern; adding them *when load justifies it* is the senior signal.
