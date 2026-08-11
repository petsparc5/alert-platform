INSERT INTO users (id, email, status, role, api_key_hash, created_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'ana.ruiz@example.com',      'ACTIVE',   'ADMIN', 'e170e6ed065b7e96757879b3c2aecd4d3406f38b95f2786714a1b968ec22afa8', '2026-07-01T09:12:00Z'),
    ('22222222-2222-2222-2222-222222222222', 'bruno.costa@example.com',   'ACTIVE',   'USER',  '24ddaef8a3a4a845573096b339c7e3ae23d770470fdb9083d4a1ecb103519016', '2026-07-03T14:45:00Z'),
    ('33333333-3333-3333-3333-333333333333', 'carla.mendes@example.com',  'ACTIVE',   'USER',  'b8d54b374e401322c9eecc0a91eaacb3d060929b714e57beab74b0ec95ee1dfa', '2026-07-08T18:20:00Z'),
    ('44444444-4444-4444-4444-444444444444', 'diego.santos@example.com',  'ACTIVE',   'USER',  'c3d8708c0521606e05c70fdca9dec8be72444b2177d6226354d3dc82ce7aa2e8', '2026-07-15T11:03:00Z'),
    ('55555555-5555-5555-5555-555555555555', 'elena.ferreira@example.com','DISABLED', 'USER',  'dd830b3abfcec9ae09bed1555bc3a701cc4434a71267ef7d6a62c762a8115a5b', '2026-07-20T08:30:00Z');

-- TODO(security): SLACK_WEBHOOK configs hold secret webhook URLs. Encrypt the
-- `config` JSON at rest (application-level column encryption). For now the URLs
-- are injected via Flyway placeholders sourced from an uncommitted .env file.
INSERT INTO channel_configs (user_id, type, config, enabled) VALUES
    ('11111111-1111-1111-1111-111111111111', 'EMAIL',         '{"address": "ana.ruiz@example.com"}', TRUE),
    ('11111111-1111-1111-1111-111111111111', 'SLACK_WEBHOOK', '{"webhookUrl": "${slackWebhookAna}"}', TRUE),
    ('22222222-2222-2222-2222-222222222222', 'EMAIL',         '{"address": "bruno.costa@example.com"}', TRUE),
    ('33333333-3333-3333-3333-333333333333', 'EMAIL',         '{"address": "carla.mendes@example.com"}', TRUE),
    ('33333333-3333-3333-3333-333333333333', 'SLACK_WEBHOOK', '{"webhookUrl": "${slackWebhookCarla}"}', FALSE),
    ('44444444-4444-4444-4444-444444444444', 'SLACK_WEBHOOK', '{"webhookUrl": "${slackWebhookDiego}"}', TRUE),
    ('55555555-5555-5555-5555-555555555555', 'EMAIL',         '{"address": "elena.ferreira@example.com"}', TRUE);

INSERT INTO alert_rules (user_id, category, criteria, active) VALUES
    ('11111111-1111-1111-1111-111111111111', 'DISASTER',      '{"type": "EARTHQUAKE", "minMagnitude": 3.0}', TRUE),
    ('22222222-2222-2222-2222-222222222222', 'DISASTER',      '{"type": "EARTHQUAKE", "minMagnitude": 4.5}', TRUE),
    ('33333333-3333-3333-3333-333333333333', 'DISASTER',      '{"type": "EARTHQUAKE", "minMagnitude": 6.0, "bbox": {"minLat": 32.5, "minLon": -124.5, "maxLat": 42.0, "maxLon": -114.0}}', TRUE),
    ('44444444-4444-4444-4444-444444444444', 'DISASTER',      '{"type": "EARTHQUAKE", "minMagnitude": 5.0, "bbox": {"minLat": 30.0, "minLon": 129.0, "maxLat": 46.0, "maxLon": 146.0}}', TRUE),
    ('33333333-3333-3333-3333-333333333333', 'MARKET',        '{"symbols": ["AAPL", "TSLA"], "movementThresholdPct": 5.0, "direction": "ANY"}', TRUE),
    ('22222222-2222-2222-2222-222222222222', 'BREAKING_NEWS', '{"keywords": ["earthquake", "tsunami"], "topics": ["world"]}', FALSE);

INSERT INTO events (dedup_key, category, type, source, severity, occurred_at, payload) VALUES
    ('usgs:us7000n7x8', 'DISASTER', 'EARTHQUAKE', 'usgs', 5.2, '2026-08-10T14:23:11Z', '{"magnitude": 5.2, "place": "12km SE of Ridgecrest, CA", "coordinates": [-117.61, 35.58, 8.2], "usgsId": "us7000n7x8"}'),
    ('usgs:us7000n80a', 'DISASTER', 'EARTHQUAKE', 'usgs', 6.4, '2026-08-11T02:14:55Z', '{"magnitude": 6.4, "place": "78km SW of Tokyo, Japan", "coordinates": [139.21, 35.11, 30.0], "usgsId": "us7000n80a"}'),
    ('usgs:us7000n81b', 'DISASTER', 'EARTHQUAKE', 'usgs', 3.1, '2026-08-11T05:40:02Z', '{"magnitude": 3.1, "place": "5km N of Anza, CA", "coordinates": [-116.67, 33.60, 12.4], "usgsId": "us7000n81b"}'),
    ('usgs:us7000n82c', 'DISASTER', 'EARTHQUAKE', 'usgs', 4.7, '2026-08-09T22:10:33Z', '{"magnitude": 4.7, "place": "Andreanof Islands, Aleutian Islands, Alaska", "coordinates": [-176.34, 51.62, 44.0], "usgsId": "us7000n82c"}'),
    ('usgs:us7000n83d', 'DISASTER', 'EARTHQUAKE', 'usgs', 7.1, '2026-08-08T18:55:07Z', '{"magnitude": 7.1, "place": "62km W of Coquimbo, Chile", "coordinates": [-71.98, -30.02, 25.0], "usgsId": "us7000n83d"}'),
    ('usgs:us7000n84e', 'DISASTER', 'EARTHQUAKE', 'usgs', 2.8, '2026-08-10T07:31:48Z', '{"magnitude": 2.8, "place": "10km E of Reno, NV", "coordinates": [-119.69, 39.52, 6.1], "usgsId": "us7000n84e"}'),
    ('usgs:us7000n85f', 'DISASTER', 'EARTHQUAKE', 'usgs', 5.9, '2026-08-09T03:47:22Z', '{"magnitude": 5.9, "place": "128km SW of Padang, Sumatra, Indonesia", "coordinates": [99.71, -1.62, 35.0], "usgsId": "us7000n85f"}'),
    ('usgs:us7000n86g', 'DISASTER', 'EARTHQUAKE', 'usgs', 4.2, '2026-08-10T20:05:59Z', '{"magnitude": 4.2, "place": "26km S of Pinotepa Nacional, Oaxaca, Mexico", "coordinates": [-98.05, 16.11, 18.0], "usgsId": "us7000n86g"}'),
    ('usgs:us7000n87h', 'DISASTER', 'EARTHQUAKE', 'usgs', 6.0, '2026-08-07T12:38:14Z', '{"magnitude": 6.0, "place": "40km W of Kushiro, Japan", "coordinates": [143.85, 42.98, 55.0], "usgsId": "us7000n87h"}'),
    ('usgs:us7000n88i', 'DISASTER', 'EARTHQUAKE', 'usgs', 3.5, '2026-08-11T01:09:37Z', '{"magnitude": 3.5, "place": "18km ENE of West Yellowstone, WY", "coordinates": [-110.87, 44.72, 4.8], "usgsId": "us7000n88i"}');

INSERT INTO notification_deliveries (event_id, user_id, channel_type, status, attempts, error, sent_at) VALUES
    ((SELECT id FROM events WHERE dedup_key = 'usgs:us7000n80a'), '11111111-1111-1111-1111-111111111111', 'EMAIL',         'SENT',          1, NULL,                                              '2026-08-11T02:15:03Z'),
    ((SELECT id FROM events WHERE dedup_key = 'usgs:us7000n80a'), '11111111-1111-1111-1111-111111111111', 'SLACK_WEBHOOK', 'SENT',          1, NULL,                                              '2026-08-11T02:15:04Z'),
    ((SELECT id FROM events WHERE dedup_key = 'usgs:us7000n80a'), '44444444-4444-4444-4444-444444444444', 'SLACK_WEBHOOK', 'SENT',          1, NULL,                                              '2026-08-11T02:15:05Z'),
    ((SELECT id FROM events WHERE dedup_key = 'usgs:us7000n83d'), '11111111-1111-1111-1111-111111111111', 'EMAIL',         'SENT',          1, NULL,                                              '2026-08-08T18:55:19Z'),
    ((SELECT id FROM events WHERE dedup_key = 'usgs:us7000n83d'), '22222222-2222-2222-2222-222222222222', 'EMAIL',         'SENT',          1, NULL,                                              '2026-08-08T18:55:20Z'),
    ((SELECT id FROM events WHERE dedup_key = 'usgs:us7000n83d'), '11111111-1111-1111-1111-111111111111', 'SLACK_WEBHOOK', 'FAILED',        2, 'Slack webhook returned HTTP 500',                 NULL),
    ((SELECT id FROM events WHERE dedup_key = 'usgs:us7000n85f'), '22222222-2222-2222-2222-222222222222', 'EMAIL',         'DEAD_LETTERED', 5, 'SMTP connection refused after 5 retries',         NULL),
    ((SELECT id FROM events WHERE dedup_key = 'usgs:us7000n87h'), '44444444-4444-4444-4444-444444444444', 'SLACK_WEBHOOK', 'PENDING',       0, NULL,                                              NULL);
