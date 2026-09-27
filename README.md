# open5e-backend

A read-only REST API over the [Open5e](https://open5e.com) D&D 5e dataset, built with Spring Boot and PostgreSQL.

The database holds 33 Open5e tables (creatures, spells, magic items, classes, species, rules, …). The API currently
exposes **creatures**; the other tables are next.

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

`application.properties` already points at the Compose database (`localhost:5434`), so no extra configuration is
needed. You can also run `com.main.app.Application` from IntelliJ.

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

## API

All endpoints are read-only `GET`s.

| Endpoint | Description |
|---|---|
| `GET /api/creatures` | All creatures (3,541 in the current dump; not paginated yet) |
| `GET /api/creatures/{key}` | One creature by key, e.g. `a5e-mm_aboleth`. Returns `404` if the key doesn't exist |
| `GET /api/creatures/test` | Simple check that the controller is up |

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

- **Restored database (the Compose setup):** Flyway sees an existing schema, records it as V1 without running the
  script, then applies V2 and anything newer.
- **Empty database:** Flyway runs every migration and creates an empty schema. Data comes from the dump.
- Hibernate runs with `ddl-auto=validate`: it checks the entities against the schema at startup and never changes the
  schema itself.

To change the schema, add a new file named `V3__description.sql` (next number), and restart the app. Don't edit
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
| `CreatureMapperTest` | Unit tests for converting creature rows to response objects (no database needed) |
| `CreatureMapperDataTest` | Converts every JSON column of every creature in the database and compares the result with the original, so any lost or changed data fails the test. It is skipped if the table is empty |

## Project structure

```
src/main/java/com/main/app
├── Application.java
├── controller/        REST controllers
├── dtos/Creature/     Response records (CreatureDTO and its nested types)
├── entity/            JPA entities
├── repository/        Spring Data repositories
└── service/           Services and CreatureMapper (entity → DTO)
src/main/resources
├── application.properties
└── db/migration/      Flyway migrations
docker/postgres/       Dump restore script (and the dump, which is not committed)
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
