
# Biz-Engine — Phase 14 Foundation

Initial Spring Boot modular-monolith foundation for Biz-Engine.

## Requirements
- Java 21
- Maven 3.9+
- Docker Engine + Docker Compose (for local PostgreSQL)

## Start PostgreSQL
```bash
cp .env.example .env
docker compose up -d postgres
```
Defaults are for local development only. Do not use them in shared or production environments.

## Run
```bash
mvn spring-boot:run
```
Flyway applies database migrations at startup.

## Verify
- `GET http://localhost:8080/api/v1/system/status`
- `GET http://localhost:8080/actuator/health`
- `GET http://localhost:8080/actuator/health/readiness`
- `GET http://localhost:8080/actuator/health/liveness`

## Test
```bash
mvn test
```

Code is organized by business capability, not one global controller/service/repository package. Partner credentials, request-signature authentication, event ingestion, outbox, Kafka and AI orchestration are subsequent implementation slices.

[//]: # (continue...)

Where to put the Public Key File
Your configuration specifies classpath:certs/public.der. You should place this file inside your standard Maven or Gradle resources directory.
Create a folder named certs inside your src/main/resources directory and put your key file there:
text
src/
└── main/
├── java/
│   └── com/superapp/bizengine/...
└── resources/             <-- Standard resources folder
├── certs/             <-- Create this folder
│   └── public.der     <-- Put your public key file here
└── application.properties
Use code with caution.
Quick Checklist for your public.der file:
1. Format: The code snippet expects a binary DER formatted public key (not a plain text PEM file starting with -----BEGIN PUBLIC KEY-----).
2. Generation: If you have an RSA PEM public key file (public.pem), you can easily convert it to a binary DER format using your terminal:bash
   openssl rsa -pubin -in public.pem -outform DER -out public.der
   Use code with caution.
   Once the file is placed in src/main/resources/certs/public.der, Spring’s built-in ResourceLoader will automatically locate it at runtime when your application boots up.
   Would you like help generating an RSA public/private keypair using OpenSSL, or would you like to see how to adjust the Java code so that it can read a standard text-based PEM file instead of a binary DER file?

[//]: # (Infrastructure)


11.2 Spring Boot project structure

I would use a structure close to:

src/main/java/com/bizengine/

    BizEngineApplication.java

    api/
        controller/
        dto/
        mapper/
        exception/

    partner/
        Partner.java
        PartnerService.java
        PartnerRepository.java

    application/
        Application.java
        ApplicationService.java

    identity/
        Identity.java
        Identifier.java
        IdentityService.java

    entity/
        Entity.java
        EntityService.java

    event/
        Event.java
        EventType.java
        EventService.java
        EventIngestionService.java

    context/
        Context.java
        ContextService.java

    geography/
        Geography.java
        GeographyService.java

    knowledge/
        Observation.java
        Signal.java
        Interest.java
        Affinity.java
        Trend.java

    intelligence/
        Insight.java
        Inference.java
        Prediction.java
        IntelligenceService.java

    recommendation/
        Recommendation.java
        RecommendationService.java

    policy/
        Policy.java
        PolicyVersion.java
        PolicyService.java

    decision/
        Decision.java
        DecisionService.java

    outcome/
        Outcome.java
        OutcomeService.java

    feedback/
        Feedback.java
        FeedbackService.java

    provenance/
        Evidence.java
        Provenance.java
        Source.java
        Model.java

    ai/
        AiGateway.java
        AiTaskService.java
        AiResultValidator.java

    query/
        IntelligenceQueryService.java

    infrastructure/
        persistence/
        kafka/
        redis/
        search/
        graph/
        vector/

This maps closely to the logical modules established earlier rather than inventing a new architecture.

11.3 Keep API DTOs separate from domain objects

This is important.

Do not make your PostgreSQL/JPA entities your public API contract.

Bad:

@PostMapping("/events")
public Event save(@RequestBody Event event)

Instead:

@PostMapping("/events")
public EventResponse createEvent(
@RequestBody CreateEventRequest request)

Then:

HTTP Request
↓
DTO
↓
Validation
↓
Application Service
↓
Domain Model
↓
Persistence

This lets the internal model evolve without breaking every partner.

11.4 API layer

The initial controllers can be organized around the Phase 10 contract:

PartnerController

ApplicationController

IdentityController
EntityController

[//]: # (Storage Choice)

    Storage Choice decision

    Put this at the end of your document:

        Phase 8 — Locked Technology Choices

        Biz-Engine will use a polyglot storage architecture in which each storage technology serves a defined workload rather than acting as a competing system of record.

            PostgreSQL — authoritative transactional and domain state; flexible entities via JSONB.

            Apache Kafka — durable event streaming and asynchronous event distribution.

            ClickHouse — high-volume event storage and real-time analytics.

            Redis — disposable hot-state/cache layer.

            Elasticsearch — search-optimized projection of Biz-Engine data.

            Neo4j — relationship/knowledge graph capabilities.

            Qdrant — vector embeddings and semantic similarity search.

            Google BigQuery — historical and large-scale analytical workloads.

            Google Cloud Storage — large immutable artifacts, datasets, exports and archives.

            Cold archival storage — deferred until retention volume and compliance requirements justify it.

        PostgreSQL remains the primary authoritative system of record. Secondary stores such as Elasticsearch, Qdrant, Neo4j, Redis and analytical stores should generally be treated as projections, indexes, caches or specialized representations of canonical Biz-Engine information.

        The physical storage technologies remain independent of the canonical Biz-Engine domain model.

    This is the version we carried forward into Phase 9 / ERD.

    And there's one architectural consequence worth emphasizing: Phase 9 ERD primarily model PostgreSQL, not attempt to include Kafka, ClickHouse, Elasticsearch, Qdrant and Neo4j into one giant relational diagram. Those will have their own schemas/projections later.

[//]: # (Project / Package structure)

Now: moving into actual code

This is where I agree with your direction.

We should stop producing phases that are only conceptual. The architecture has now reached the point where the next work should progressively become the actual repository.

I would start the implementation with the Spring Boot Biz-Engine modular monolith, not microservices.

13.28 Initial repository
biz-engine/
├── pom.xml
├── README.md
├── .gitignore
├── docker-compose.yml
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── bizengine/
│   │   │           └── BizEngineApplication.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/
│   │           └── migration/
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── bizengine/
│
└── ai/
└── ...

I would use a root package such as:

com.bizengine

and then organize by business capability, rather than by generic technical layers.

13.29 Package architecture

The first version should look approximately like:

com.bizengine
│
├── BizEngineApplication
│
├── partner
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── authentication
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── authorization
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── entity
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── identity
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── event
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── context
├── geography
├── observation
├── signal
├── interest
├── trend
├── insight
├── inference
├── prediction
├── recommendation
├── policy
├── decision
├── action
├── outcome
├── feedback
├── provenance
│
├── ai
│   ├── application
│   ├── domain
│   └── infrastructure
│
├── query
│
├── privacy
├── retention
├── audit
├── governance
│
├── infrastructure
│   ├── persistence
│   ├── messaging
│   ├── security
│   ├── observability
│   └── configuration
│
└── shared
├── domain
├── error
└── web

This is deliberately not:

controller/
service/
repository/
entity/

at the root.

That traditional arrangement becomes difficult to manage once Biz-Engine grows.

13.30 First actual Java file

src/main/java/com/bizengine/BizEngineApplication.java

package com.bizengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BizEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizEngineApplication.class, args);
    }
}

That's intentionally boring.

The intelligence belongs in the modules, not in the application bootstrap class.

13.31 First pom.xml

We should establish the foundation before adding dozens of dependencies.

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="
http://maven.apache.org/POM/4.0.0
https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>

    <groupId>com.bizengine</groupId>
    <artifactId>biz-engine</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <name>biz-engine</name>
    <description>Biz-Engine intelligence platform</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>

        <!-- Web/API -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Persistence -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Database migrations -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <!-- Tests -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>
                    org.springframework.boot
                </groupId>
                <artifactId>
                    spring-boot-maven-plugin
                </artifactId>
            </plugin>
        </plugins>
    </build>

</project>

One caveat: the exact Spring Boot version should be pinned against the version we choose for the repository rather than blindly following this illustrative skeleton. Before we generate the complete build, we should lock the current dependency versions together.

13.32 Configuration

src/main/resources/application.yml

spring:

application:
name: biz-engine

datasource:
url: ${DB_URL:jdbc:postgresql://localhost:5432/biz_engine}
username: ${DB_USERNAME:biz_engine}
password: ${DB_PASSWORD:biz_engine}

jpa:
open-in-view: false
hibernate:
ddl-auto: validate

flyway:
enabled: true

kafka:
bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

data:
redis:
host: ${REDIS_HOST:localhost}
port: ${REDIS_PORT:6379}

server:
port: ${SERVER_PORT:8080}

management:
endpoints:
web:
exposure:
include:
- health
- info
- metrics

endpoint:
health:
probes:
enabled: true

Notice:

ddl-auto: validate

not:

ddl-auto: create

or:

ddl-auto: update

because database schema belongs to migrations, not Hibernate silently changing production structure.

13.33 First module: Partner

Before implementing every domain, we should create the first complete vertical slice.

That slice should be:

Partner
↓
Authentication
↓
Event
↓
PostgreSQL
↓
Outbox
↓
Kafka

Then we progressively add intelligence.

The first package:

com.bizengine.partner

with:

partner/
├── api/
│   └── PartnerController.java
│
├── application/
│   └── PartnerService.java
│
├── domain/
│   ├── Partner.java
│   └── PartnerRepository.java
│
└── infrastructure/
└── persistence/
13.34 Domain model

Partner.java
...
controller

Now we have our first actual flow:

HTTP
↓
Controller
↓
DTO validation
↓
Application service
↓
Domain
↓
Repository
↓
PostgreSQL

That's the pattern we should use throughout Biz-Engine.


[//]: # (Implementation phase / featre/logic after logic)

13.39 But we should NOT build everything this way blindly

There is an important distinction.

Some components are straightforward CRUD:

Partner
Application
Domain
Department
Policy

Others are processing pipelines:

Event
Observation
Signal
Insight
AI
Recommendation
Decision
Feedback

Those need richer domain/application logic.

So we should build the system vertical slice by vertical slice, not table by table.

13.40 First implementation milestones

I recommend this exact sequence now:

Milestone A — Foundation
Spring Boot
PostgreSQL
Flyway
Docker
configuration
health checks
global errors
structured logging
Milestone B — Partner
Partner
Application
Credential
authentication
Milestone C — Event ingestion
Event
EventType
Context
idempotency
transactional outbox
Kafka
Milestone D — Entity/Identity
Entity
Identity
Identifier
identity resolution
Milestone E — Intelligence
Observation
Signal
Interest
Insight
Evidence
Provenance
Milestone F — AI
AI Gateway
Python/Flask service
task contract
candidate intelligence
validation
Milestone G — Decision loop
Recommendation
Policy
Decision
Action
Outcome
Feedback
Milestone H — Secondary infrastructure
Redis
Elasticsearch
ClickHouse
Neo4j
Qdrant

only as the corresponding workload requires it.

13.41 This also changes how we do the remaining phases

Previously:

Phase 12
↓
architecture explanation

Going forward:

Phase
↓
architecture decision
↓
package structure
↓
files
↓
classes/interfaces
↓
database migration
↓
configuration
↓
tests
↓
integration

So, for example, when we reach the next authentication implementation, I won't just say:

"We need public-key authentication."

We'll produce:

authentication/
├── api/
│   └── AuthenticationController.java
├── application/
│   └── AuthenticationService.java
├── domain/
│   ├── PartnerCredential.java
│   └── CredentialRepository.java
├── infrastructure/
│   ├── SignatureVerifier.java
│   └── ...
└── ...

plus the SQL migration, DTOs, configuration, tests, and integration with Spring Security.

13.42 One important correction to the development approach

We should not attempt to code the entire Phase 9 ERD immediately.


# Biz-Engine — Phase 17: Authorization and tenant isolation

Phase 17 extends the Phase 14–16 foundation with permission and scope-based authorization, explicit deny rules, and tenant-isolation checks. The Phase 16 Ed25519 signed-request authentication remains included.

## Included

- Partner credentials with key ID, Ed25519 public-key PEM, status, and lifecycle timestamps
- Flyway migration `V2__create_partner_authentication_tables.sql`
- Canonical request signing contract with body SHA-256 hash
- Timestamp freshness validation and random nonce validation
- Ed25519 signature verification against registered active credentials
- PostgreSQL-backed, atomic nonce uniqueness to reject replayed requests
- Periodic nonce cleanup after the accepted replay window
- Bounded request-body caching (default 1 MiB) so the signed body bytes match the body received by the controller
- Authenticated partner request context
- Basic safe operation boundary: a partner can read only its own partner record; partner registration, listing, and status changes require an operator and are denied to partner credentials
- Documentation and Python signing example in `docs/partner-authentication.md`
- Permission vocabulary and scope-based partner access grants
- Fail-closed `PartnerAuthorizationService`, with cross-tenant denial and deny-overrides-allow
- Flyway migration `V3__create_partner_access_grants.sql`
- Authorization policy documentation in `docs/authorization-and-tenancy.md`
- Tests for canonical message format and Ed25519 URL-safe signature encoding

## Required headers

Every request to `/api/v1/partners/**` must send:

- `X-Biz-Partner-Id`
- `X-Biz-Key-Id`
- `X-Biz-Timestamp` (Unix seconds)
- `X-Biz-Nonce` (fresh URL-safe random value)
- `X-Biz-Signature` (URL-safe Base64 without padding)

See [the authentication guide](docs/partner-authentication.md) for the canonical message, key generation, provisioning, signing sample, rotation, and revocation. See [authorization and tenancy](docs/authorization-and-tenancy.md) for grant semantics and integration requirements. The authorization service is ready for use by the event API in Phase 18; it is not a substitute for operator identity or policy administration.

## Provisioning model

Credential-enrollment endpoints are deliberately not exposed yet. In Phase 16, an operator provisions a partner and its public key through a controlled internal database/deployment workflow. The partner private key never leaves the partner's secure environment. Phase 17 will implement broader operator authorization and tenant policy before partner management operations are opened.

## Run locally

Requirements: Java 21, Maven 3.9+, Docker.

```bash
cp .env.example .env
docker compose up -d postgres
mvn test
mvn spring-boot:run
```

The `/api/v1/system/status` endpoint remains public. Partner endpoints require a provisioned credential and a valid signature. The default local DB credentials are for local development only.

## Configuration

- `BIZ_PARTNER_AUTH_ENABLED` (default `true`; disable only for isolated tests)
- `BIZ_PARTNER_CLOCK_SKEW_SECONDS` (default `300`)
- `BIZ_PARTNER_MAX_BODY_BYTES` (default `1048576`)
- `BIZ_PARTNER_NONCE_CLEANUP_DELAY_MS` (default `60000`)

## Important production notes

- Require HTTPS/TLS in every non-local environment.
- Protect partner private keys with a secret manager or hardware-backed key storage where appropriate.
- Provision/revoke keys through audited operator workflows; do not expose a public credential enrollment endpoint.
- Never log private keys, request signatures, or sensitive request bodies.
- The Phase 16 filter protects `/api/v1/partners/**`. Any future partner-facing routes must be added to the authentication policy before release.
- Authentication proves which partner signed the request; it does not by itself authorize every operation. The filter currently allows partners to read only their own partner record and denies partner-management operations. Phase 17 expands authorization, tenant isolation, and policy enforcement.

## Verification status

Java 21 is available in the authoring environment, but Maven is not installed, so the Maven build and tests could not be executed here. The archive is structurally checked; run `mvn test` locally before deployment.


## Phase 18 — Event ingestion

The project now includes `POST /api/v1/events`. It requires Phase 16 signed partner authentication and an exact application-scope `EVENT_INGEST` grant. Include an `Idempotency-Key` header. PostgreSQL stores the event and guarantees one event per partner/key.

See [`docs/event-ingestion.md`](docs/event-ingestion.md) for request/response examples and controlled authorization provisioning. Kafka/outbox publication is intentionally deferred to Phase 19.


## Phase 19 — Transactional outbox and Kafka

Phase 18 event ingestion now creates a `biz_outbox_event` row in the same PostgreSQL transaction as the canonical `biz_event` row. If either insert fails, the transaction rolls back both writes. A scheduled dispatcher publishes pending outbox messages to Kafka topic `biz.events.v1` and marks them published only after Kafka acknowledges the send.

### Local services

```bash
cp .env.example .env
docker compose up -d postgres kafka
mvn test
mvn spring-boot:run
```

Kafka is exposed locally at `localhost:9092`. The Compose Kafka configuration is for local development only; production requires a secured, replicated Kafka cluster and appropriate authentication, encryption, ACLs, and monitoring.

### Delivery guarantees

- PostgreSQL is authoritative; Kafka is transport.
- Outbox publication is **at-least-once**, not exactly-once end-to-end. If Kafka accepts a message and the process crashes before PostgreSQL records `PUBLISHED`, the message can be sent again after restart.
- Every message has a stable `eventId` and outbox ID. Consumers must be idempotent and persist their processed-message key with their own effects in one transaction.
- Failures retry with bounded exponential backoff. After `BIZ_OUTBOX_MAX_ATTEMPTS`, the dispatcher attempts to forward the message to `biz.events.dlq`. If the DLQ is unavailable, the outbox row remains pending and retries.
- A successful DLQ send marks the row `DEAD`; operators should inspect the DLQ, fix the cause, and replay through a controlled process.

### Configuration

- `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`)
- `BIZ_OUTBOX_ENABLED` (default `true`)
- `BIZ_OUTBOX_POLL_DELAY_MS` (default `1000`)
- `BIZ_OUTBOX_BATCH_SIZE` (default `50`)
- `BIZ_OUTBOX_SEND_TIMEOUT_MS` (default `3000`)
- `BIZ_OUTBOX_MAX_ATTEMPTS` (default `10`)
- `BIZ_OUTBOX_BASE_BACKOFF_MS` (default `1000`)
- `BIZ_OUTBOX_MAX_BACKOFF_MS` (default `300000`)
- `BIZ_EVENT_TOPIC` (default `biz.events.v1`)
- `BIZ_EVENT_DLQ_TOPIC` (default `biz.events.dlq`)

### Migration

`V5__create_transactional_outbox.sql` creates the durable outbox table and indexes. Flyway applies it after the existing partner, credential, authorization, and event migrations.

### Verification status

Java 21 is available in the authoring environment, but Maven is not installed here, so the Maven build and test suite could not be executed. Run `mvn test` before relying on this as a deployable build. The local Kafka Compose setup also has not been launched in this environment.


## Phase 20 — What changed

This phase extends Phase 19 with the first consumer-side intelligence data foundation. Accepted events remain immutable operational facts; downstream processing adds an entity link where a partner-scoped subject reference exists, records normalized context, and creates one observation per event.

### Added components

- `EntityResolutionService`: creates or resolves an entity by `(partner_id, application_id, entity_type, external_reference)`. It deliberately does not merge across partners.
- `IdentifierHashService` and `IdentifierRegistryService`: normalize and HMAC-SHA256 hash identifiers so raw email/phone values are not stored in `biz_identifier`.
- `ContextNormalizer`: conservatively preserves source context fields and normalizes selected fields such as country code and surface.
- `ObservationProcessingService`: persists `biz_context` and `biz_observation` in a transaction, with unique event IDs to make replay safe.
- `EventObservationKafkaListener`: consumes `biz.events.v1` and processes the event envelope.
- `KafkaConsumerConfiguration`: retries failures and publishes exhausted messages to `biz.events.v1.DLQ` by default.
- Migration `V6__create_entity_identity_context_observation.sql`.

### Data flow

`biz_event` + `biz_outbox_event` → Kafka `biz.events.v1` → resolve/create scoped entity → normalize context → persist context + observation.

The original event and its context are not overwritten. Missing subject references result in observations without an entity link; they are not treated as processing failures. Duplicate Kafka deliveries do not create duplicate observations.

### Important identity and privacy limits

- External subject references are only comparable inside the same partner/application/entity-type scope.
- Identifier hashes are keyed HMAC-SHA256 values. Set `BIZ_IDENTITY_HASH_SECRET` to a strong, stable secret of at least 32 characters and store it in a secret manager outside local development. Do not casually rotate it: identifier lookups depend on the same key. A versioned key-rotation workflow should be implemented before planned rotation.
- Identifier registration does not imply verification. Records start as `UNVERIFIED`; matching an identifier is not proof that a real-world identity has been verified.
- No automatic cross-partner identity merging or probabilistic name matching is implemented.

## Run locally

Requirements: Java 21, Maven 3.9+, Docker.

```bash
cp .env.example .env
docker compose up -d postgres kafka
mvn test
mvn spring-boot:run
```

`mvn test` should be run before relying on the code. Maven was not available in the generation environment, so this archive has not been Maven-built or test-executed there. The archive can be structurally checked, but that is not a substitute for compiling and running the tests.

## Next phase

Phase 21 should introduce deterministic signal generation from observations, with explicit scope, recency, evidence, provenance, and deduplication. It should not introduce AI-generated conclusions as authoritative facts.

## Phase 22 — Aggregation and trend detection

Phase 22 adds deterministic, explainable trend snapshots over Phase 21 signals. The snapshot table is a rebuildable read model; `biz_signal` remains the evidence source.

### Aggregation behavior

- Supported windows: 1 hour, 24 hours, 7 days (168 hours), and 30 days (720 hours).
- Each snapshot compares the current window with the immediately preceding window of the same length.
- Counts are signal-record counts, not unique people. `current_distinct_entities` is reported separately and excludes signals without an entity link.
- Weighted strength applies each signal's stored base strength and half-life decay at snapshot time. The historical signal remains available as evidence even if its TTL has passed; TTL does not erase historical activity from window counts.
- Direction (`UP`, `DOWN`, `STABLE`) compares current and previous signal counts. `change_ratio` is null when the previous count is zero, avoiding a misleading infinite percentage.
- Snapshots store window boundaries and `algorithm_version=1` for auditability. They can be rebuilt from PostgreSQL signals.

### API

`GET /api/v1/trends?windowHours=24&limit=50` returns the authenticated partner's latest partner-scoped trend snapshots. Only the authenticated partner's own `PARTNER` scope is exposed by this endpoint. Supported `windowHours` values are `1`, `24`, `168`, and `720`; `limit` must be 1–100. The route requires the Phase 16 Ed25519 signed-request headers. No cross-partner trend endpoint is exposed.

### Scheduling and migration

Migration `V8__create_trend_aggregation.sql` creates the snapshot table and indexes. A scheduled job recalculates all supported windows every 15 minutes by default. Configure with `BIZ_TREND_AGGREGATION_ENABLED` and `BIZ_TREND_AGGREGATION_DELAY_MS`. The first scheduled run may have no results if there are no persisted signals in the lookback period.

### Interpretation limits

Trend direction is descriptive, not causal. Small sample sizes can produce noisy changes; this phase does not yet add minimum sample thresholds, seasonality correction, anomaly detection, regional/country aggregation, or privacy thresholds for population-level outputs. Those controls should be introduced before exposing broader scopes.

### Verification status

The archive is structurally checked and the Maven configuration is valid XML. Maven is not installed in the authoring environment, so Java compilation, database migrations, and tests were not executed here. Run `mvn test` and exercise migrations against a fresh PostgreSQL database before deployment.


## Phase 23 — Insights and evidence

Phase 23 adds deterministic, explainable insight generation from the latest eligible partner-scope trend snapshots. It introduces `biz_insight_rule`, `biz_insight`, and `biz_insight_evidence` in migration `V9__create_insights_and_evidence.sql`. The generation job evaluates the newest eligible snapshot per partner/signal/window, deduplicates insight records, records trend evidence, supersedes opposite-direction insights, and expires stale insights.

### API

`GET /api/v1/insights?status=ACTIVE&limit=50` requires the standard signed partner request. The authenticated partner ID is taken from server-side request context. The endpoint returns only that partner's own insights and includes supporting trend snapshots and source references through signals, observations, and originating events. `status` may be `ACTIVE`, `EXPIRED`, or `SUPERSEDED`; `limit` must be 1–100.

### Interpretation and confidence

Initial rules require at least five current-window signals and three distinct entities. These are starter thresholds, not validated statistical criteria. The confidence field is a deterministic evidence-sufficiency score—not a probability, causal claim, or significance test. Rule and score thresholds should be validated on domain data before business-critical use.

### Configuration

- `BIZ_INSIGHT_GENERATION_ENABLED` (default `true`)
- `BIZ_INSIGHT_GENERATION_DELAY_MS` (default `900000`, 15 minutes)

See [`docs/insights-and-evidence.md`](docs/insights-and-evidence.md) for lifecycle, evidence, API, and limitations.

## Verification status for Phase 23

The project archive and basic source/configuration structure were checked. Maven compilation, automated tests, Flyway migrations against PostgreSQL, and Kafka-backed integration tests were not run in this authoring environment. Run `mvn test` and start PostgreSQL/Kafka locally before treating this phase as verified.


## Phase 24 — Recommendations

Phase 24 adds deterministic, explainable recommendations based on active insights. Recommendations are review prompts, not commands or autonomous actions.

### Included

- Migration `V10__create_recommendations.sql` for the recommendation lifecycle and indexes.
- `RecommendationGenerationService` evaluates active insights with confidence at least `0.55` and MEDIUM/HIGH evidence quality.
- Rule outputs include an objective, title, rationale, suggested action, priority, confidence, source insight, and expiry.
- Recommendation generation is idempotent on `(partner_id, source_insight_id, recommendation_code)`. Existing `ACCEPTED` or `DISMISSED` states are preserved when the same recommendation is reevaluated.
- `GET /api/v1/recommendations?status=PROPOSED&limit=50` returns only the authenticated partner's own recommendations and requires `RECOMMENDATION_READ` at the exact partner scope.
- Partner authentication filter now covers `/api/v1/recommendations`.

### Important boundaries

- A recommendation does not authorize or execute an action. Partner systems retain control over app-specific execution.
- The rule set is a starter implementation. Priority thresholds and suggested actions are illustrative and should be calibrated against real outcomes before production use.
- This phase does not yet expose endpoints for accepting/dismissing recommendations, policy evaluation, decisions, action execution, or outcome feedback. Those belong to subsequent phases.

### Run locally

```bash
cp .env.example .env
docker compose up -d postgres kafka
mvn test
mvn spring-boot:run
```

### Verification status

The archive can be structurally checked, but Maven compilation, automated tests, and Flyway migration execution have not been run in this environment. Run `mvn test` and start the local services before relying on this as a deployable build.



