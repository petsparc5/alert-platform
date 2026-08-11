# Alert Platform

Users configure **alerts** and get **notified** (email + Slack) when important real-world events occur. The demo proves the full pipeline for **USGS earthquakes → email + Slack** with a REST admin surface.

- **Design decisions & rationale:** [DESIGN.md](DESIGN.md)
- **Build plan & progress:** [IMPLEMENTATION.md](IMPLEMENTATION.md)

Maven coordinates: `com.petsparc5.alerts:alert-platform` (repo/IDE name: `Sonrisa`).

## Architecture

Modular monolith wired via Kafka topics so modules can later be split into services:

```
external feeds → Ingestion → raw.<source> → Normalization → events → Matching → notifications → Dispatch → Email / Slack
                                                   │                                                  │
                                                Postgres (events)                        Postgres (notification_deliveries)
```

## Tech stack

- **Java 25**, **Spring Boot 4.0** (Spring Framework 7)
- **PostgreSQL** (relational + JSONB), **Flyway** migrations
- **Apache Kafka** (KRaft mode, no ZooKeeper)
- **Mailpit** as the local SMTP catcher (email in the demo)
- **Lombok** for boilerplate; **Testcontainers** for integration tests

## Prerequisites

- JDK 25 (`JAVA_HOME` must point at it)
- Maven (no wrapper committed — use a local `mvn`)
- Docker + Docker Compose

## Local development

Start the infrastructure (Postgres, Kafka, Mailpit) and wait for health:

```bash
docker compose up -d --wait
```

Run the application (applies Flyway `V1`/`V2` on startup):

```bash
mvn spring-boot:run
```

Tear down:

```bash
docker compose down          # keep volumes
docker compose down -v       # also drop the Postgres volume (re-seeds on next up)
```

### Service endpoints

| Service | Host endpoint | Notes |
|---------|---------------|-------|
| App | `http://localhost:8080` | Spring Boot |
| Actuator | `http://localhost:8080/actuator/{health,info,metrics}` | |
| PostgreSQL | `localhost:5432` | db/user/pass `alerts`/`alerts`/`alerts` |
| Kafka | `localhost:29092` | host clients (see note below) |
| Mailpit SMTP | `localhost:1025` | app sends here |
| Mailpit UI | `http://localhost:8025` | view caught mail |

All of the above are overridable via env vars (`DB_HOST`, `KAFKA_BOOTSTRAP_SERVERS`, `MAIL_HOST`, …) — see `src/main/resources/application.yml`.

## Kafka connectivity

The broker exposes **two listeners** so the same cluster serves both host and containerized clients:

| Listener | Advertised as | Use from |
|----------|---------------|----------|
| `EXTERNAL` | `localhost:29092` | processes on the **host** (current demo: app run via `mvn`) |
| `INTERNAL` | `kafka:9092` | other **containers** on the compose network |

> **When the app itself is containerized** (added as a service in `docker-compose.yml`), it must connect to the internal listener — set `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`. The host default of `localhost:29092` will not resolve from inside the compose network.

### Topics

Not created yet — they are provisioned as `NewTopic` beans in the ingestion/normalization/matching/dispatch steps (see IMPLEMENTATION.md), not via broker auto-creation, so partition counts are explicit:

| Topic | Key |
|-------|-----|
| `raw.usgs` | `source:externalId` |
| `events` | dedup key |
| `notifications` | `userId` |
| `notifications.DLT` | `userId` |

### Replication factor

The demo runs a **single broker**, so every topic is created with **replication factor 1** (a higher factor would fail — you can't replicate across brokers you don't have). For real availability, run a **multi-broker cluster** (typically 3 brokers, `replication.factor=3`, `min.insync.replicas=2`); that is a post-demo infrastructure change, not a setting on this single node.

## Authentication

Simple **API-key** verification: a client sends its key, the app hashes it (SHA-256) and matches against `users.api_key_hash`; the user's `role` (`USER`/`ADMIN`) drives authorization. No sessions or tokens. JWT is a possible future exploration (DESIGN D13/D24).

### Seeded demo users

`V2__seed_demo_data.sql` seeds users with **hashed** keys. Plaintext keys for local testing (never stored):

| User | Role | API key |
|------|------|---------|
| ana.ruiz@example.com | ADMIN | `ak_demo_ana_admin` |
| bruno.costa@example.com | USER | `ak_demo_bruno` |
| carla.mendes@example.com | USER | `ak_demo_carla` |
| diego.santos@example.com | USER | `ak_demo_diego` |
| elena.ferreira@example.com | USER (DISABLED) | `ak_demo_elena` |

## Common commands

```bash
mvn compile                          # compile main sources
mvn test                             # run all tests
mvn -Dtest=ClassName test            # single test class
mvn -Dtest=ClassName#method test     # single test method
mvn package                          # build the artifact
mvn clean install                    # full clean build + install
```
