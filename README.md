# Remus

A self-hostable character sheet manager for the Drakar och Demoner tabletop
RPG (the modern Riotminds edition, published in English as *Dragonbane*).
Each player registers an account and manages their own character sheets —
attributes, skills, weapons, inventory, resources — through a web UI that
mirrors the physical sheet.

A hosted instance runs at [remus.fxh.nu](https://remus.fxh.nu).

This is a fan-made, unofficial tool. It is not affiliated with or endorsed by
Riotminds or Free League.

## Stack

- **Backend**: Scala 3, ZIO, Tapir, zio-http, doobie + SQLite.
- **Frontend**: Scala.js + Laminar.
- **Shared module**: domain model, Tapir endpoint definitions, and circe JSON
  codecs are cross-compiled and used by both the server and the browser
  client, so the two never drift out of sync.
- **Build**: sbt 2.x (using its built-in `projectMatrix` for JVM/JS
  cross-building instead of the old sbt-crossproject plugin).

## Running locally

Requires JDK 17+ and sbt.

```sh
sbt server/copyClientAssets   # links the client with Scala.js and copies
                               # the bundle into the server's resources
sbt server/run
```

Then open http://localhost:8080. There's also an internal admin API on
http://localhost:8081 (Swagger UI at `/docs`), currently just a scaffold for
future admin functionality. This port isn't meant to be exposed publicly;
the Docker setup binds it to `127.0.0.1` only, reachable from the host
machine but not the network.

Configuration lives in `modules/server/src/main/resources/application.conf`
(loaded via zio-config), with environment variables overriding the defaults:
`PORT` (8080), `ADMIN_PORT` (8081), `DB_PATH` (`./data/remus.db`).

Authentication is "Login with Discord" — there's no username/password. You
need a Discord application (create one at the
[Discord Developer Portal](https://discord.com/developers/applications)) with
an OAuth2 redirect registered at
`http://localhost:8080/api/auth/discord/callback` (or your real host/port).
`DISCORD_CLIENT_ID` and `DISCORD_CLIENT_SECRET` are required, with no
default — the app fails to start if either is missing. `DISCORD_REDIRECT_URI`
defaults to `http://localhost:8080/api/auth/discord/callback`; override it to
match whatever redirect URI you registered with Discord if you're not
running on localhost:8080.

For local `sbt server/run`, sbt doesn't load `.env` files itself, so export
these in your shell before running (or use a tool like
[direnv](https://direnv.net/) with a gitignored `.envrc`):

```sh
export DISCORD_CLIENT_ID=...
export DISCORD_CLIENT_SECRET=...
sbt server/run
```

Whenever you change client code, re-run `server/copyClientAssets` before
`server/run` (or before `server/reStart`-style workflows) to refresh the
bundle — it's a plain resource file, not something sbt's `run` recompiles
for you automatically.

## Running the tests

```sh
sbt "server/testOnly *"
```

(`sbt server/test` sometimes reports "no tests to run" right after a clean
build on sbt 2.x — a testQuick-style change-detection quirk. `testOnly *`
always runs everything.)

Repository tests run against a real temp-file SQLite database (no mocking).

## Self-hosting with Docker

Copy `docker/.env.example` to `docker/.env` and fill in your Discord OAuth
credentials — Docker Compose loads it automatically, and it's gitignored so
secrets never land in the repo.

```sh
cd docker
cp .env.example .env   # fill in DISCORD_CLIENT_ID / DISCORD_CLIENT_SECRET
docker compose up -d --build
```

This builds the server (embedding the compiled client bundle) into a single
container and serves it on port 8080, persisting the SQLite database in a
named volume. No separate database server or Node/JS toolchain is needed —
Laminar and Scala.js compile to a single dependency-free JS bundle.

To change the port or database path, edit `docker/docker-compose.yml`'s
`PORT`/`DB_PATH` environment variables.

## Project layout

```
modules/
  shared/   cross-compiled (JVM + JS): domain model, Tapir endpoints, JSON codecs
  server/   ZIO + Tapir + zio-http + doobie backend
  client/   Scala.js + Laminar frontend
docker/     Dockerfile + docker-compose.yml
```

## Scope

This is a first iteration focused on core character-sheet CRUD: each user
authenticates via Discord OAuth and owns the characters they create — there's
no campaign/GM grouping yet. The frontend is intentionally plain
(no component framework, no client-side routing) since it's expected to be
reshaped as it gets used at the table.

## Database schema and migrations

`CharacterSheet` is stored as flattened scalar columns on `characters`
(header, attributes, combat stats, resources, currency, armor) plus six
child tables for its lists: `character_skills`, `character_secondary_skills`,
`character_abilities`, `character_inventory_items`, `character_weapons`, and
`character_weapon_properties` — no JSON blob. Schema changes are real
[Flyway](https://flywaydb.org/) migrations under
`modules/server/src/main/resources/db/migration/V<N>__*.sql`, applied on
boot before the connection pool is built (see `Db.scala`).
