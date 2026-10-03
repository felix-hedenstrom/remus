package dnd.shared

export io.circe.Codec
export sttp.tapir.Schema
export io.github.iltotore.iron.circe.given

// sttp.tapir.codec.iron is a plain package (its givens live in the package
// object sttp.tapir.codec.iron, a separate compilation unit) - Scala disallows
// a wildcard `export` with a package as the prefix, so this one stays a
// per-file `import sttp.tapir.codec.iron.given` instead.
