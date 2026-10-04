package nu.fxh.remus.server

import nu.fxh.remus.shared.NonEmptyString
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import zio.Chunk
import zio.Config
import zio.config.derivation.kebabCase
import zio.config.magnolia.DeriveConfig

// zio-config has no Iron integration, so this is written by hand - visible
// package-wide (nu.fxh.remus.server) without an import, the same way the rest of this
// codebase relies on same-package top-level givens for Iron-refined fields.
// DeriveConfig (not plain Config) because that's the typeclass zio-config-magnolia's
// case-class derivation actually searches for.
given DeriveConfig[NonEmptyString] =
  DeriveConfig(
    Config.string.mapOrFail(s => s.refineEither[Not[Blank]].left.map(msg => Config.Error.InvalidData(Chunk.empty, msg)))
  )

@kebabCase
final case class AppConfig(
  port: Int,
  adminPort: Int,
  dbPath: NonEmptyString,
  discordClientId: String,
  discordClientSecret: String,
  discordRedirectUri: String
)
