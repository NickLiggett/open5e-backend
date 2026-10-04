# open5e-backend

A REST API over the [Open5e](https://open5e.com) D&D 5e dataset (creatures, spells, magic items, classes, species,
rules and more), built with Spring Boot and PostgreSQL.

- The Open5e data is the **default content**: everyone can read it, and nobody can change it.
- **Users add their own content** (homebrew creatures, spells, items, …) in their own documents, visible only to them
  and to anyone they share a document with.
- Users can **customize default content** by copying it into their own document; the original never changes.
- Default content can be **refreshed from the Open5e API** without touching users' content.

## Contents

- [Quick start](#quick-start)
- [Running locally](#running-locally)
- [Signing in](#signing-in)
- [API](#api)
- [Changing content](#changing-content)
- [Refreshing default content](#refreshing-default-content)
- [How it works](#how-it-works)
- [Database and migrations](#database-and-migrations)
- [Tests](#tests)
- [Project structure](#project-structure)
- [Future plans](#future-plans)
- [Troubleshooting](#troubleshooting)

## Tech stack

- Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security as an OAuth2 resource server), Hibernate 7
- PostgreSQL 18, with [Flyway](https://documentation.red-gate.com/flyway) migrations
- Gradle (wrapper included), Lombok
- Docker Compose for local development, with optional Keycloak for sign-in

## Quick start

You need [Docker Desktop](https://www.docker.com/products/docker-desktop/) running. Nothing else is required: the
app is built inside Docker.

```sh
git clone https://github.com/NickLiggett/open5e-backend.git
cd open5e-backend

# 1. Start the database and the app (the first build takes a few minutes)
docker compose up -d --build

# 2. Load the default content from the Open5e API (about a minute; only needed once)
docker compose run --rm app --spring.profiles.active=import --open5e.import.mode=apply

# 3. Try it
curl http://localhost:8080/api/creatures/a5e-mm_aboleth
curl -H 'X-User: dm' http://localhost:8080/api/me
```

Step 2 isn't needed if you start from a database dump instead; see [Getting the data](#getting-the-data).

Stop everything with `docker compose stop`; your data is kept. `docker compose down -v` deletes the database too.

## Running locally

### Getting the data

The database starts empty unless you give it a dump. There are two ways to fill it:

| | How | When |
|---|---|---|
| **From the Open5e API** | Start the stack, then run the importer (step 2 of the quick start) | Any time; no files needed. This gets the latest Open5e data |
| **From a dump** | Put a `pg_dump` custom-format file at `docker/postgres/open5e_backup.dump` **before the database's first start** | You want to copy this app's database, with users' content, e.g. to another machine |

To make a dump of this app's Compose database:

```sh
docker compose exec db pg_dump -U nick -d open5e -Fc -f /tmp/open5e_backup.dump
docker compose cp db:/tmp/open5e_backup.dump docker/postgres/open5e_backup.dump
```

The dump is gitignored. It's restored only when the database volume is first created; to restore it again, run
`docker compose down -v` and start again.

The dump must come from a database this app created, because it includes Flyway's history (see
[Database and migrations](#database-and-migrations)). A copy of an older Open5e database that never ran these
migrations, such as the original Postgres.app database, won't start (Flyway reports "Found non-empty schema(s)
"open5e" but no schema history table"); load its content with the importer instead.

### Option 1: everything in Docker

```sh
docker compose up --build        # or -d to run in the background
```

The app waits for the database to be ready (including a dump restore, which can take up to a minute), then starts
on <http://localhost:8080>. It uses the `dev` profile, so requests choose their user with an `X-User` header (see
[Signing in](#signing-in)).

After changing code, rebuild and restart the app with `docker compose up -d --build app`.

### Option 2: database in Docker, app from IntelliJ or Gradle

Start only the database:

```sh
docker compose up -d --wait db   # --wait returns once it's ready to accept connections
```

Then run the app with the **`dev` profile**. Without it every request is anonymous: you can read default content,
but `/api/me` and all writes return `401`.

- **Gradle:** `./gradlew bootRun`. The build already sets the `dev` profile for `bootRun`. In IntelliJ, the same
  task is in the **Gradle** tool window under **Tasks → application → bootRun**.
- **IntelliJ Ultimate:** **Run → Edit Configurations…**, select (or add, with **+ → Spring Boot**) a configuration for
  `com.main.app.Application`, and set **Active profiles** to `dev`.
- **IntelliJ Community:** **Run → Edit Configurations…**, select the **Application** configuration for
  `com.main.app.Application`, then **Modify options → Add VM options** and enter `-Dspring.profiles.active=dev`.

The console should show `The following 1 profile is active: "dev"`. `application.properties` already points at the
Compose database (`localhost:5434`). To have IntelliJ start the database for you, add a **Before launch** step to
the run configuration that runs `docker compose up -d --wait db`.

### Docker commands

| Command | What it does |
|---|---|
| `docker compose up --build` | Build the images and start the app and database |
| `docker compose up -d --build` | Same, in the background |
| `docker compose up -d --wait db` | Start only the database, and wait until it's ready |
| `docker compose up -d --build app` | Rebuild and restart only the app after code changes |
| `docker compose logs -f app` | Follow the app logs (`db` for the database) |
| `docker compose ps` | Show container status and ports |
| `docker compose stop` | Stop the containers and keep the data |
| `docker compose down` | Remove the containers and keep the database volume |
| `docker compose down -v` | Remove the containers **and** the database volume; the next start begins again (restoring the dump if there is one) |
| `docker compose exec db psql -U nick -d open5e` | Open a `psql` shell on the database |

### Ports and credentials

| Service | Host port | Details |
|---|---|---|
| app | `8080` | The API. Change with `OPEN5E_APP_PORT` |
| db | `5434` | Database `open5e`, user `nick`, password `open5e`. Change the port with `OPEN5E_DB_PORT` |

For example, `OPEN5E_DB_PORT=5435 docker compose up -d db`. The database uses 5434 so it doesn't clash with a local
Postgres on 5432. These are local development credentials only. When the app runs outside Docker, override its
connection with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`, e.g. if you
changed `OPEN5E_DB_PORT`.

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
| Clients | `initiative-tracker`: the web app, authorization code flow with PKCE required, no password grant. `open5e-cli`: public, with the password grant for curl/Postman. Both accept redirect URIs `http://localhost:*`, any origin, and put the `open5e-api` audience in access tokens |
| Test users | `dm`, `player`, `stranger` (password = username) |
| Mail | Mailpit, <http://localhost:8025>: catches the emails Keycloak sends |

Tokens last an hour. The password grant is only for local testing; the web app uses the authorization code flow.
Run `docker compose up -d app` afterwards to go back to the `dev` profile.

#### Registration, password reset and the web app

The realm lets people **register** themselves (a "Register" link on Keycloak's sign-in page, with no email
verification locally) and **reset a forgotten password** ("Forgot Password?"). The reset email goes to Mailpit, not
the internet: open <http://localhost:8025>, open the message and follow the link. The
[web app](https://github.com/NickLiggett/initiative-tracker-v0.1) sends people to those pages from its own login page.

To try only Keycloak and the mail catcher, next to the app in the `dev` profile (for example while working on a
frontend that runs its own backend):

```sh
docker compose -f compose.yaml -f compose.auth.yaml up -d keycloak mailpit
```

- **The realm is imported only when Keycloak first starts.** After changing `open5e-realm.json`, recreate it with
  `docker compose -f compose.yaml -f compose.auth.yaml rm -sf keycloak`, then the `up -d keycloak` above. Accounts made
  in Keycloak are lost (they are in its own memory); the app's users and their data are not.
- **Browsers need `webOrigins`.** A browser can read Keycloak's token answer only if the client lists the page's origin
  (CORS). `curl` doesn't care, so a client without it works until a web app tries it. Here it is `*`, which suits a
  local realm; a real one should list the app's address.
- **Production:** turn on `verifyEmail`, set a real `smtpServer`, list the app's real redirect URIs and web origin
  instead of `http://localhost:*` and `*`, and add a password policy.

For production, point `OIDC_ISSUER_URI` at your provider (e.g. Auth0, a hosted Keycloak) and have it issue tokens
with the `open5e-api` audience. The code doesn't change.

## API

Every resource has a list endpoint and a get-by-key endpoint (writing is covered in
[Changing content](#changing-content)):

- `GET /api/<resource>`: a page of what the current user can see, e.g. `/api/spells?level=3&school=evocation`
- `GET /api/<resource>/{key}`: one by key, e.g. `/api/spells/srd-2024_fireball`. `404` if it doesn't exist or isn't
  visible to the current user.

The endpoint names are the Open5e v2 names, so Open5e documentation and keys carry over.

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
| `/api/publishers`, `/api/gamesystems`, `/api/licenses`, `/api/itemrarities` | `name`. Global lookups: no document, visible to everyone, read-only |

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
returns `{key, name}` references instead; fetch the full resource from its own endpoint. Links between resources
(`crossreferences`) are paths on this API, e.g. `/api/spells/srd_fireball`.

A creature's `savingThrows`, `skillBonuses` and `speed` only list the entries it has; the `*All` variants list every
one.

Example (trimmed):

```json
{
  "key": "a5e-mm_aboleth-thrall",
  "name": "Aboleth Thrall",
  "document": { "key": "a5e-mm", "name": "Monstrous Menagerie", "…": "…" },
  "derivedFrom": null,
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

| Status | When |
|---|---|
| `400` | A filter or body value of the wrong type, an unknown field or `sort` property |
| `401` | Nobody is signed in (for `/api/me` and writes), or the token is invalid |
| `403` | You can see it but not change it (default content, or you're a viewer) |
| `404` | It doesn't exist, or you can't see it |
| `409` | A key that already exists |

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

# Customize a copy of default content; the response has the copy's key, and derivedFrom is the original's key
curl -X POST -H 'X-User: dm' http://localhost:8080/api/creatures/a5e-mm_aboleth/copy
curl -X PATCH -H 'X-User: dm' -H 'Content-Type: application/json' \
     http://localhost:8080/api/creatures/u2-homebrew_aboleth -d '{"name": "Elder Aboleth", "hitPoints": 300}'
```

- **Where it goes:** `POST` bodies can give a `document` (a document key). Without one, content goes in your
  personal homebrew document, `u{yourId}-homebrew` (your id is in `GET /api/me`).
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

Content you can't see behaves as if it doesn't exist (`404`), so other users' content is never revealed.

### Your settings, avatar and tracker

Things that belong to you rather than to a document. They are yours alone: there is no way to read or change
someone else's, and signing out of one browser and into another finds them as you left them.

| Request | Does |
|---|---|
| `GET /api/me/settings` | Your settings, as far as you have chosen any: `mode` (`light`, `dark` or `system`), `primary` and `secondary` (`#rrggbb`). Also `avatarVersion` (see below), or `null` without an avatar |
| `PUT /api/me/settings` | **Replaces** your settings with the body's. Unknown settings and bad values are a `400`. `avatarVersion` is ignored, so a fetched object can be sent back |
| `GET /api/me/tracker` | The state you left your initiative tracker in: any JSON object you saved, or `{}` |
| `PUT /api/me/tracker` | Replaces it. The shape is the app's to choose; it must be a JSON object of up to 256 KB (`413` beyond that) |
| `PUT /api/me/avatar` | Sets your avatar to the body: a PNG, JPEG, WebP or GIF picture of up to 512 KB, sent as is with its `Content-Type`. The bytes must be that kind of picture (`400`), and other types, such as SVG, are a `415`. Answers `{"avatarVersion": ...}` |
| `DELETE /api/me/avatar` | Removes it (also fine if you have none) |
| `GET /api/users/{username}/avatar` | Anyone's avatar, as the picture itself, or `404`. **No sign-in needed**, because a browser's `<img>` can't send a token; there is no way to list users from here. Sends an `ETag` and `Cache-Control: no-cache`, so a browser keeps it and asks again each time (`304`) |

`avatarVersion` is when the picture was saved (milliseconds since 1970). Add it to the avatar's address
(`/api/users/dm/avatar?v=1759...`) to make browsers fetch a new picture straight away.

```bash
curl -X PUT -H 'X-User: dm' -H 'Content-Type: application/json' http://localhost:8080/api/me/settings   -d '{"mode": "dark", "primary": "#2e7d32", "secondary": "#8d6e63"}'
curl -X PUT -H 'X-User: dm' -H 'Content-Type: image/png' --data-binary @me.png http://localhost:8080/api/me/avatar
curl -o dm.png http://localhost:8080/api/users/dm/avatar
```

They are kept in the tables `user_settings`, `user_avatars` and `user_tracker_states`, and deleted with the user.

## Refreshing default content

The importer loads or refreshes the default content from the [Open5e API](https://api.open5e.com/v2/) without
touching users' content. It runs as a command, not a web server.

It takes every table in the schema to be an Open5e endpoint, except those in `NOT_CONTENT` in `DefaultContentImporter`
(`flyway_schema_history`, `users`, `document_members` and the user data tables). **A new table that isn't Open5e
content must be added there**, or the next import will try to fetch and prune it; `ImportMappingTest` fails if it
isn't.

```sh
# With Docker (the database must be running): report what would change, without writing anything
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
  changes. Into an empty database, it loads everything (about 9,400 rows).
- **What it never does:** write to users' documents. An upstream row whose key belongs to user content is skipped
  and listed. Users' copies of deleted default rows keep their data; only their `derivedFrom` is cleared.
- **Safety net:** it refuses to delete more than half of a table's default rows (for example, if the API returned
  too little). Add `--open5e.import.allow-large-deletions=true` if that's really intended.
- **Fields with no column** (e.g. new in the API) aren't imported and are listed, as a sign the schema may need a
  migration.
- **Timestamps** from the API have no time zone and are read as UTC. **Links** to other Open5e resources become paths
  on this API.
- The source is `open5e.import.source-url` (default `https://api.open5e.com/v2`); the command exits with `0` on
  success and `1` on failure.

## How it works

A short tour of the ideas the code is built on. The design decisions and their history are in
[docs/PLAN.md](docs/PLAN.md).

- **Documents own content.** Every resource belongs to a document (`document_key`). Open5e's sources (`srd-2024`,
  `a5e-mm`, …) are documents with no owner: that's default content. Each user has their own documents (keys
  `u{userId}-…`), and `document_members` shares them with other users as `VIEWER` or `EDITOR`.
- **Visibility is one rule, applied to every query.** A Hibernate filter (`ownership/Visibility`), defined on the
  `Document` entity and switched on in every session, limits every JPA query, including loads by key, to documents
  that are default content, the user's own, or shared with them. Resource entities get it by extending
  `OwnedResource`. Guard tests fail if an entity doesn't, or maps a collection (which the filter wouldn't cover).
- **Who is asking** comes from `CurrentUser`: the `X-User` header under `dev` (`DevCurrentUser`), a verified token
  otherwise (`JwtCurrentUser`). The user is resolved once at the start of each request.
- **Who may change what** is decided by `DocumentAccess`. Default content is also protected by database triggers
  (migration V2), so a bug can't change it; only the importer's transaction lifts that protection.
- **JSON data is typed.** The Open5e data has many JSON columns (`jsonb`). Entities map them straight onto Java records
  (`@JdbcTypeCode(SqlTypes.JSON)`), read with a snake_case mapper (`common/json/DatabaseJson`); API responses use
  camelCase.
- **Every resource looks the same.** Each has an entity, a DTO record with `from(entity)`, a filter record that
  builds the list query, a repository and a controller. Reads go through `ResourceQueries`, writes through
  `ResourceWriter`, which applies request JSON to the entity with Jackson; a test keeps DTO and entity fields in
  step, which is what makes that safe.
- **The tables mirror the Open5e API**, endpoint for table and field for column. That's why the importer
  (`importer/`) needs no per-table code.

## Database and migrations

Flyway manages the `open5e` schema. Migrations are in `src/main/resources/db/migration` and run when the app starts:

| Migration | Purpose |
|---|---|
| `V1__create_schema.sql` | The whole schema: the 33 Open5e content tables (`jsonb` for JSON data, a `document_key` and `derived_from` on every resource), users, document owners and sharing (`document_members`), indexes and foreign keys |
| `V2__protect_default_content.sql` | Triggers that refuse changes to default content and default documents. A transaction can opt out with `SET LOCAL open5e.allow_default_content_changes = 'on'`, as the importer does |
| `V3__user_data.sql` | `user_settings`, `user_avatars` and `user_tracker_states`: what belongs to a user rather than to a document, deleted with the user |

- **Empty database:** Flyway runs all the migrations, creating an empty schema; the importer fills it.
- **Restored dump** of this app's database: it includes Flyway's history table, so Flyway finds the schema up to date
  and only runs migrations added since the dump was made.
- Hibernate runs with `ddl-auto=validate`: it checks the entities against the schema at startup and never changes
  the schema itself.

To change the schema, add a new file with the next version number (e.g. `V7__description.sql`) and restart the app.
Don't edit migrations that have already been applied.

## Tests

The tests run against the Compose database, which must be running and have default content (from the importer or a
dump):

```sh
docker compose up -d --wait db
./gradlew test
```

They create rows for test users named `zz-test-*` and delete them afterwards; tests that touch default content only
use transactions that are rolled back. To run them against another database, set `SPRING_DATASOURCE_URL`.

| Test | What it checks |
|---|---|
| `ApplicationTests` | The Spring context starts, Flyway runs, and Hibernate validates the entities against the schema |
| `DatabaseJsonTest`, `CreatureJsonTest` | Reading snake_case database JSON into records; camelCase API output (no database needed) |
| `JsonColumnsRoundTripTest` | For every entity, each `jsonb` column of every default row survives the mapping unchanged. Covers new entities automatically |
| `DtoMappingTest` | For every entity, `XDTO.from(X)` matches field by field, and no entity field is left out of its DTO by mistake |
| `EndpointSmokeTest` | Every `/api/<table>` endpoint: list totals match the table, a listed key can be fetched, unknown keys give a problem-details `404` |
| `FilterTest` | Each list filter's total against the same condition in SQL, plus sorting, page-size limits and `400`s |
| `ResourceWriteTest` | Creating, replacing, updating, deleting and copying; documents and sharing; permissions for owners, editors, viewers and strangers; validation errors |
| `WriteSmokeTest` | Every writable endpoint: copy a default resource, `PUT` its full JSON back unchanged, `PATCH` it, delete it |
| `DefaultContentProtectionTest` | The database refuses changes to default content and documents unless a transaction opts in |
| `VisibilityTest`, `AnonymousVisibilityTest` | Who sees what through real requests: owners, members, strangers, anonymous requests |
| `TokenSignInTest` | Token sign-in: first sign-in, username clashes, signed-in writes, invalid tokens |
| `ProfileTest` | A user's own settings, avatar picture and tracker state through real requests: validation, replacing, separate users, size limits, picture types and contents, `ETag`/`304`, deleting a user deletes it all |
| `ProfileSignInTest` | The same with token sign-in: only the signed-in user can read or change theirs, but anyone can see an avatar |
| `OwnedResourceMappingTest` | Every entity for a table with a `document_key` is covered by the visibility filter; no collection mappings |
| `ImportMappingTest` | One recorded row per Open5e endpoint (`src/test/resources/import-fixtures/`) maps onto its table |
| `ImportMergeTest` | Merging upstream rows: inserts, updates, deletes, user content untouched, the deletion guard, dry runs, UTC timestamps |
| `ApiUrlsTest` | Which links are rewritten, and which are left alone |

`EndpointSmokeTest` treats every controller as a table, apart from `/api/me` and `/api/users`; add a controller that
isn't one to its `NOT_RESOURCES`.

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
├── profile/           A user's own settings, avatar picture and tracker state (/api/me/...), and avatars
│                      for everyone to see (/api/users/{username}/avatar)
├── importer/          Loading and refreshing default content from the Open5e API
├── creature/          creatures, creature types, creature sets
├── spell/             spells, spell schools
├── item/              items, magic items, item sets, categories, rarities, weapons, weapon properties, armor,
│                      services
├── character/         classes, species, backgrounds, feats
├── rule/              rules, rulesets
└── reference/         abilities, skills, sizes, alignments, languages, damage types, conditions, environments,
                       images, publishers, game systems, licenses
src/main/resources
├── application.properties          Defaults (database, sign-in, paging)
├── application-import.properties   The import profile (no web server)
└── db/migration/                   Flyway migrations
src/test/resources/import-fixtures/ One recorded row per Open5e endpoint
docker/postgres/       Postgres image with the dump restore script (and the dump, if you have one; not committed)
docker/keycloak/       Keycloak realm for local token sign-in
docs/PLAN.md           Design decisions and how the project was built, phase by phase
compose.yaml           Local app + database stack
compose.auth.yaml      Adds Keycloak and token sign-in (see Signing in)
Dockerfile             Multi-stage build of the app image
```

Each resource package has the same five classes per resource: the entity (`Spell`), its DTO (`SpellDTO`, with
`from(Spell)`), its list filter (`SpellFilter`), a repository and a controller.

## Future plans

Not started yet, roughly in order of usefulness:

- **Tests that don't need a local database.** Run the tests against a throwaway Postgres (Testcontainers) loaded
  with the importer's fixtures or a small dump, instead of the Compose database, so they work anywhere.
- **Continuous integration.** A GitHub Actions workflow that builds and tests every push and pull request.
- **Sign-in in the frontend.** The frontend ([initiative-tracker](../initiative-tracker-v0.1)) uses the `dev`
  profile's `X-User` header; it still needs the authorization code flow against Keycloak (with registration and
  password reset), and the API would then need CORS settings for its origin or a proxy, as in development.
- **API documentation.** Generated OpenAPI docs and a Swagger UI (e.g. springdoc), so the endpoints and filters
  don't have to be read from this README.
- **Safe concurrent edits.** A version column and `If-Match`/ETag checks, so two people editing the same resource
  don't silently overwrite each other (today the last write wins, for a user's saved tracker and settings too).
- **Production deployment.** A hosted identity provider, secrets instead of the dev defaults in
  `application.properties`, a published app image, a health endpoint (Spring Boot Actuator) instead of
  `/api/creatures/test`, and a scheduled importer run to pick up Open5e updates.
- **Account management.** Linking a dev-profile user to a signed-in identity, display names, and deleting accounts
  (today a user who owns documents can't be deleted).
- **Email invitations** to shared documents, for people who have never signed in (sharing needs their username, and
  so a first sign-in, today).
- **Better search.** Full-text search across names and descriptions, and more filters (e.g. by creature environment
  or spell components).
- **Images.** Open5e image paths (`/static/img/…`) point at Open5e's own site; serve or proxy them so they work
  from this API.
- **Schema cleanup.** Drop the embedded `document` JSON columns, which the app no longer reads (documents come from
  the `documents` table).

## Troubleshooting

- **Lists are empty and keys like `a5e-mm_aboleth` give `404`.** The database has no default content; the app also
  logs a warning at startup. Load it with the importer (step 2 of the [quick start](#quick-start)). If you meant to
  restore a dump, the database was probably first created without it: put the dump in `docker/postgres/`, run
  `docker compose down -v`, and start again.
- **`Connection to localhost:5434 refused` when running from IntelliJ or Gradle.** The database container isn't
  running, or is still restoring a dump. Run `docker compose up -d --wait db`, which returns once it's ready.
- **`/api/me` and writes return `401` when running from IntelliJ.** The `dev` profile isn't active; see
  [Option 2](#option-2-database-in-docker-app-from-intellij-or-gradle).
- **The database container exits.** Check `docker compose logs db`. If a dump restore failed, the dump may not be
  in `pg_dump`'s custom format (`-Fc`); make it again, then `docker compose down -v` and start again.
- **`port is already allocated`.** Something else is using 8080 or 5434. Choose other ports with `OPEN5E_APP_PORT` or
  `OPEN5E_DB_PORT` (see [Ports and credentials](#ports-and-credentials)).
- **Requests with a token fail with a server error.** The app couldn't reach the token issuer to fetch its keys.
  With `compose.auth.yaml`, check that Keycloak is running (`docker compose -f compose.yaml -f compose.auth.yaml ps`).
- **The importer stops with "upstream is missing N of M rows".** The API returned much less than the database has,
  so it refused to delete that much. Check the API; if the deletions are intended, add
  `--open5e.import.allow-large-deletions=true`.
- **The app won't start: "Found non-empty schema(s) "open5e" but no schema history table".** The database was
  restored from a dump that didn't come from this app (e.g. the original Postgres.app database). Remove the dump from
  `docker/postgres/`, run `docker compose down -v`, start again, and load the content with the importer.
- **The app won't start: "Validate failed: Migrations have failed validation"** (e.g. "Detected applied migration not
  resolved locally"). The database was created by an older set of migrations (V1 to V6, before they were combined
  into two). Its user content, if it has any you want to keep, can't be carried over automatically; otherwise run
  `docker compose down -v`, start again, and load the content with the importer.
- **You want to start over.** `docker compose down -v` deletes the database; the next start begins empty (or restores
  your dump).
