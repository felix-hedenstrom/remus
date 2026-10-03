package dnd.shared

enum Attribute derives CanEqual, Codec, Schema:
  case Strength, Constitution, Agility, Intelligence, Will, Charisma
