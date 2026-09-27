# open5e-backend

A REST API over the [Open5e](https://open5e.com) D&D 5e dataset, built with Spring Boot and PostgreSQL.

The database holds 33 Open5e tables (creatures, spells, magic items, classes, species, rules, …). The Open5e content
is the **default content**, visible to everyone. Users will also be able to add their own content, visible only to
them and anyone they share it with. Every table has paged, filterable read endpoints, and users can create
content, customize copies of default content, and share their documents. See [docs/PLAN.md](docs/PLAN.md) for the
roadmap.

## Tech stack

- Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security as an OAuth2 resource server), Hibernate 7
- PostgreSQL 18, with [Flyway](https://documentation.red-gate.com/flyway) migrations
- Gradle (wrapper included), Lombok
- Docker Compose for local development, with optional Keycloak for sign-in

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
profile (see [Signing in](#signing-in)). To run `com.main.app.Application` from
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

## Signing in

How requests say who they are depends on the Spring profile:

| Profile | Sign-in | Used by |
|---|---|---|
| `dev` | The `X-User` header; no tokens | `docker compose up`, `./gradlew bootRun` |
| anything else | Bearer tokens (JWTs) from an OpenID Connect provider | `compose.auth.yaml`, production |

Without sign-in, requests are anonymous: they can read default content, `/api/me` returns `401`, and writes get
`401`.

### Local development: the `X-User` header

Under `dev`, each request names its user in an `X-User` header:

```sh
curl http://localhost:8080/api/me                      # {"id":1,"username":"dev"} (no header: the "dev" user)
curl -H 'X-User: dm' http://localhost:8080/api/me      # {"id":2,"username":"dm"}
```

- A user is created the first time their name is used. Names are 1-32 lowercase letters, digits or hyphens.
- Use different names to check what each user can see, e.g. that a player sees a DM's shared content and a stranger
  doesn't.
- Anyone can claim any name, so the `dev` profile is for local development only. In every other profile the header
  is ignored.

### Real sign-in: bearer tokens

In every other profile the API is an OAuth2 resource server. It accepts access tokens from the OpenID Connect issuer
in `spring.security.oauth2.resourceserver.jwt.issuer-uri` (env `OIDC_ISSUER_URI`, default the local Keycloak below)
whose audience includes `open5e-api`:

```sh
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/me
```

- The user is created on their first signed-in request, found afterwards by the token's issuer and subject.
- Their username comes from the token's `preferred_username`, made to fit the username rules. If another user has
  it (for example a dev-profile user of the same name), `-2`, `-3`, … is added.
- Invalid, expired or wrong-audience tokens are a `401`, even for reads.
- The issuer's signing keys are fetched on the first request with a token. Requests with tokens fail while the
  issuer is unreachable; anonymous requests don't need it.

To try it locally, `compose.auth.yaml` adds Keycloak and runs the app with token sign-in:

```sh
docker compose -f compose.yaml -f compose.auth.yaml up --build

# Get a token for a test user (dm, player or stranger; the password is the username)
TOKEN=$(curl -s -X POST http://localhost:8180/realms/open5e/protocol/openid-connect/token \
  -d grant_type=password -d client_id=open5e-cli -d username=dm -d password=dm | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/me
```

| | |
|---|---|
| Keycloak | <http://localhost:8180>, admin console user `admin`, password `admin` |
| Realm | `open5e`, from `docker/keycloak/open5e-realm.json` |
| Client | `open5e-cli`: public, password grant for curl/Postman, redirect URIs `http://localhost:*` for a local frontend |
| Test users | `dm`, `player`, `stranger` (password = username) |

Tokens last an hour. The password grant is only for local testing; a real frontend would use the authorization
code flow. Run `docker compose up -d app` afterwards to go back to the `dev` profile.

For production, point `OIDC_ISSUER_URI` at your provider (e.g. Auth0, a hosted Keycloak) and have it issue tokens
with the `open5e-api` audience. The code doesn't change.

## API

Every resource has a list endpoint and a get-by-key endpoint (writing is covered in
[Changing content](#changing-content)):

- `GET /api/<resource>`: a page of what the current user can see, e.g. `/api/spells?level=3&school=evocation`
- `GET /api/<resource>/{key}`: one by key, e.g. `/api/spells/srd-2024_fireball`. `404` if it doesn't exist or isn't
  visible to the current user.

### Paging and sorting

| Parameter | Default | |
|---|---|---|
| `page` | `0` | Zero-based page number |
| `pageSize` | `50` | Up to `500`. It isn't called `size` because that's a filter on creatures and items |
| `sort` | `key` | A response field, optionally with a direction: `sort=name,desc`. Repeat for several |

Lists return the page and its position:

```json
{
  "content": [ { "key": "srd-2024_animate-dead", "name": "Animate Dead", "…": "…" } ],
  "page": { "size": 50, "number": 0, "totalElements": 29, "totalPages": 1 }
}
```

### Resources and filters

Every resource except the global lookups can be filtered with `document` (one or more document keys,
comma-separated: `document=srd-2014,srd-2024`) and `name` (case-insensitive part of the name). Boolean filters take
`true` or `false`.

| Endpoint | Other filters |
|---|---|
| `/api/documents` | `publisher`, `gamesystem` (keys); `name` |
| `/api/creatures` | `cr` (exact, e.g. `0.25`), `crMin`, `crMax`, `type` (e.g. `dragon`), `size` (e.g. `huge`) |
| `/api/creaturetypes`, `/api/creaturesets` | |
| `/api/spells` | `level`, `school` (e.g. `evocation`), `class` (e.g. `srd-2024_wizard`), `damageType` (e.g. `fire`), `concentration`, `ritual` |
| `/api/spellschools` | |
| `/api/items` | `category` (e.g. `armor`) |
| `/api/magicitems` | `category` (e.g. `wand`), `rarity` (e.g. `legendary`), `requiresAttunement` |
| `/api/itemsets`, `/api/itemcategories`, `/api/services` | |
| `/api/weapons` | `isSimple`, `isImprovised` |
| `/api/weaponproperties` | `type` (e.g. `Mastery`) |
| `/api/armor` | `category` (`light`, `medium`, `heavy`) |
| `/api/classes` | `subclassOf` (a class key: its subclasses), `subclass` (`false`: base classes only), `casterType` |
| `/api/species` | `subspeciesOf` (a species key), `isSubspecies` |
| `/api/backgrounds` | |
| `/api/feats` | `hasPrerequisite`, `type` |
| `/api/rulesets` | |
| `/api/rules` | `ruleset` (a ruleset key) |
| `/api/abilities`, `/api/sizes`, `/api/damagetypes`, `/api/conditions`, `/api/images` | |
| `/api/alignments` | (no `name`: alignments only have a `shortName`) |
| `/api/skills` | `ability` (e.g. `dex`) |
| `/api/languages` | `isExotic`, `isSecret` |
| `/api/environments` | `aquatic`, `planar`, `interior` |
| `/api/publishers`, `/api/gamesystems`, `/api/licenses`, `/api/itemrarities` | `name`. Global lookups: no document, visible to everyone |

Other endpoints:

| Endpoint | Description |
|---|---|
| `GET /api/me` | The current user, or `401` if nobody is signed in |
| `GET /api/creatures/test` | Simple check that the app is up |

### Response shapes

Responses use camelCase fields. JSON data is returned as nested objects and arrays, e.g. a creature's actions and
their attacks, a spell's casting options, a class's features, an item's weapon and armor stats. Resources that
belong to a document include a `document` summary and `derivedFrom` (see below). Where the Open5e data embeds copies
of other rows (an ability's skills, a creature set's creatures, an item set's items, a ruleset's rules), the API
returns `{key, name}` references instead; fetch the full resource from its own endpoint.

A creature's `savingThrows`, `skillBonuses` and `speed` only list the entries it has; the `*All` variants list every
one.

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

### Errors

Errors are [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details (`application/problem+json`):

```json
{ "status": 400, "title": "Bad Request", "detail": "Invalid value 'three' for 'level'", "instance": "/api/spells" }
```

`404` for a missing or invisible key, `400` for a filter value of the wrong type or an unknown `sort` property.

### Who can see what

Every resource belongs to a **document**: a source like the SRD (`srd-2024`) or a user's own homebrew. A user can see a
resource if its document is default content (no owner), they own the document, or it has been shared with them. The
rule is a Hibernate filter enabled for every database query (`ownership/Visibility`), so endpoints get it without
doing anything. Content outside a user's view behaves as if it doesn't exist (`404`).

Customized copies of default content carry `derivedFrom`, the key of the resource they were copied from; it is `null`
for everything else.

## Changing content

Signed-in users (see [Signing in](#signing-in)) can create their own content, customize copies of default
content, and share it. Default content never changes.

### Creating and editing resources

Every resource except the global lookups (publishers, game systems, licenses, item rarities) supports:

| Request | Does |
|---|---|
| `POST /api/<resource>` | Creates a resource. `201` with its URL in `Location` |
| `PUT /api/<resource>/{key}` | Replaces all its fields; fields left out are cleared |
| `PATCH /api/<resource>/{key}` | Changes only the fields given |
| `DELETE /api/<resource>/{key}` | Deletes it. `204` |
| `POST /api/<resource>/{key}/copy` | Copies a resource you can see (e.g. default content) into your own document to customize it. Body optional: `{"document": "<key>"}` |

Request bodies use the same JSON as responses, so you can `GET` a resource, change it and `PUT` it back as it is.
The server sets `key`, `document` and `derivedFrom`: they're ignored in `PUT` and `PATCH` bodies. Unknown fields and
values of the wrong type are rejected with a `400` that names the field.

```sh
# Create, in your personal homebrew document (created on first use)
curl -X POST -H 'X-User: dm' -H 'Content-Type: application/json' http://localhost:8080/api/creatures \
     -d '{"name": "Owlbear King", "challengeRating": 5, "type": {"key": "monstrosity", "name": "Monstrosity"}}'

# Customize a copy of default content; the copy's derivedFrom is the original's key
curl -X POST -H 'X-User: dm' http://localhost:8080/api/creatures/a5e-mm_aboleth/copy
curl -X PATCH -H 'X-User: dm' -H 'Content-Type: application/json' \
     http://localhost:8080/api/creatures/u2-homebrew_aboleth -d '{"name": "Elder Aboleth", "hitPoints": 300}'
```

- **Where it goes:** `POST` bodies can give a `document` (a document key). Without one, content goes in your
  personal homebrew document, `u{yourId}-homebrew`.
- **Keys** are the document key plus a slug of the name, e.g. `u2-homebrew_owlbear-king`. Give `slug` in the body to
  choose it. Creating a key that exists is a `409`; copies get the next free key (`…_aboleth-2`).
- **Copies** appear alongside the original in lists. Changing a copy never changes the original.

### Documents and sharing

| Request | Does |
|---|---|
| `POST /api/documents` | Creates a document you own. Body: `name`, optional `slug`, `displayName`, `desc`, `author`. Key: `u{yourId}-{slug}` |
| `PUT /api/documents/{key}` | Changes its name, display name, description and author. Owner only |
| `DELETE /api/documents/{key}` | Deletes it **and everything in it**. Owner only |
| `GET /api/documents/{key}/members` | Its owner and members, with roles |
| `PUT /api/documents/{key}/members/{username}` | Shares it with a user, or changes their role. Body: `{"role": "VIEWER"}` or `{"role": "EDITOR"}`. Owner only |
| `DELETE /api/documents/{key}/members/{username}` | Stops sharing with a user. The owner can remove anyone; members can remove themselves |

Documents include `ownerId`: compare it with `GET /api/me` to tell your own documents apart.

### Who can change what

| | See its content | Add, change, delete content | Rename, delete, manage members |
|---|---|---|---|
| Default content | everyone | nobody (copy it instead) | nobody |
| Owner | yes | yes | yes |
| `EDITOR` member | yes | yes | no |
| `VIEWER` member | yes | no | no |
| Anyone else | no | no | no |

Responses: `401` when nobody is signed in, `403` when you can see something but not change it, `404` when you
can't see it at all (so other users' content is never revealed).

The database also refuses changes to default content (migration V5), as a backstop in case of an application bug.

## Refreshing default content

The default content can be refreshed from the [Open5e API](https://api.open5e.com/v2/) without touching users'
content. The importer runs as a command, not a web server:

```sh
# With Docker: report what would change (nothing is written)
docker compose run --rm app --spring.profiles.active=import --open5e.import.mode=dry-run

# Apply the changes
docker compose run --rm app --spring.profiles.active=import --open5e.import.mode=apply

# Or from Gradle, against the database in application.properties
./gradlew bootRun --args='--spring.profiles.active=import --open5e.import.mode=dry-run'
```

It prints what changed per table:

```
table              fetched  inserted  updated  unchanged  deleted
magicitems            2322         3       58       2261        0
services                30         0        7         23        0
...
98 changes
```

- **What it does:** fetches all 33 endpoints, then in **one transaction** adds new default rows, replaces changed
  ones (unchanged rows aren't written) and deletes default rows upstream no longer has. If anything fails, nothing
  changes.
- **What it never does:** write to users' documents. An upstream row whose key belongs to user content is skipped
  and listed. Users' copies of deleted default rows keep their data; only their `derivedFrom` is cleared.
- **Safety net:** it refuses to delete more than half of a table's default rows (for example, if the API returned
  too little). Add `--open5e.import.allow-large-deletions=true` if that's really intended.
- **Fields with no column** (e.g. new in the API) aren't imported and are listed, as a sign the schema may need a
  migration.
- **Timestamps** from the API have no time zone and are read as UTC.
- **Links** to other Open5e resources become paths on this API (`/api/spells/srd_fireball`); see V6 below.
- The source is `open5e.import.source-url` (default `https://api.open5e.com/v2`); the command exits with `0` on
  success and `1` on failure.

At the time of writing, the public API has 98 changes compared with `open5e_backup.dump`: 3 new magic items,
attunement fixes on 58 magic items, wording changes on 7 services and 4 spells, one ability and one class, and the
24 documents' publication dates (the dump stored them 7 hours off, from being read in a non-UTC time zone).

## Database and migrations

Flyway manages the `open5e` schema. Migrations are in `src/main/resources/db/migration`:

| Migration | Purpose |
|---|---|
| `V1__create_open5e_schema.sql` | The full `open5e` schema from the dump: 33 tables, indexes and foreign keys |
| `V2__drop_stray_public_tables.sql` | Drops leftover tables from the `public` schema |
| `V3__creature_json_columns_to_jsonb.sql` | Converts the creature JSON columns from text to `jsonb`, like every other table |
| `V4__ownership.sql` | Users, document owners and sharing (`document_members`); `document_key` and `derived_from` on every resource |
| `V5__protect_default_content.sql` | Triggers that refuse changes to default content and default documents. A transaction can opt out with `SET LOCAL open5e.allow_default_content_changes = 'on'`, as the importer does |
| `V6__rewrite_open5e_links.sql` | Rewrites links to other Open5e resources (`http://<any host>/v2/spells/srd_fireball/`) to paths on this API (`/api/spells/srd_fireball`) |

- **Restored database (the Compose setup):** Flyway sees an existing schema, records it as V1 without running the
  script, then applies V2 and anything newer.
- **Empty database:** Flyway runs every migration and creates an empty schema. Data comes from the dump.
- `jsonb` columns map straight onto Java records in the entities (`@JdbcTypeCode(SqlTypes.JSON)`). Hibernate reads
  them with the snake_case mapper in `common/json/DatabaseJson`; API responses use Spring's camelCase mapper.
- Hibernate runs with `ddl-auto=validate`: it checks the entities against the schema at startup and never changes the
  schema itself.

To change the schema, add a new file with the next version number (e.g. `V7__description.sql`), and restart the
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
| `JsonColumnsRoundTripTest` | For every entity, compares each `jsonb` column of every default row with the mapped value, so any field lost or changed by the records fails the test. Covers new entities automatically |
| `DtoMappingTest` | For every entity, checks `XDTO.from(X)` field by field against every default row, and that no entity field is left out of its DTO by mistake |
| `EndpointSmokeTest` | Every `/api/<table>` endpoint: list totals match the table, a listed key can be fetched, unknown keys give a problem-details `404` |
| `FilterTest` | Each list filter's total against the same condition in SQL, plus sorting, page-size limits and `400`s |
| `ResourceWriteTest` | Creating, replacing, updating, deleting and copying resources; documents and sharing; permissions for owners, editors, viewers and strangers; validation errors |
| `WriteSmokeTest` | Every writable endpoint: copy a default resource, check the copy matches, `PUT` its full JSON back unchanged, `PATCH` it, delete it |
| `DefaultContentProtectionTest` | The database refuses changes to default content and documents unless a transaction opts in |
| `ImportMappingTest` | One recorded row per Open5e endpoint (`src/test/resources/import-fixtures/`): every field lands in a column with a valid value, key columns are filled, links are rewritten |
| `ImportMergeTest` | Merging upstream rows: inserts, updates, deletes, user content untouched, the large-deletion guard, dry runs, UTC timestamps. Every merge is rolled back |
| `ApiUrlsTest` | Which links are rewritten, and which are left alone |
| `VisibilityTest` | Real requests as different users (`dev` profile): owners and members see a homebrew creature, strangers get `404`, everyone sees default content |
| `AnonymousVisibilityTest` | Outside `dev`, requests without a token are anonymous: the `X-User` header is ignored and writes get `401` |
| `TokenSignInTest` | Token sign-in: users created on first sign-in and found by issuer and subject, username clashes get a suffix, signed-in writes, invalid tokens get `401` |
| `OwnedResourceMappingTest` | Every entity for a table with a `document_key` extends `OwnedResource`, so the visibility filter covers it, and no entity has collection mappings, which the filter wouldn't cover |
| `CreatureDataTest` | Compares every JSON column of every creature in the database with the API objects, so any lost or changed data fails the test. It is skipped if the table is empty. The comparison lives in `JsonColumnRoundTrip`, for reuse by other tables |

The ownership and writing tests add rows (users `zz-test-*` and their documents) and delete them afterwards;
the database backstop test only uses transactions that are rolled back.

## Project structure

```
src/main/java/com/main/app
├── Application.java
├── common/            Records shared across resources (NamedReference, DocumentSummary, Description, …)
│   ├── json/          Database JSON mapping (snake_case mapper, Hibernate config)
│   ├── query/         Filter building blocks (Specs) and jsonb SQL functions
│   └── web/           ResourceQueries (list/get for every controller), error handling
├── document/          Documents and sharing; DocumentAccess decides who may change what; the Document
│                      entity defines the visibility filter
├── ownership/         OwnedResource (base class for resource entities), the visibility rule, and
│                      ResourceWriter (create/replace/update/delete/copy for every resource)
├── user/              Sign-in (SecurityConfig), CurrentUser and its dev (X-User) and token (JWT)
│                      implementations, /api/me
├── importer/          Refreshing default content from the Open5e API
├── creature/          creatures, creature types, creature sets
├── spell/             spells, spell schools
├── item/              items, magic items, item sets, categories, rarities, weapons, weapon properties, armor,
│                      services
├── character/         classes, species, backgrounds, feats
├── rule/              rules, rulesets
└── reference/         abilities, skills, sizes, alignments, languages, damage types, conditions, environments,
                       images, publishers, game systems, licenses

Each resource has the same five classes: the entity (`Spell`), its DTO (`SpellDTO`, with `from(Spell)`), its list
filter (`SpellFilter`), a repository and a controller.
src/main/resources
├── application.properties
└── db/migration/      Flyway migrations
docker/postgres/       Postgres image with the dump restore script (and the dump, which is not committed)
docker/keycloak/       Keycloak realm for local token sign-in
docs/PLAN.md           Roadmap and design decisions
compose.yaml           Local app + database stack
compose.auth.yaml      Adds Keycloak and token sign-in (see Signing in)
Dockerfile             Multi-stage build of the app image
```

## Troubleshooting

- **The database container exits with `open5e_backup.dump not found`.** The dump is missing from `docker/postgres/`.
  Copy it there, then run `docker compose down -v` and start again. The `-v` matters: the restore only runs when the
  volume is created, so a volume left behind by the failed attempt would otherwise start empty.
- **`port is already allocated`.** Something else is using 8080 or 5434. Change the host side of the port mapping in
  `compose.yaml` (for example `"5435:5432"`); if you change the database port, update `spring.datasource.url` in
  `application.properties` too.
- **Requests with a token fail with a server error.** The app couldn't reach the token issuer to fetch its keys.
  With `compose.auth.yaml`, check that Keycloak is running (`docker compose -f compose.yaml -f compose.auth.yaml ps`).
- **The importer stops with "upstream is missing N of M rows".** The API returned much less than the database has,
  so it refused to delete that much. Check the API; if the deletions are intended, add
  `--open5e.import.allow-large-deletions=true`.
- **You want a fresh copy of the data.** Run `docker compose down -v`, then `docker compose up`.
