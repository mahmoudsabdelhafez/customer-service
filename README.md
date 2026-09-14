# Customer Service

Manages bank customers: registration, retrieval, listing and update. One half of a
pair of co-dependent microservices — the other, the account service, owns accounts
and is a separate deployable with its own database.

Built with Java 21, Spring Boot 4.1.1, Spring Data JPA, Spring Security,
PostgreSQL, Flyway and Kafka.

**Contents:** [Running it](#running-it) · [Project structure](#project-structure) ·
[API](#api) · [Design decisions](#design-decisions) · [Events](#events) ·
[Testing](#testing) · [Assumptions](#assumptions) · [Shortcomings](#shortcomings)

---

## Running it

**Prerequisites:** JDK 21, PostgreSQL with a `customer_db` database. Kafka is
optional — see [Events](#events).

```bash
# Runs with the dev profile by default: local Postgres/Kafka, no env vars needed.
./mvnw spring-boot:run

# Tests plus the coverage report (target/site/jacoco/index.html)
./mvnw clean verify
```

Flyway creates the schema on first start; there is no `ddl-auto: update` anywhere.

| URL | What it is |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI 3 specification |

### Profiles

`spring.profiles.default` is `dev`, so running with no `-Dspring.profiles.active`
still works out of the box.

| Profile | Datasource | Logging |
|---|---|---|
| `dev` (default) | localhost, with sensible defaults if unset | `DEBUG` + SQL logging |
| `prod` | no defaults — `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` are required | `WARN`, no SQL logging |

`prod`'s missing defaults are deliberate: a deployment that forgets `DB_PASSWORD`
fails to start with a clear error instead of silently running against whatever
the default would have been.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
# -> fails fast: 'url' must start with "jdbc" — DB_URL was never set
```

### Security

Every endpoint requires HTTP Basic auth except Swagger. Two roles:

| User | Password | Can |
|---|---|---|
| `viewer` | `viewer` (dev default) | `GET` only |
| `admin` | `admin` (dev default) | `GET`, `POST`, `PUT` |

```bash
curl -u admin:admin -X POST http://localhost:8080/api/v1/customers -H "Content-Type: application/json" -d '{...}'
curl -u viewer:viewer http://localhost:8080/api/v1/customers/1000001
```

In Swagger UI, click **Authorize** (top right) and enter a username/password —
every "Try it out" call then carries it automatically. A request with no
credentials gets `401`; `viewer` attempting a write gets `403`.

### Configuration

Every setting has a working local default and can be overridden by an
environment variable.

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/customer_db` (dev only) |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `54321` (dev only) |
| `VIEWER_PASSWORD` / `ADMIN_PASSWORD` | `viewer` / `admin` (dev only) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `SERVER_PORT` | `8080` |

---

## Project structure

```
com.bank.customer
├── controller/            CustomerController
├── dto/                   Request/response records, kept separate from the entity
├── service/               CustomerService — use cases, transactions, the entity/DTO boundary
├── event/                 CustomerEvent, CustomerEventPublisher — the Kafka producer
├── exception/             CustomerNotFoundException, DuplicateLegalIdException
│   └── handler/           GlobalExceptionHandler
├── persistence/
│   ├── entity/            Customer, Address, CustomerType
│   └── repository/        CustomerRepository — Spring Data JPA
└── config/                OpenApiConfig, SecurityConfig
```

Each package has one job; `service` is the only layer that knows both the entity
and the API contract. `db/migration/` holds the Flyway SQL that owns the schema.

---

## API

| Method | Path | Returns |
|---|---|---|
| `POST` | `/api/v1/customers` | `201` + `Location`, `400`, `409` |
| `GET` | `/api/v1/customers/{id}` | `200`, `404` |
| `GET` | `/api/v1/customers?type=RETAIL&page=0&size=20` | `200` |
| `PUT` | `/api/v1/customers/{id}` | `200`, `400`, `404` |

Errors are RFC 9457 `application/problem+json`, produced in one place by
`GlobalExceptionHandler`. A validation failure lists every rejected field at once:

```json
{
  "status": 400,
  "title": "Validation failed",
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/customers",
  "errors": {
    "name": "name must be between 3 and 150 characters",
    "address.country": "country must be an ISO 3166-1 alpha-2 code, e.g. EG"
  }
}
```

---

## Design decisions

### The 7-digit customer id

The requirement is that a customer id is 7 digits, and that an account number is
10 digits beginning with those 7. The id is therefore not a UUID and not an
unbounded sequence — it is drawn from a Postgres sequence bounded to
`1000000..9999999`:

```sql
CREATE SEQUENCE customer_id_seq START WITH 1000000 MINVALUE 1000000 MAXVALUE 9999999 NO CYCLE;
```

The bound is also a `CHECK` constraint on the table. The sequence guarantees it
for rows this application writes; the constraint guarantees it for every other
writer — a migration script, a DBA, a future service. Exhausting the range is a
loud failure (the sequence refuses to cycle) rather than a silent wrap into
6-digit ids that would corrupt account numbers.

### Accounts are not modelled here

`Customer` has no `accounts` collection, no foreign key and no shared table. The
rules "up to 10 accounts", "one salary account, rest saving or investment" and
the account number format all belong to the account service, which references a
customer by id alone. That is what makes the two services independently
deployable; a JPA relationship between them would make them one service wearing
two hats.

### Validation in three layers

1. **DTO** — `@Valid` on the request body. Rejects bad input before any business
   code runs and produces the field-by-field `400` above.
2. **Entity** — the same constraints on `Customer`. Protects the domain from
   callers that are not the REST API, such as an event consumer added later.
3. **Database** — `NOT NULL`, `UNIQUE (legal_id)`, and `CHECK` constraints on the
   id range and the customer type. The last line of defence, and the only one
   that also applies to writers outside this application.

The DTO and the entity are separate types on purpose: the published API contract
should not change every time the persistence model does.

### Legal id is the natural key

It is unique and `updatable = false`, and `UpdateCustomerRequest` simply has no
`legalId` field — the immutable thing is absent from the update contract rather
than accepted and then rejected. Registration pre-checks it with
`existsByLegalId` to return a clean `409`, and the unique constraint catches the
race where two requests arrive at once (handled as a `409` too).

### Address as an embeddable record

`Address` is an `@Embeddable` Java record, so its columns live inline on
`customers` instead of in a join table it would never share with anything. It has
no identity of its own, which is exactly what `@Embeddable` means.

### Transactions

`CustomerService` is `@Transactional(readOnly = true)` at class level, and the two
writing methods opt back in. Read paths cannot accidentally run in a write
transaction. `open-in-view` is off, so the persistence context closes at the end
of the service call and lazy-loading surprises cannot leak into the web layer.

---

## Events

The service publishes to the `customer-events` Kafka topic, keyed by customer id
so all events for one customer keep their order on a single partition:

```json
{
  "eventType": "CUSTOMER_CREATED",
  "customerId": 1000001,
  "name": "Mahmoud Abdelhafez",
  "type": "RETAIL",
  "occurredAt": "2026-09-12T13:18:56.365659Z"
}
```

`eventType` is `CUSTOMER_CREATED` or `CUSTOMER_UPDATED`.

This is what makes the pair event-driven rather than chatty. The account service
consumes `CUSTOMER_CREATED` and keeps its own record that customer 1000001
exists, so opening an account does not require a synchronous HTTP call back here
— and still works while this service is down.

**Publishing never fails or delays a request.** Registering a customer is the
business operation; announcing it is a side effect, and a side effect must not be
able to break or slow down its cause.

`KafkaTemplate.send` is less asynchronous than it looks: with no reachable broker
it blocks for `max.block.ms` waiting for cluster metadata before failing. Left on
the request thread that turned every write into a one-minute hang. So the send
runs on the application executor (virtual threads) instead, `max.block.ms` is cut
to 5s, and both failure modes — `send` throwing immediately, and the returned
future failing later — are caught and logged.

The service therefore runs perfectly well with no broker at all, which is why
Kafka is optional in development.

---

## Testing

```
./mvnw clean verify      # 49 tests, 91% line coverage, fails below 70%
```

JaCoCo enforces the 70% line coverage the task asks for; the build fails below
it. Coverage report: `target/site/jacoco/index.html`.

| Test | Scope |
|---|---|
| `CustomerValidationTest` | Bean Validation rules, no Spring context |
| `CustomerRepositoryTest` | `@DataJpaTest` against the **real Flyway migration** on H2 |
| `CustomerServiceTest` | Business rules with the repository and publisher mocked |
| `CustomerServiceIntegrationTest` | The service against a real database and real transactions |
| `CustomerControllerTest` | `@WebMvcTest`: status codes, JSON shape, validation, and role-based access |
| `CustomerEventPublisherTest` | What lands on the topic, and both broker failure modes |
| `CustomerServiceApplicationTests` | The whole context starts |

Two things worth pointing at: the repository tests run the production migration
rather than letting Hibernate invent a schema, and `ddl-auto: validate` means an
entity that drifts away from that migration **fails the build** instead of
failing in production.

---

## Assumptions

- **A customer has one name, not first/last.** Corporate and investment customers
  have a single registered name.
- **`legalId` means national id for retail customers and commercial registration
  number for the others.** Formats vary by country, so it is validated as
  alphanumeric-with-hyphens rather than against one national scheme.
- **Email and phone are optional.** Not every onboarding channel captures them.
  Email is deliberately *not* unique: sharing a household or corporate address is
  legitimate and no requirement says otherwise.
- **`country` is an uppercase ISO 3166-1 alpha-2 code.** Rejected rather than
  normalised, so the stored value is predictable.
- **No customer lifecycle status.** The task specifies a status for *accounts*,
  not for customers, so inventing an ACTIVE/SUSPENDED/CLOSED state machine here
  would be scope the assignment does not ask for.
- **One shared topic, one direction.** Nothing this service does depends on
  account state, so it publishes and does not consume.

---

## Shortcomings

Things I would fix before this went anywhere near production:

- **Events are not transactional.** The event is published after the entity is
  saved but inside the same transaction. If the transaction later rolls back, a
  `CUSTOMER_CREATED` has already been announced for a customer that does not
  exist; if the broker is down, the event is lost. The fix is the transactional
  outbox pattern — write the event to an `outbox` table in the same transaction
  and relay it separately — which is the right answer but more infrastructure
  than a take-home warrants.
- **No delete endpoint.** Deleting a customer who still has accounts is not a
  decision this service can make alone; it needs the account service, and the
  orchestration is out of scope.
- **In-memory users, not a real identity provider.** Fine for demonstrating the
  security mechanics; a real deployment would swap `InMemoryUserDetailsManager`
  for an OAuth2 resource server, which changes `SecurityConfig` alone.
- **Tests use H2, not Postgres.** Fast and Docker-free, but H2 in PostgreSQL mode
  is not Postgres. Testcontainers would run the tests against the real engine.
- **No optimistic locking.** Two concurrent `PUT`s to the same customer, last
  write wins. A `@Version` column would fix it; nothing in the assignment
  requires concurrent edits.
- **Update is a full replace.** `PUT` replaces every mutable field, so a partial
  edit means sending the whole object. A `PATCH` endpoint would be friendlier.
- **No rate limiting, caching, or observability beyond logging.**
