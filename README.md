# Pellegrino Simulator

A self-hostable character sheet manager for the Drakar och Demoner tabletop
RPG (the modern Riotminds edition, published in English as *Dragonbane*).
Each player registers an account and manages their own character sheets —
attributes, skills, weapons, inventory, resources — through a web UI that
mirrors the physical sheet.

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

Then open http://localhost:8080. The SQLite database file is created at
`./data/pellegrino.db` by default (override with the `DB_PATH` env var; port
via `PORT`).

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

```sh
cd docker
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
authenticates with a username/password and owns the characters they create —
there's no campaign/GM grouping yet. The frontend is intentionally plain
(no component framework, no client-side routing) since it's expected to be
reshaped as it gets used at the table.
