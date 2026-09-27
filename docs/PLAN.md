# Plan

## Goal

A REST API over the Open5e dataset where:

- **Default content** (the Open5e creatures, spells, items, …) is available to everyone and never modified by users.
- **Users add their own content** that only they can see, unless they share it.
- **Customizing a default resource** creates the user's own copy, shown alongside the original. The original is unchanged
  for everyone.
- **Sharing:** a user (e.g. a DM) can share their content with other users (e.g. their party).

## Design decisions

1. **Documents are the unit of ownership.** Every resource already belongs to a document (`document_key`). Default
   documents (`srd-2024`, `a5e-mm`, …) have no owner and are read-only. Each user gets their own document, and
   everything they create goes in it. Keys follow the Open5e `{document}_{slug}` pattern, so user keys can't collide.
2. **One visibility rule, applied in one place.** A user can see a resource if its document has no owner, they own it,
   or they're a member of it. The rule is a Hibernate filter that is enabled in every session and also applies to
   loads by key, so every JPA query gets it, including derived repository queries. Resource entities get it by
   extending `OwnedResource`; `OwnedResourceMappingTest` fails if an entity for a table with a `document_key` doesn't.
   Native SQL bypasses it, so application code must not query resource tables with native queries.
3. **Customized copies** live in the user's document with a new key and a `derived_from` link to the original. Lists
   show both.
4. **Sharing** is per document: `document_members` grants a user `VIEWER` or `EDITOR` access.
5. **Login comes last.** Services ask a `CurrentUser` interface who is making the request:
   - `dev` profile: a default dev user, switchable with an `X-User` header. It exists only under `dev` (Compose and
     `bootRun` set it); in any other profile requests are anonymous and the header is ignored.
   - Tests: requests with the `X-User` header under `@ActiveProfiles("dev")`.
   - Later: read from a verified token (Spring OAuth2 resource server with any OIDC provider; Keycloak in Compose for
     local, a hosted provider in production). Only the `CurrentUser` implementation changes.
6. **`jsonb` columns map straight onto Java records** in the entities (`@JdbcTypeCode(SqlTypes.JSON)`), using a
   snake_case Jackson mapper registered with Hibernate. The API serializes the same records as camelCase.
7. **Code is organized by feature** (`com.main.app.creature`, `com.main.app.spell`, …), with shared records in
   `com.main.app.common`.
8. **Default content is refreshed by an importer** that only touches default documents. Restoring the dump over a
   database with user content would delete it.

## Data model additions

```
users              id, idp_subject, display_name, created_at
documents          + owner_id → users   (null = default content, read-only)
document_members   document_key, user_id, role (VIEWER | EDITOR)
<each resource>    + derived_from → same table   (set on customized copies)
```

Generated key columns (`document_key`, `category_key`, …) become plain columns the app sets, so user-created rows
don't depend on embedded JSON copies.

## Phases

| # | Phase | Status |
|---|---|---|
| 1 | **Foundation:** snake_case `jsonb` mapping, creatures to `jsonb`, feature packages, shared records | Done |
| 2 | **Ownership:** users, `owner_id`, `document_members`, `derived_from`; `CurrentUser` (dev implementation); visibility filter; isolation tests | Done |
| 3 | **Read endpoints** for all resources, with pagination, filters and a shared error handler | Done |
| 4 | **Write endpoints:** create/update/delete in own documents, copy-to-customize, sharing | |
| 5 | **Real login:** token-based `CurrentUser`, Keycloak in Compose | |
| 6 | **Importer** for default content | |

## Resources (phase 3)

Endpoint names match Open5e v2 paths, so cross-reference URLs (`http://localhost:8000/v2/spells/x/`) can be rewritten
to `/api/spells/x`.

| Endpoint | Table (rows) | Filters | Notes |
|---|---|---|---|
| `/api/documents` | documents (24) | publisher, gamesystem | licenses list |
| `/api/publishers`, `/api/gamesystems`, `/api/licenses` | 6 / 3 / 3 | — | global lookups, no owner |
| `/api/abilities` | abilities (6) | document | descriptions; skills as references |
| `/api/skills` | skills (20) | ability | descriptions |
| `/api/sizes`, `/api/alignments` | 7 / 9 | document | |
| `/api/languages` | languages (19) | exotic, secret | |
| `/api/damagetypes`, `/api/conditions` | 13 / 21 | document | conditions have an icon image |
| `/api/environments` | environments (31) | aquatic, planar, interior | |
| `/api/creatures` | creatures (3,541) | name, cr, type, size | done (not yet paginated) |
| `/api/creaturetypes`, `/api/creaturesets` | 14 / 1 | document | set's creatures as references |
| `/api/spellschools` | spellschools (9) | document | |
| `/api/spells` | spells (1,955) | name, level, school, class, damage type, concentration, ritual | typed casting options |
| `/api/itemcategories`, `/api/itemrarities` | 24 / 6 | — | global lookups |
| `/api/items` | items (440) | name, category | shared weapon/armor stats |
| `/api/magicitems` | magicitems (2,319) | name, category, rarity, attunement | same base as items |
| `/api/itemsets` | itemsets (20) | document | items as references |
| `/api/weapons`, `/api/weaponproperties`, `/api/armor` | 75 / 29 / 25 | simple, martial, category | |
| `/api/classes` | classes (151) | document, `/{key}/subclasses` | features, hit points |
| `/api/species` | species (63) | document, `/{key}/subspecies` | traits |
| `/api/backgrounds`, `/api/feats` | 58 / 91 | document, has prerequisite | benefits |
| `/api/rulesets`, `/api/rules` | 52 / 283 | ruleset | rules loaded from `rules`, not the embedded copy |
| `/api/images`, `/api/services` | 32 / 30 | document | |

As built:

- **Parent/child lookups are filters** rather than sub-routes: `/api/classes?subclassOf=…`, `/api/species?subspeciesOf=…`,
  `/api/rules?ruleset=…`.
- **Packages are grouped by domain** (`spell`, `item`, `character`, `rule`, `reference`, …) rather than one per table.
- **The page size parameter is `pageSize`**, because `size` is a filter on creatures and items.
- **No collection mappings** on entities: Hibernate filters don't apply to them, so related resources are loaded with
  queries. `OwnedResourceMappingTest` enforces this.
- **Tests are generic** and pick up new entities and endpoints automatically: `JsonColumnsRoundTripTest`,
  `DtoMappingTest`, `EndpointSmokeTest`; `FilterTest` checks each filter against SQL.
- **Not done yet:** rewriting cross-reference URLs (`http://localhost:8000/v2/spells/x/`) to `/api/spells/x`.

## Data notes

- `desc` is a reserved word in SQL; entity columns must quote it.
- `rulesets.rules` uses camelCase `initialHeaderLevel`; everything else is snake_case.
- `weapons` has no `is_martial` column, but the weapon data embedded in items does.
- `creaturesets.creatures`, `rulesets.rules`, `abilities.skills` and `itemsets.items` hold copies of rows from other
  tables; the API returns references instead.
