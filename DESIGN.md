# Alert Platform — Design Decisions

Living design document. Every decision is recorded here as we make it. Status legend:

- **DECIDED** — agreed, will build on it.
- **PROPOSED** — my recommendation, awaiting confirmation.
- **OPEN** — needs a call before we can proceed.
- **DEFERRED** — agreed direction, intentionally not for this phase.

Maven coordinates: `com.petsparc5.alerts:alert-platform` (repo/IDE name: `Sonrisa`).

---

## 1. Product summary

Users configure **alerts** and get **notified** when important real-world events occur (breaking news, market movements, natural disasters, extensible to more). Delivery via **email** and **Slack** initially; channel set must be extensible. An **admin view** is required.

No further product input is available — all decisions are made by us and recorded here.

---

## 2. Decision log

| # | Topic | Status | Decision / Recommendation |
|---|-------|--------|---------------------------|
| D1 | Language / framework | DECIDED | Java + Spring Boot |
| D2 | Event transport | DECIDED | Apache Kafka |
| D3 | Database | DECIDED | PostgreSQL |
| D4 | Ingestion model | DECIDED | Producers ingest via webhooks + polling → Kafka |
| D5 | Persistence purpose | DECIDED | Postgres stores historic events + users + notification preferences/settings |
| D6 | Consumption | DECIDED | Consumers read events, persist, and dispatch based on user preferences |
| D7 | Java / Spring Boot version | DECIDED | Java 25 + Spring Boot 4.0; drop to Java 21 + Spring Boot 3.5 if ecosystem friction appears |
| D8 | Deployment topology | PROPOSED | Modular monolith (single Spring Boot deployable, clear module boundaries), Kafka for async decoupling; split into services later if needed |
| D9 | Notification timing | PROPOSED | Immediate delivery first; digest/batching deferred |
| D10 | Email transport | DECIDED | Plain SMTP for the demo (no budget yet); Mailpit for local dev. Behind `EmailChannel` so a managed provider (SES, chosen for launch) drops in later — see §10.1 and D18 |
| D11 | Slack transport | DECIDED | Slack Incoming Webhook now; channel model designed to extend to a Slack App/Bot without schema migration — see §10.3 |
| D12 | Admin / UI | DECIDED | Backend-only, REST API only; no UI for now |
| D13 | End-user auth | DECIDED | Local accounts + Spring Security JWT, roles `USER`/`ADMIN`, OIDC-ready; OAuth deferred (no UI to drive redirect flow) — see §10.2 |
| D18 | Managed email provider | DEFERRED | Add **Amazon SES** behind `EmailChannel` for launch, once budget exists — replaces demo SMTP with no pipeline change |
| D14 | Data source topology | DECIDED | Per-source raw topics `raw.<source>`, each with its own connector (ingest + normalize); converge to unified `events` downstream — see §4 |
| D19 | Demo launch source | DECIDED | **USGS earthquakes only** for the demo (free, no key, poll-based); news/market/other disaster feeds deferred post-demo — see §6 |
| D20 | `events` growth strategy | DEFERRED | Monthly time-partitioning + retention policy, or TimescaleDB extension on the same Postgres, once volume warrants — see §12.1 |
| D21 | Deduplication mechanism | DECIDED | Correctness via unique constraint on `events.dedup_key` (`source:externalId`); idempotent upsert in normalization. **No app-side cache in the demo** — see §12.2 |
| D22 | Caching layer | DEFERRED | **Redis** post-demo for shared dedup set, active-rule cache, and rate-limiting. No Caffeine/in-process cache in the demo — see §12.2 |
| D23 | Testing stack | DECIDED | JUnit 5 + AssertJ + Mockito, Spring Boot test slices, Testcontainers (Postgres/Kafka), MockWebServer for USGS, MockMvc for API — see §13 |
| D24 | Demo auth simplification | DECIDED | Demo uses **HTTP Basic + roles** for speed; JWT (D13) remains the target and swaps in post-demo |
| D15 | Persistence stack | PROPOSED | Spring Data JPA + Flyway migrations; JSONB for flexible payloads/criteria |
| D16 | Local dev + test infra | PROPOSED | Docker Compose (Postgres + Kafka KRaft); Testcontainers in tests |
| D17 | Poller scaling | PROPOSED | Spring `@Scheduled` now; add ShedLock when >1 instance |

---

## 3. Architecture (PROPOSED)

Modular monolith, modules wired via Kafka topics so they can later be extracted into independent services without rework.

```
                 ┌──────────────┐   webhooks
 external feeds ─┤  Ingestion   │◄────────────
   (poll/push)   │  module      │
                 └──────┬───────┘
                        │ publish RawEvent per source
                        ▼
         [ raw.usgs ] [ raw.gdacs ] [ raw.news ] … (Kafka, one per source)
                        │
                 ┌──────▼───────┐
                 │ Normalization│  per-source connector: canonicalize + dedup
                 │  module      │──► persist Event (Postgres)
                 └──────┬───────┘
                        │ publish Event
                        ▼
                 [ events ] (Kafka)
                        │
                 ┌──────▼───────┐
                 │  Matching    │  event × active alert rules
                 │  module      │──► NotificationRequest per (user, channel)
                 └──────┬───────┘
                        ▼
                 [ notifications ] (Kafka)
                        │
                 ┌──────▼───────┐
                 │  Dispatch    │  NotificationChannel strategy
                 │  module      │──► Email / Slack / … + retries + DLT
                 └──────┬───────┘
                        │ record delivery status
                        ▼
                   Postgres (notification_deliveries)

 ┌──────────────┐
 │ Admin / API  │  Spring Security; manage users, rules, sources,
 │  module      │  view events + delivery status
 └──────────────┘
```

### Modules
- **Ingestion** — `WebhookController` (push), `PollingScheduler` + per-source connectors (pull). Connector SPI: `EventSource` producing `RawEvent`.
- **Normalization** — consumes `raw-events`, maps to canonical `Event`, deduplicates, persists, republishes to `events`.
- **Matching** — consumes `events`, evaluates active alert rules, emits one `NotificationRequest` per matched (user, channel).
- **Dispatch** — consumes `notifications`, routes to a `NotificationChannel` bean, retries with backoff, dead-letters failures, records delivery status.
- **Admin/API** — REST API secured with roles.

---

## 4. Kafka topics (DECIDED topology)

Per-source **raw** topics for independent scaling; a single **unified** normalized topic downstream.

| Topic | Key | Purpose |
|-------|-----|---------|
| `raw.<source>` (e.g. `raw.usgs`, `raw.gdacs`, `raw.news`, `raw.market`) | `source:externalId` | Raw payloads from one source; one topic per source |
| `events` | event dedup key | Normalized canonical events (source-agnostic) |
| `notifications` | `userId` | Per-(user, channel) delivery requests |
| `notifications.DLT` | `userId` | Dead-lettered undeliverable notifications |

Rationale (D14): each source scales to its own volume (market high, earthquakes low), an isolated hot/failing source can't starve others, and sources can be added/removed without touching others. Downstream converges at `events` because matching/dispatch are source-agnostic once normalized.

Each source = one **connector** that owns its `raw.<source>` topic *and* normalizes its payload into `Event`. A **config-driven source registry** provisions the topic + wiring per source (config, not code sprawl) to avoid topic sprawl.

Scaling knobs: partitions per `raw.<source>` topic + consumer concurrency per source.

Dedup: normalization enforces a unique `dedup_key` (source + external id) so re-polled/replayed events don't double-notify.

---

## 5. Domain model (PROPOSED)

### Alert rules / matching
Category discriminator + typed criteria (stored as JSONB), matched by per-category `RuleMatcher` strategy beans. Adding a category = new matcher + criteria schema, no changes to the pipeline.

- `BREAKING_NEWS` — keywords, topics
- `MARKET` — symbols, movement threshold %, direction
- `DISASTER` — types (earthquake/flood/…), region/bbox, minimum severity

### Tables (initial)
- `users` — id, email, status, created_at
- `channel_configs` — user_id, type (`EMAIL`/`SLACK`/…), config JSONB (address / webhook url), enabled — supports multiple channels per user
- `alert_rules` — id, user_id, category, criteria JSONB, active
- `events` — id, dedup_key (unique), category, type, source, severity, occurred_at, ingested_at, payload JSONB
- `notification_deliveries` — id, event_id, user_id, channel_type, status, attempts, error, sent_at
- `sources` — id, type, config JSONB, poll_interval, enabled

---

## 6. Ingestion sources

Abstracted behind `EventSource`; concrete connectors added per launch.

**Demo scope (D19):** **USGS earthquakes only.** Poll the USGS GeoJSON summary feed (e.g. `all_hour.geojson` / `significant_hour.geojson`), key each quake by its USGS event id for dedup, normalize magnitude/place/coordinates/time into the canonical `Event` (category `DISASTER`, type `EARTHQUAKE`, severity = magnitude). No API key required.

**Deferred post-demo:** GDACS (other disasters), a news API, a market data API — each a new connector + `raw.<source>` topic, no pipeline change.

---

## 7. Java / Spring Boot version (D7 — DECIDED)

**Java 25 + Spring Boot 4.0** (Spring Framework 7), matching current `pom.xml`. Fallback plan: if we hit ecosystem friction (Kafka tooling, Testcontainers, or other libs lagging the 4.0/Java 25 line), drop to **Java 21 LTS + Spring Boot 3.5**. Kept low-risk because our module boundaries and Kafka wiring don't depend on version-specific APIs.

---

## 8. Cross-cutting (PROPOSED)
- Migrations: Flyway.
- Local infra: Docker Compose (Postgres, Kafka in KRaft mode — no ZooKeeper).
- Tests: Testcontainers for Postgres + Kafka; slice tests per module.
- Observability: Spring Actuator + Micrometer.
- Delivery reliability: retry with backoff + dead-letter topic; idempotent consumers keyed on dedup/notification id.

---

## 9. Open questions summary
All core decisions resolved for the demo. Remaining items are tracked as DEFERRED in the log (D18 SES, D20 event growth, D22 Redis).

---

## 10. Trade-off notes (recorded rationale)

### 10.1 Email transport (D10)
Axis is ownership of deliverability, not "SMTP vs API" (SES exposes both).
- **Plain SMTP:** no vendor lock-in, simple protocol, but *we* own SPF/DKIM/DMARC + IP reputation + running/patching a mail server; no bounce/complaint handling.
- **Managed provider (SES / Postmark / SendGrid):** vendor manages IP reputation + DKIM signing (high inbox rates), bounce/complaint webhooks, suppression lists, templates, analytics; cheap (SES ~$0.10/1k, free tiers elsewhere); adds a vendor dependency.
- **Shared concern:** sending-domain DNS verification (DKIM) is required either way or alerts hit spam.
- **Decision:** For the demo (deadline tomorrow, no budget) use **plain SMTP** behind the `EmailChannel` abstraction, with **Mailpit** as the local dev catcher (no real mail). A managed provider is not free, so it's deferred (**D18**): swap in **Amazon SES** for launch — same `EmailChannel`, no pipeline change. Deliverability is load-bearing for an alerting product, so this is a known demo→launch upgrade, not a permanent choice.

### 10.2 Authentication (D13)
- **Local accounts (Spring Security):** self-contained, offline-friendly, no external cost; but we own password hashing, reset/MFA flows, and the extra attack surface.
- **OAuth/OIDC SSO:** IdP owns credentials + MFA, standards-based, fast onboarding; but external dependency/cost and more setup.
- **Decisive concern:** backend-only, no UI — OAuth's interactive browser redirect has nothing to complete it until a front-end exists.
- **Decision:** local accounts + Spring Security **JWT**, roles `USER`/`ADMIN`, admin endpoints locked now. Spring Security supports both models, so adding OIDC later is configuration, not a rewrite.

### 10.3 Slack extensibility (D11)
Launch with **Incoming Webhook** (user supplies a webhook URL — minimal). To grow into a **Slack App/Bot** (OAuth token, post to channels/DMs) without a schema migration:
- `channel_configs.type` distinguishes variants (e.g. `SLACK_WEBHOOK` vs future `SLACK_APP`), and `config` JSONB holds either a webhook URL *or* (later) OAuth token + workspace/channel.
- Each variant is a separate `NotificationChannel` implementation; the pipeline routes by `type` and is unaffected by the addition.

---

## 11. Demo build plan (planned — not started)

Scope: prove the full pipeline for **USGS earthquakes → email + Slack**, REST admin, due tomorrow. Nothing beyond that.

Flow: USGS poller → `raw.usgs` → normalize + persist + dedup → `events` → match against alert rules → `notifications` → dispatch to Email (SMTP/Mailpit) + Slack (webhook), record delivery → REST API to manage it all.

Build order:
1. **Skeleton & infra** — Spring Boot 4.0 deps (web, security, data-jpa, kafka, mail, validation, actuator, flyway, postgres; test: testcontainers, spring-kafka-test); `docker-compose.yml` (Postgres, Kafka KRaft, Mailpit); `application.yml`.
2. **Persistence** — Flyway `V1__init.sql`: `users`, `channel_configs`, `alert_rules`, `events`, `notification_deliveries` (no `sources` table for demo). JPA entities + repositories.
3. **Ingestion** — `UsgsPoller` (`@Scheduled`) fetches USGS GeoJSON, publishes each quake keyed by USGS event id to `raw.usgs`.
4. **Normalization** — consume `raw.usgs` → canonical `Event` (`DISASTER`/`EARTHQUAKE`, severity = magnitude), enforce unique `dedup_key`, persist, publish `events`.
5. **Matching** — consume `events` → active `EARTHQUAKE` rules (min magnitude, optional region/bbox) → `NotificationRequest` per matched (user, channel) → `notifications`.
6. **Dispatch** — consume `notifications` → `NotificationChannel` strategy (`EmailChannel` SMTP, `SlackChannel` webhook), retry + `notifications.DLT`, write `notification_deliveries`.
7. **REST API + security** — Spring Security JWT, roles `USER`/`ADMIN`. User: manage own channel configs + alert rules. Admin: list events, deliveries, users.
8. **Tests** — Testcontainers slice tests per module + one end-to-end (sample USGS payload → assert delivery record written).

Demo-scope simplifications (noted, not permanent):
- USGS poller config hardcoded (no `sources` table).
- Single in-process app; immediate delivery only (no digests).
- Dedup via DB unique constraint only — **no app-side cache** (Redis deferred, D22).
- **HTTP Basic + roles** instead of JWT (D24); JWT is the target (D13).

### 11.1 Demo cut line
**In:** pipeline ingest→dispatch, both channels (email via Mailpit, Slack webhook), minimal REST to create a user/rule and view events + deliveries, and the two priority tests (§13).
**Out (post-demo):** full test pyramid, JWT, Redis, event partitioning/retention, `sources` table, additional feeds.

**Risk register:**
- Spring Boot 4.0 / Java 25 ecosystem friction → fallback to Java 21 + Spring Boot 3.5 (D7).
- Six topics + consumers are the bulk of the work; kept because it's the demo's whole point.

---

## 12. Data store & deduplication

### 12.1 Why PostgreSQL (D3)
Workload is two shapes in one store: **relational/transactional** (users, channel configs, rules, deliveries — need constraints, joins, ACID) and **semi-structured, append-heavy** (`events` + flexible per-source payloads / per-category criteria). Postgres serves both: relational core **plus** `JSONB` (GIN-indexable) for flexible fields, and a unique constraint gives dedup for free.

Rejected: MySQL (weaker JSON), MongoDB (loses relational integrity + multi-entity transactions for users/rules/deliveries), dedicated time-series DB (extra ops for a demo).

**Concern — `events` growth (D20):** append-heavy and unbounded. Not a demo problem. Launch mitigation, staying on one database: monthly **time-partitioning** + **retention policy**, or the **TimescaleDB** extension (hypertables + compression) if volume climbs.

### 12.2 Deduplication & caching (D21, D22)
Two separate concerns:
- **Correctness (in for demo):** source of truth is a **unique constraint on `events.dedup_key`** = `source:externalId` (e.g. `usgs:us7000abcd`). Normalization does an idempotent insert/upsert, so Kafka redelivery, replays, and overlapping poll windows never store or notify twice. Kafka keying by that id keeps one event's records in a single partition (ordering).
- **Optimization (deferred, D22):** USGS re-serves the same quakes each poll, so most polled items are dupes. A cache would short-circuit the DB "seen it?" check, and matching would cache rarely-changing active rules. **Chosen approach:** skip in-process (Caffeine) entirely and go straight to **Redis** post-demo — shared dedup set + rule cache + rate-limiting in one place, correct across multiple instances. For the demo the DB constraint is the sole (and sufficient) guard.

---

## 13. Testing strategy (D23)

**Frameworks:** JUnit 5, AssertJ, Mockito (unit); Spring Boot test slices (`@DataJpaTest`, `@WebMvcTest`, `@SpringBootTest`); Testcontainers (real Postgres + Kafka); MockWebServer/WireMock (stub USGS feed); MockMvc (API); `spring-kafka-test` embedded broker where a full container is too slow.

**Test pyramid:**
| Layer | What | Example |
|-------|------|---------|
| Unit | Pure logic, no Spring | `EarthquakeRuleMatcher` (magnitude/region), USGS GeoJSON → `Event` mapper |
| Slice | One layer + its context | `@DataJpaTest` dedup constraint; `@WebMvcTest` + security on API |
| Integration | Real infra via Testcontainers | Publish to `raw.usgs` → assert `Event` persisted once (dedup) |
| End-to-end | Whole pipeline | Sample USGS payload in → `notification_deliveries` row out |
| Security | AuthZ | non-admin blocked from admin endpoints; auth required |

**Demo priority:** unit tests on the **matcher + normalizer** (highest logic risk) and **one end-to-end happy path**; backfill slice/integration/security tests post-demo. Conventions: Given-When-Then, FIRST/CORRECT, no comments.
