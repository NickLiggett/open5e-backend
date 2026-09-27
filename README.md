# open5e-backend

A REST API over the [Open5e](https://open5e.com) D&D 5e dataset, built with Spring Boot and PostgreSQL.

The database holds 33 Open5e tables (creatures, spells, magic items, classes, species, rules, …). The Open5e content
is the **default content**, visible to everyone. Users will also be able to add their own content, visible only to
them and anyone they share it with. The API currently exposes **creatures** (read-only); the other tables, and
endpoints for creating content, are next. See [docs/PLAN.md](docs/PLAN.md) for the roadmap.

## Tech stack

- Java 21, Spring Boot 4.1 (Web MVC, Data JPA), Hibernate 7
- PostgreSQL 18, with [Flyway](https://documentation.red-gate.com/flyway) migrations
- Gradle (wrapper included), Lombok
- Docker Compose for local development

## Running locally

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Compose v2)
- The database dump, `open5e_backup.dump`. It is **not in the repository**; copy it to
  `docker/postgres/open5e_backup.dump` before the first start.
- JDK 21, only if you run the app or tests outside Docker

### Option 1: app and database in Docker

```sh
docker compose up --build
```

The first start creates the database volume and restores the dump into it, which can take up to a minute. The app waits
for the restore to finish, then starts on <http://localhost:8080>. Later starts reuse the volume and skip the restore.

Check that it's running:

```sh
curl http://localhost:8080/api/creatures/a5e-mm_aboleth
```

### Option 2: database in Docker, app from Gradle or your IDE

```sh
docker compose up -d db
./gradlew bootRun
```

`application.properties` already points at the Compose database (`localhost:5434`), and `bootRun` uses the `dev`
profile (see [Users in local development](#users-in-local-development)). To run `com.main.app.Application` from
IntelliJ, set **Active profiles** to `dev` in the run configuration; without it, every request is anonymous.

### Docker commands

| Command | What it does |
|---|---|
| `docker compose up --build` | Build the app image and start the app and database |
| `docker compose up -d --build` | Same, in the background |
| `docker compose up -d db` | Start only the database |
| `docker compose logs -f app` | Follow the app logs |
| `docker compose ps` | Show container status and ports |
| `docker compose stop` | Stop the containers and keep the data |
| `docker compose down` | Remove the containers and keep the database volume |
| `docker compose down -v` | Remove the containers **and** the database volume; the next start restores the dump again |
| `docker compose exec db psql -U nick -d open5e` | Open a `psql` shell on the database |
| `docker compose up -d --build app` | Rebuild and restart only the app after code changes |

### Ports and credentials

| Service | Host port | Details |
|---|---|---|
| app | `8080` | The API |
| db | `5434` | Database `open5e`, user `nick`, password `open5e` |

The database uses host port 5434 so it doesn't clash with other local Postgres instances on 5432. These are local
development credentials only. Override any datasource setting with environment variables such as
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.

## Users in local development

There is no sign-in yet. Under the `dev` profile, which Compose and `bootRun` use, each request says who it is with
an `X-User` header:

```sh
curl http://localhost:8080/api/me                      # {"id":1,"username":"dev"} (no header: the "dev" user)
curl -H 'X-User: dm' http://localhost:8080/api/me      # {"id":2,"username":"dm"}
```

- A user is created the first time their name is used. Names are 1-32 lowercase letters, digits or hyphens.
- Use different names to check what each user can see, e.g. that a player sees a DM's shared content and a stranger
  doesn't.
- Anyone can claim any name, so the `dev` profile is for local development only. Without it, the header is ignored
  and every request is anonymous: only default content is visible and `/api/me` returns `401`.

Real sign-in replaces this in phase 5 without changing the endpoints (see [docs/PLAN.md](docs/PLAN.md)).

## API

All endpoints are `GET`s for now.

| Endpoint | Description |
|---|---|
| `GET /api/creatures` | All creatures visible to the current user (3,541 defaults in the current dump; not paginated yet) |
| `GET /api/creatures/{key}` | One creature by key, e.g. `a5e-mm_aboleth`. Returns `404` if it doesn't exist or isn't visible to the current user |
| `GET /api/creatures/test` | Simple check that the controller is up |
| `GET /api/me` | The current user, or `401` if nobody is signed in |

### Who can see what

Every resource belongs to a **document**: a source like the SRD (`srd-2024`) or a user's own homebrew. A user can see a
resource if its document is default content (no owner), they own the document, or it has been shared with them. The
rule is a Hibernate filter enabled for every database query (`ownership/Visibility`), so endpoints get it without
doing anything. Content outside a user's view behaves as if it doesn't exist (`404`).

Customized copies of default content carry `derivedFrom`, the key of the resource they were copied from; it is `null`
for everything else.

Creature responses use camelCase fields, and the stat block is returned as nested JSON objects and arrays:
document, type, size, speeds, ability scores and modifiers, saving throws, skill bonuses, languages, resistances and
immunities, actions (with their attacks and usage limits), traits, environments, illustration and cross-references.
`savingThrows`, `skillBonuses` and `speed` only list the entries a creature has; the `*All` variants list every one.

Example (trimmed):

```json
{
  "key": "a5e-mm_aboleth-thrall",
  "name": "Aboleth Thrall",
  "type": { "key": "humanoid", "name": "Humanoid" },
  "speed": { "unit": "feet", "walk": 30, "swim": 30 },
  "abilityScores": { "strength": 14, "dexterity": 14, "constitution": 14, "intelligence": 10, "wisdom": 10, "charisma": 12 },
  "actions": [
    {
      "name": "Poison Ink Knife",
      "actionType": "ACTION",
      "attacks": [
        { "attackType": "WEAPON", "toHitMod": 4, "reach": 5, "damageDieCount": 1, "damageDieType": "D4", "damageBonus": 2,
          "extraDamageType": { "key": "poison", "name": "Poison" } }
      ]
    }
  ]
}
```

## Database and migrations

Flyway manages the `open5e` schema. Migrations are in `src/main/resources/db/migration`:

| Migration | Purpose |
|---|---|
| `V1__create_open5e_schema.sql` | The full `open5e` schema from the dump: 33 tables, indexes and foreign keys |
| `V2__drop_stray_public_tables.sql` | Drops leftover tables from the `public` schema |
| `V3__creature_json_columns_to_jsonb.sql` | Converts the creature JSON columns from text to `jsonb`, like every other table |
| `V4__ownership.sql` | Users, document owners and sharing (`document_members`); `document_key` and `derived_from` on every resource |

- **Restored database (the Compose setup):** Flyway sees an existing schema, records it as V1 without running the
  script, then applies V2 and anything newer.
- **Empty database:** Flyway runs every migration and creates an empty schema. Data comes from the dump.
- `jsonb` columns map straight onto Java records in the entities (`@JdbcTypeCode(SqlTypes.JSON)`). Hibernate reads
  them with the snake_case mapper in `common/json/DatabaseJson`; API responses use Spring's camelCase mapper.
- Hibernate runs with `ddl-auto=validate`: it checks the entities against the schema at startup and never changes the
  schema itself.

To change the schema, add a new file with the next version number (e.g. `V5__description.sql`), and restart the
app. Don't edit
migrations that have already been applied.

## Tests

The tests need the database, so start it first:

```sh
docker compose up -d db
./gradlew test
```

| Test | What it checks |
|---|---|
| `ApplicationTests` | The Spring context starts, Flyway runs, and Hibernate validates the entities against the schema |
| `DatabaseJsonTest` | Reading snake_case database JSON into records (no database needed) |
| `CreatureJsonTest` | camelCase API output, and leaving out absent speeds and skills (no database needed) |
| `VisibilityTest` | Real requests as different users (`dev` profile): owners and members see a homebrew creature, strangers get `404`, everyone sees default content |
| `AnonymousVisibilityTest` | Outside `dev`, requests are anonymous and the `X-User` header is ignored |
| `OwnedResourceMappingTest` | Every entity for a table with a `document_key` extends `OwnedResource`, so the visibility filter covers it |
| `CreatureDataTest` | Compares every JSON column of every creature in the database with the API objects, so any lost or changed data fails the test. It is skipped if the table is empty. The comparison lives in `JsonColumnRoundTrip`, for reuse by other tables |

The ownership tests add rows (users `zz-test-*` and their documents) and delete them afterwards.

## Project structure

```
src/main/java/com/main/app
├── Application.java
├── common/            Records shared across resources (NamedReference, DocumentSummary, …)
│   └── json/          Database JSON mapping (snake_case mapper, Hibernate config)
├── creature/          Everything for /api/creatures: entity, repository, service, controller,
│                      CreatureDTO and its nested records
├── document/          The Document entity; defines the visibility filter
├── ownership/         OwnedResource (base class for resource entities) and the visibility rule
└── user/              CurrentUser and its implementations, /api/me
src/main/resources
├── application.properties
└── db/migration/      Flyway migrations
docker/postgres/       Dump restore script (and the dump, which is not committed)
docs/PLAN.md           Roadmap and design decisions
compose.yaml           Local app + database stack
Dockerfile             Multi-stage build of the app image
```

## Troubleshooting

- **The database container exits with `open5e_backup.dump not found`.** The dump is missing from `docker/postgres/`.
  Copy it there, then run `docker compose down -v` and start again. The `-v` matters: the restore only runs when the
  volume is created, so a volume left behind by the failed attempt would otherwise start empty.
- **`port is already allocated`.** Something else is using 8080 or 5434. Change the host side of the port mapping in
  `compose.yaml` (for example `"5435:5432"`); if you change the database port, update `spring.datasource.url` in
  `application.properties` too.
- **You want a fresh copy of the data.** Run `docker compose down -v`, then `docker compose up`.
