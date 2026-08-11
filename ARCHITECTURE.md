# Alert Platform — Diagrams

Visual companion to [`DESIGN.md`](./DESIGN.md). Diagrams use [Mermaid](https://mermaid.js.org/) and render in GitHub, IntelliJ, and most Markdown viewers. Reflects the demo scope (USGS earthquakes → email + Slack).

---

## 1. Data model (ER diagram)

```mermaid
erDiagram
    USERS ||--o{ CHANNEL_CONFIGS : "has"
    USERS ||--o{ ALERT_RULES : "defines"
    USERS ||--o{ NOTIFICATION_DELIVERIES : "receives"
    EVENTS ||--o{ NOTIFICATION_DELIVERIES : "triggers"

    USERS {
        uuid id PK
        string email UK
        string password_hash
        string role "USER | ADMIN"
        string status
        timestamptz created_at
    }

    CHANNEL_CONFIGS {
        uuid id PK
        uuid user_id FK
        string type "EMAIL | SLACK_WEBHOOK"
        jsonb config "email address / webhook url"
        boolean enabled
    }

    ALERT_RULES {
        uuid id PK
        uuid user_id FK
        string category "DISASTER"
        jsonb criteria "type=EARTHQUAKE, minMagnitude, region/bbox"
        boolean active
        timestamptz created_at
    }

    EVENTS {
        uuid id PK
        string dedup_key UK "source:externalId e.g. usgs:us7000abcd"
        string category "DISASTER"
        string type "EARTHQUAKE"
        string source "usgs"
        numeric severity "magnitude"
        timestamptz occurred_at
        timestamptz ingested_at
        jsonb payload "place, coords, raw fields"
    }

    NOTIFICATION_DELIVERIES {
        uuid id PK
        uuid event_id FK
        uuid user_id FK
        string channel_type "EMAIL | SLACK_WEBHOOK"
        string status "PENDING | SENT | FAILED | DEAD_LETTERED"
        int attempts
        string error
        timestamptz sent_at
    }
```

Notes:
- `dedup_key` unique constraint is the sole dedup guard for the demo (DESIGN §12.2).
- `config` and `criteria` are `JSONB` so channels and rule types extend without schema migrations.
- `sources` table is deferred; the demo hardcodes the USGS poller config.

---

## 2. System architecture & event flow

```mermaid
flowchart TD
    subgraph ext[External]
        USGS[USGS GeoJSON feed]
    end

    subgraph ingestion[Ingestion module]
        POLL[UsgsPoller @Scheduled]
    end

    subgraph norm[Normalization module]
        N[Normalizer: canonicalize + dedup + persist]
    end

    subgraph match[Matching module]
        M[Rule matcher: event x active alert rules]
    end

    subgraph dispatch[Dispatch module]
        D[NotificationChannel router + retry]
        EMAIL[EmailChannel - SMTP]
        SLACK[SlackChannel - webhook]
    end

    subgraph api[Admin / API module]
        REST[REST API - Spring Security roles]
    end

    DB[(PostgreSQL)]
    MAIL[Mailpit / SMTP]
    SL[Slack Incoming Webhook]

    USGS -->|poll| POLL
    POLL -->|publish RawEvent| T1([raw.usgs])
    T1 --> N
    N -->|persist Event| DB
    N -->|publish Event| T2([events])
    T2 --> M
    M -->|read active rules| DB
    M -->|NotificationRequest per user,channel| T3([notifications])
    T3 --> D
    D --> EMAIL --> MAIL
    D --> SLACK --> SL
    D -->|record delivery| DB
    D -.->|on failure| DLT([notifications.DLT])

    REST --- DB

    classDef topic fill:#fde68a,stroke:#b45309,color:#000;
    class T1,T2,T3,DLT topic;
```

Yellow nodes are Kafka topics. Per-source raw topics (`raw.<source>`) converge to the single normalized `events` topic; matching and dispatch are source-agnostic (DESIGN §4).

---

## 3. Deferred / post-demo (for context)

```mermaid
flowchart LR
    subgraph now[Demo]
        A[DB unique constraint dedup]
        B[HTTP Basic auth]
        C[SMTP via Mailpit]
        E[Single events table]
    end
    subgraph later[Post-demo]
        A2[Redis: shared dedup + rule cache + rate limit]
        B2[JWT then OIDC-ready]
        C2[Amazon SES]
        E2[Partitioning / TimescaleDB + retention]
        F2[More sources: GDACS, news, market]
        G2[Slack App/Bot]
    end
    A --> A2
    B --> B2
    C --> C2
    E --> E2
```
