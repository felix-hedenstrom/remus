CREATE TABLE users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL,
  discord_id TEXT NOT NULL UNIQUE
);

CREATE TABLE sessions (
  token TEXT PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expires_at INTEGER NOT NULL
);

CREATE TABLE characters (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  owner_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  -- CharacterHeader
  name TEXT NOT NULL,
  species TEXT NOT NULL,
  age_category TEXT NOT NULL,
  profession TEXT NOT NULL,
  weakness TEXT NOT NULL,
  appearance TEXT NOT NULL,
  portrait TEXT,

  -- Attributes
  attr_strength_value INTEGER NOT NULL,
  attr_strength_distressed INTEGER NOT NULL,
  attr_constitution_value INTEGER NOT NULL,
  attr_constitution_distressed INTEGER NOT NULL,
  attr_agility_value INTEGER NOT NULL,
  attr_agility_distressed INTEGER NOT NULL,
  attr_intelligence_value INTEGER NOT NULL,
  attr_intelligence_distressed INTEGER NOT NULL,
  attr_will_value INTEGER NOT NULL,
  attr_will_distressed INTEGER NOT NULL,
  attr_charisma_value INTEGER NOT NULL,
  attr_charisma_distressed INTEGER NOT NULL,

  -- CombatStats
  damage_bonus_str TEXT NOT NULL,
  damage_bonus_agl TEXT NOT NULL,
  movement INTEGER NOT NULL,

  -- Resources
  willpower_max INTEGER NOT NULL,
  willpower_current INTEGER NOT NULL,
  body_points_max INTEGER NOT NULL,
  body_points_current INTEGER NOT NULL,
  death_rolls_successes INTEGER NOT NULL,
  death_rolls_failures INTEGER NOT NULL,

  -- Currency
  gold INTEGER NOT NULL,
  silver INTEGER NOT NULL,
  copper INTEGER NOT NULL,

  -- Armor
  armor_type TEXT NOT NULL,
  armor_protection INTEGER NOT NULL,
  armor_penalty_sneaking INTEGER NOT NULL,
  armor_penalty_evade INTEGER NOT NULL,
  armor_penalty_acrobatics INTEGER NOT NULL,
  helmet_type TEXT NOT NULL,
  helmet_protection INTEGER NOT NULL,
  helmet_penalty_spot_hidden INTEGER NOT NULL,
  helmet_penalty_ranged_attacks INTEGER NOT NULL,

  -- Inventory scalars (items is a child table below)
  carry_capacity INTEGER NOT NULL,
  keepsake TEXT NOT NULL
);

CREATE INDEX idx_characters_owner ON characters(owner_user_id);

-- Skill is a fixed 30-value enum, always fully populated: one row per
-- enum value, no ordering column needed. Display order is reconstructed
-- in Scala via Skill.generalSkillsOrdered/weaponSkillsOrdered.
CREATE TABLE character_skills (
  character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
  skill TEXT NOT NULL,
  value INTEGER NOT NULL,
  marked_for_advancement INTEGER NOT NULL,
  PRIMARY KEY (character_id, skill)
);

CREATE TABLE character_secondary_skills (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
  position INTEGER NOT NULL,
  name TEXT NOT NULL,
  value INTEGER NOT NULL,
  attribute TEXT NOT NULL,
  marked_for_advancement INTEGER NOT NULL
);

CREATE INDEX idx_character_secondary_skills_character ON character_secondary_skills(character_id);

CREATE TABLE character_abilities (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
  position INTEGER NOT NULL,
  text TEXT NOT NULL
);

CREATE INDEX idx_character_abilities_character ON character_abilities(character_id);

CREATE TABLE character_inventory_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
  position INTEGER NOT NULL,
  text TEXT NOT NULL,
  weight TEXT NOT NULL
);

CREATE INDEX idx_character_inventory_items_character ON character_inventory_items(character_id);

CREATE TABLE character_weapons (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  character_id INTEGER NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
  position INTEGER NOT NULL,
  name TEXT NOT NULL,
  grip TEXT NOT NULL,
  weapon_range TEXT NOT NULL,
  damage TEXT NOT NULL,
  break_value TEXT NOT NULL
);

CREATE INDEX idx_character_weapons_character ON character_weapons(character_id);

CREATE TABLE character_weapon_properties (
  weapon_id INTEGER NOT NULL REFERENCES character_weapons(id) ON DELETE CASCADE,
  property TEXT NOT NULL,
  PRIMARY KEY (weapon_id, property)
);
