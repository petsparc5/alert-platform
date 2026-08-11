# Alert Platform — Implementation Plan & Progress

Tracks *what has been built*. Design decisions and their rationale live in **[DESIGN.md](DESIGN.md)**.

Scope: prove the full pipeline for **USGS earthquakes → email + Slack** with a REST admin surface.

Flow: USGS poller → `raw.usgs` → normalize + persist + dedup → `events` → match against alert rules → `notifications` → dispatch to Email (SMTP/Mailpit) + Slack (webhook), record delivery → REST API to manage it all.

---

## Progress

| Step | Description | Status |
|------|-------------|--------|
| 1 | Skeleton & infra | ✅ Done (2026-08-11) |
| 2 | Persistence | ✅ Done (2026-08-11) |
| 3 | Ingestion | ✅ Done (2026-08-11) |
| 4 | Normalization | 🔄 In progress (2026-08-12) — tests written, implementations stubbed |
| 5 | Matching | ⬜ Not started |
| 6 | Dispatch | ⬜ Not started |
| 7 | REST API + security | ⬜ Not started |
| 8 | Tests | ⬜ Not started |

---

## Build order

### 1. Skeleton & infra — ✅ Done
Spring Boot 4.0 deps (web, security, data-jpa, kafka, mail, validation, actuator, flyway, postgres; test: testcontainers, spring-kafka-test); `docker-compose.yml` (Postgres, Kafka KRaft, Mailpit); `application.yml`.

Delivered:
- `pom.xml` — `spring-boot-starter-parent` 4.0.0, Java 25; the starters above plus `spring-kafka`, `flyway-core` + `flyway-database-postgresql`, `postgresql`; Lombok (D26); Testcontainers BOM 1.20.6 (Spring Boot 4.0 no longer manages Testcontainers versions).
- `AlertPlatformApplication` + five module packages (`ingestion`, `normalization`, `matching`, `dispatch`, `api`).
- `application.yml` — datasource, JPA (`validate`, `open-in-view: false`), Flyway, Kafka, mail, actuator; env-overridable.
- `docker-compose.yml` — Postgres 17, Kafka 3.9 (KRaft, single node; dual `INTERNAL`/`EXTERNAL` listeners → `kafka:9092` for containers, `localhost:29092` for host; healthcheck), Mailpit. RF forced to 1 on a single broker (multi-broker path documented in README).
- `README.md` — overview, stack, local-dev/compose instructions, Kafka connectivity + `kafka:9092` containerization note, replication-factor guidance, demo API keys.

### 2. Persistence — ✅ Done
Flyway `V1__init.sql`: `users`, `channel_configs`, `alert_rules`, `events`, `notification_deliveries` (no `sources` table for the demo). JPA entities + repositories.

Delivered:
- `V1__init.sql` — five tables. `users.id` UUID; all other PKs `BIGINT` identity (D25). Enums as `VARCHAR` + `CHECK`. JSONB on `channel_configs.config`, `alert_rules.criteria`, `events.payload`. Unique `events.dedup_key`.
- `V2__seed_demo_data.sql` — realistic dummy data: 5 users (mix of `USER`/`ADMIN`, one `DISABLED`), their channel configs, 6 alert rules, 10 USGS earthquake events, 8 deliveries across `SENT`/`FAILED`/`DEAD_LETTERED`/`PENDING`. API keys stored as SHA-256 hashes (D24); plaintext demo keys documented in the delivery notes, not committed to the schema.
- Entities under `com.petsparc5.alerts.persistence.entity`; repositories under `…persistence.repository`. Plain ID fields (UUID/Long) instead of JPA associations. Lombok integrated; accessors added per class only when consumed.

### 3. Ingestion — ✅ Done
`UsgsPoller` (`@Scheduled`) fetches USGS GeoJSON, publishes each quake keyed by USGS event id to `raw.usgs`.

Delivered:
- `FeedClient<T>` — pull-based source contract (`fetch()`); `SourcePoller<T>` — template base owning fetch→map→publish and default error handling (`safeExecute`/`onPollError`), subclasses supply `toRawEvents`.
- `usgs.dto` — `UsgsFeature`/`UsgsProperties`/`UsgsGeometry`/`UsgsFeatureCollection` records mapping the GeoJSON summary feed, `@JsonIgnoreProperties(ignoreUnknown = true)`.
- `UsgsFeedClient` — `RestClient`-backed `FeedClient<UsgsFeatureCollection>`, feed URL from `ingestion.usgs.feed-url`; throws `IllegalStateException` on a null/structurally invalid body.
- `UsgsPoller` — `SourcePoller<UsgsFeatureCollection>`, `@Scheduled(fixedDelayString = "${ingestion.usgs.poll-interval-ms}")`; skips features with a null id (logged), serializes each valid feature as the `RawEvent` payload.
- `RawEvent`/`RawEventPublisher` — publishes to `raw.<source>` keyed by `source:externalId`.
- `InfrastructureConfiguration` — manual `RestClient.Builder` and `ObjectMapper` beans (D28 gap).
- Tests: `UsgsFeedClientTest` (`MockRestServiceServer`), `UsgsPollerTest`, `RawEventPublisherTest` (Mockito, no Spring context).

### 4. Normalization — 🔄 In progress
Consume `raw.usgs` → canonical `Event` (`DISASTER`/`EARTHQUAKE`, severity = magnitude), enforce unique `dedup_key`, persist, publish `events`.

TDD red phase committed: tests written first, production classes compile with stubbed (`UnsupportedOperationException`) bodies, all normalization tests currently failing by design.

- `EventNormalizer<T>` — mapping contract (raw DTO → canonical `Event`), mirrors `FeedClient<T>`; kept as a pure, Spring-free unit to match the D23 test-pyramid priority on the mapper.
- `NormalizedEvent` — wire record for the `events` topic, decoupled from the `Event` JPA entity.
- `EventPublisher` — will serialize `Event` → `NormalizedEvent` JSON and publish to `events` keyed by `dedup_key`. *(stubbed)*
- `usgs.UsgsEventNormalizer implements EventNormalizer<UsgsFeature>` — will map magnitude/place/coordinates/time to the canonical `Event`. *(stubbed)*
- `usgs.UsgsRawEventListener` — `@KafkaListener(topics = "raw.usgs")`; will deserialize, normalize, check `existsByDedupKey`, persist, and republish. *(stubbed)*
- `Event` entity gained `@Getter @Builder @NoArgsConstructor @AllArgsConstructor` (Lombok, D26) now that normalization is its first consumer.
- Tests: `UsgsEventNormalizerTest` (pure JUnit/AssertJ), `EventPublisherTest`, `UsgsRawEventListenerTest` (Mockito, no Spring context) — same no-Spring-context style as the ingestion tests.

Next: implement the stubbed methods to turn the suite green.

### 5. Matching — ⬜ Not started
Consume `events` → active `EARTHQUAKE` rules (min magnitude, optional region/bbox) → `NotificationRequest` per matched (user, channel) → `notifications`.

### 6. Dispatch — ⬜ Not started
Consume `notifications` → `NotificationChannel` strategy (`EmailChannel` SMTP, `SlackChannel` webhook), retry + `notifications.DLT`, write `notification_deliveries`.

### 7. REST API + security — ⬜ Not started
API-key auth + roles `USER`/`ADMIN` (D24). User: manage own channel configs + alert rules. Admin: list events, deliveries, users.

### 8. Tests — ⬜ Not started
Testcontainers slice tests per module + one end-to-end (sample USGS payload → assert delivery record written).

---

## Demo-scope simplifications (noted, not permanent)
- USGS poller config hardcoded (no `sources` table).
- Single in-process app; immediate delivery only (no digests).
- Dedup via DB unique constraint only — **no app-side cache** (Redis deferred, D22).
- **API keys + roles** instead of JWT (D24); JWT is the target (D13).
- **Slack webhook secrets** injected into the seed via Flyway placeholders from an uncommitted `.env` (see `.env.example`). **TODO(security):** encrypt `channel_configs.config` at rest (application-level column encryption) rather than storing webhook URLs in plaintext.

## Demo cut line
**In:** pipeline ingest→dispatch, both channels (email via Mailpit, Slack webhook), minimal REST to create a user/rule and view events + deliveries, and the two priority tests (§13 of DESIGN).
**Out (post-demo):** full test pyramid, JWT, Redis, event partitioning/retention, `sources` table, additional feeds.

## Risk register
- Spring Boot 4.0 / Java 25 ecosystem friction → fallback to Java 21 + Spring Boot 3.5 (D7).
- Six topics + consumers are the bulk of the work; kept because it's the demo's whole point.
