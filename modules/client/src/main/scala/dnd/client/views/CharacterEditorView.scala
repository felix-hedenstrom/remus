package dnd.client
package views

import com.raquo.laminar.api.L.*
import dnd.shared.*

object CharacterEditorView:

  def apply(id: Long): Element =
    val sheetVar: Var[Option[CharacterSheet]] = Var(None)
    val saving                                = Var(false)
    val savedJustNow                          = Var(false)

    def load(): Unit =
      AppRuntime.run(Api.getCharacter(id))(
        onSuccess = {
          case Right(character) => sheetVar.set(Some(character.sheet))
          case Left(err)        => AppState.showError(err.message)
        },
        onFailure = t => AppState.showError(t.getMessage)
      )

    def save(): Unit =
      sheetVar.now().foreach { sheet =>
        saving.set(true)
        AppRuntime.run(Api.updateCharacter(id, sheet))(
          onSuccess = {
            case Right(_) =>
              saving.set(false)
              savedJustNow.set(true)
            case Left(err) =>
              saving.set(false)
              AppState.showError(err.message)
          },
          onFailure = t => { saving.set(false); AppState.showError(t.getMessage) }
        )
      }

    def update(f: CharacterSheet => CharacterSheet): Unit =
      sheetVar.update(_.map(f))
      savedJustNow.set(false)

    def textField(labelText: String, get: CharacterSheet => String, set: (CharacterSheet, String) => CharacterSheet) =
      div(
        cls := "field",
        label(labelText),
        input(
          typ := "text",
          value <-- sheetVar.signal.map(_.map(get).getOrElse("")),
          onInput.mapToValue --> (v => update(s => set(s, v)))
        )
      )

    def intField(labelText: String, get: CharacterSheet => Int, set: (CharacterSheet, Int) => CharacterSheet) =
      div(
        cls := "field field-narrow",
        label(labelText),
        input(
          typ := "number",
          value <-- sheetVar.signal.map(_.map(s => get(s).toString).getOrElse("0")),
          onInput.mapToValue --> (v => update(s => set(s, v.toIntOption.getOrElse(0))))
        )
      )

    def checkboxField(labelText: String, get: CharacterSheet => Boolean, set: (CharacterSheet, Boolean) => CharacterSheet) =
      label(
        cls := "checkbox-field",
        input(
          typ := "checkbox",
          checked <-- sheetVar.signal.map(_.exists(get)),
          onClick.mapToChecked --> (v => update(s => set(s, v)))
        ),
        labelText
      )

    def attributeBlock(name: String, get: Attributes => AttributeScore, set: (Attributes, AttributeScore) => Attributes) =
      div(
        cls := "attribute",
        div(cls := "attribute-name", name),
        input(
          typ := "number",
          cls := "attribute-value",
          value <-- sheetVar.signal.map(_.map(s => get(s.attributes).value.toString).getOrElse("0")),
          onInput.mapToValue --> (v =>
            update(s => s.copy(attributes = set(s.attributes, get(s.attributes).copy(value = v.toIntOption.getOrElse(0)))))
          )
        ),
        label(
          cls := "checkbox-field",
          input(
            typ := "checkbox",
            checked <-- sheetVar.signal.map(_.exists(s => get(s.attributes).distressed)),
            onClick.mapToChecked --> (v =>
              update(s => s.copy(attributes = set(s.attributes, get(s.attributes).copy(distressed = v))))
            )
          ),
          "Påverkad"
        )
      )

    def resourceTrack(name: String, get: Resources => ResourceTrack, set: (Resources, ResourceTrack) => Resources) =
      div(
        cls := "resource",
        div(cls := "resource-name", name),
        input(
          typ := "number",
          cls := "resource-value",
          value <-- sheetVar.signal.map(_.map(s => get(s.resources).current.toString).getOrElse("0")),
          onInput.mapToValue --> (v =>
            update(s => s.copy(resources = set(s.resources, get(s.resources).copy(current = v.toIntOption.getOrElse(0)))))
          )
        ),
        span(" / "),
        input(
          typ := "number",
          cls := "resource-value",
          value <-- sheetVar.signal.map(_.map(s => get(s.resources).max.toString).getOrElse("0")),
          onInput.mapToValue --> (v =>
            update(s => s.copy(resources = set(s.resources, get(s.resources).copy(max = v.toIntOption.getOrElse(0)))))
          )
        )
      )

    def skillRow(skill: Skill) =
      div(
        cls := "skill-row",
        span(cls := "skill-name", Labels.skill(skill)),
        span(cls := "skill-attr", Labels.attribute(skill.attribute)),
        input(
          typ := "number",
          cls := "skill-value",
          value <-- sheetVar.signal.map(
            _.flatMap(_.skills.find(_.skill == skill)).map(_.value.toString).getOrElse("0")
          ),
          onInput.mapToValue --> { v =>
            val newValue = v.toIntOption.getOrElse(0)
            update(s => s.copy(skills = s.skills.map(sv => if sv.skill == skill then sv.copy(value = newValue) else sv)))
          }
        )
      )

    def weaponRow(index: Int, weapon: Weapon) =
      def field(get: Weapon => String, set: (Weapon, String) => Weapon, placeholderText: String) =
        input(
          typ := "text",
          placeholder := placeholderText,
          value := get(weapon),
          onInput.mapToValue --> (v =>
            update(s => s.copy(weapons = s.weapons.updated(index, set(s.weapons(index), v))))
          )
        )
      div(
        cls := "weapon-row",
        field(_.name, (w, v) => w.copy(name = v), "Vapen"),
        field(_.grip, (w, v) => w.copy(grip = v), "Grepp"),
        field(_.range, (w, v) => w.copy(range = v), "Räckvidd"),
        field(_.damage, (w, v) => w.copy(damage = v), "Skada"),
        field(_.breakValue, (w, v) => w.copy(breakValue = v), "Brytvärde"),
        field(_.properties, (w, v) => w.copy(properties = v), "Egenskaper"),
        button(tpe := "button", "Ta bort", onClick --> (_ => update(s => s.copy(weapons = s.weapons.patch(index, Nil, 1)))))
      )

    def itemRow(index: Int, item: InventoryItem) =
      div(
        cls := "item-row",
        input(
          typ := "text",
          value := item.text,
          onInput.mapToValue --> (v => update(s => s.copy(inventory = s.inventory.copy(items = s.inventory.items.updated(index, InventoryItem(v))))))
        ),
        button(tpe := "button", "Ta bort", onClick --> (_ => update(s => s.copy(inventory = s.inventory.copy(items = s.inventory.items.patch(index, Nil, 1))))))
      )

    def secondarySkillRow(index: Int, skill: SecondarySkill) =
      div(
        cls := "secondary-skill-row",
        input(
          typ := "text",
          placeholder := "Namn",
          value := skill.name,
          onInput.mapToValue --> (v => update(s => s.copy(secondarySkills = s.secondarySkills.updated(index, s.secondarySkills(index).copy(name = v)))))
        ),
        input(
          typ := "number",
          value := skill.value.toString,
          onInput.mapToValue --> (v =>
            update(s => s.copy(secondarySkills = s.secondarySkills.updated(index, s.secondarySkills(index).copy(value = v.toIntOption.getOrElse(0)))))
          )
        ),
        button(tpe := "button", "Ta bort", onClick --> (_ => update(s => s.copy(secondarySkills = s.secondarySkills.patch(index, Nil, 1)))))
      )

    div(
      cls := "character-editor-view",
      onMountCallback(_ => load()),
      div(
        cls := "toolbar",
        button(tpe := "button", "Tillbaka", onClick --> (_ => AppState.page.set(Page.CharacterList))),
        button(tpe := "button", disabled <-- saving.signal, "Spara", onClick --> (_ => save())),
        child.text <-- savedJustNow.signal.map(if _ then "Sparat!" else "")
      ),
      child <-- sheetVar.signal.map {
        case None => div("Laddar...")
        case Some(_) =>
          div(
            div(
              cls := "header-section",
              textField("Namn", _.header.name, (s, v) => s.copy(header = s.header.copy(name = v))),
              textField("Spelare", _.header.playerName, (s, v) => s.copy(header = s.header.copy(playerName = v))),
              textField("Släkte", _.header.species, (s, v) => s.copy(header = s.header.copy(species = v))),
              textField("Ålder", _.header.ageCategory, (s, v) => s.copy(header = s.header.copy(ageCategory = v))),
              textField("Yrke", _.header.profession, (s, v) => s.copy(header = s.header.copy(profession = v))),
              textField("Svaghet", _.header.weakness, (s, v) => s.copy(header = s.header.copy(weakness = v))),
              textField("Utseende", _.header.appearance, (s, v) => s.copy(header = s.header.copy(appearance = v)))
            ),
            div(
              cls := "attributes-section",
              h2("Egenskaper"),
              attributeBlock("STY", _.str, (a, v) => a.copy(str = v)),
              attributeBlock("FYS", _.con, (a, v) => a.copy(con = v)),
              attributeBlock("SMI", _.agl, (a, v) => a.copy(agl = v)),
              attributeBlock("INT", _.int, (a, v) => a.copy(int = v)),
              attributeBlock("PSY", _.wil, (a, v) => a.copy(wil = v)),
              attributeBlock("KAR", _.cha, (a, v) => a.copy(cha = v))
            ),
            div(
              cls := "combat-section",
              h2("Strid & förflyttning"),
              textField("Skadebonus STY", _.combatStats.damageBonusStr, (s, v) => s.copy(combatStats = s.combatStats.copy(damageBonusStr = v))),
              textField("Skadebonus SMI", _.combatStats.damageBonusAgl, (s, v) => s.copy(combatStats = s.combatStats.copy(damageBonusAgl = v))),
              intField("Förflyttning", _.combatStats.movement, (s, v) => s.copy(combatStats = s.combatStats.copy(movement = v)))
            ),
            div(
              cls := "resources-section",
              h2("Poäng"),
              resourceTrack("Viljepoäng", _.willpower, (r, v) => r.copy(willpower = v)),
              resourceTrack("Kroppspoäng", _.bodyPoints, (r, v) => r.copy(bodyPoints = v)),
              intField(
                "Lyckade dödsslag",
                _.resources.deathRolls.successes,
                (s, v) => s.copy(resources = s.resources.copy(deathRolls = s.resources.deathRolls.copy(successes = v)))
              ),
              intField(
                "Misslyckade dödsslag",
                _.resources.deathRolls.failures,
                (s, v) => s.copy(resources = s.resources.copy(deathRolls = s.resources.deathRolls.copy(failures = v)))
              )
            ),
            div(
              cls := "skills-section",
              h2("Färdigheter"),
              div(cls := "skills-grid", children <-- sheetVar.signal.map(_ => Skill.generalSkillsOrdered.map(skillRow))),
              h2("Vapenfärdigheter"),
              div(cls := "skills-grid", children <-- sheetVar.signal.map(_ => Skill.weaponSkillsOrdered.map(skillRow))),
              h2("Sekundära färdigheter"),
              div(children <-- sheetVar.signal.map(_.map(_.secondarySkills).getOrElse(Nil).zipWithIndex.map { case (sk, i) => secondarySkillRow(i, sk) })),
              button(
                tpe := "button",
                "Lägg till sekundär färdighet",
                onClick --> (_ => update(s => s.copy(secondarySkills = s.secondarySkills :+ SecondarySkill("", 0))))
              )
            ),
            div(
              cls := "abilities-section",
              h2("Förmågor & besvärjelser"),
              textArea(
                rows := 6,
                value <-- sheetVar.signal.map(_.map(_.abilitiesText).getOrElse("")),
                onInput.mapToValue --> (v => update(s => s.copy(abilitiesText = v)))
              )
            ),
            div(
              cls := "armor-section",
              h2("Rustning & hjälm"),
              intField("Skyddsvärde rustning", _.armor.protection, (s, v) => s.copy(armor = s.armor.copy(protection = v))),
              checkboxField("Nackdel: Smyga", _.armor.penalties.sneaking, (s, v) => s.copy(armor = s.armor.copy(penalties = s.armor.penalties.copy(sneaking = v)))),
              checkboxField("Nackdel: Undvika", _.armor.penalties.evade, (s, v) => s.copy(armor = s.armor.copy(penalties = s.armor.penalties.copy(evade = v)))),
              checkboxField(
                "Nackdel: Hoppa & klättra",
                _.armor.penalties.acrobatics,
                (s, v) => s.copy(armor = s.armor.copy(penalties = s.armor.penalties.copy(acrobatics = v)))
              ),
              intField("Skyddsvärde hjälm", _.armor.helmetProtection, (s, v) => s.copy(armor = s.armor.copy(helmetProtection = v))),
              checkboxField(
                "Nackdel: Upptäcka fara",
                _.armor.helmetPenalties.spotHidden,
                (s, v) => s.copy(armor = s.armor.copy(helmetPenalties = s.armor.helmetPenalties.copy(spotHidden = v)))
              ),
              checkboxField(
                "Nackdel: Avståndsattacker",
                _.armor.helmetPenalties.rangedAttacks,
                (s, v) => s.copy(armor = s.armor.copy(helmetPenalties = s.armor.helmetPenalties.copy(rangedAttacks = v)))
              )
            ),
            div(
              cls := "weapons-section",
              h2("Vapen"),
              div(children <-- sheetVar.signal.map(_.map(_.weapons).getOrElse(Nil).zipWithIndex.map { case (w, i) => weaponRow(i, w) })),
              button(
                tpe := "button",
                "Lägg till vapen",
                onClick --> (_ => update(s => s.copy(weapons = s.weapons :+ Weapon("", "", "", "", "", ""))))
              )
            ),
            div(
              cls := "inventory-section",
              h2("Packning"),
              intField("Bärförmåga", _.inventory.carryCapacity, (s, v) => s.copy(inventory = s.inventory.copy(carryCapacity = v))),
              div(children <-- sheetVar.signal.map(_.map(_.inventory.items).getOrElse(Nil).zipWithIndex.map { case (it, i) => itemRow(i, it) })),
              button(
                tpe := "button",
                "Lägg till sak",
                onClick --> (_ => update(s => s.copy(inventory = s.inventory.copy(items = s.inventory.items :+ InventoryItem("")))))
              ),
              textField("Minnessak", _.inventory.keepsake, (s, v) => s.copy(inventory = s.inventory.copy(keepsake = v)))
            ),
            div(
              cls := "currency-section",
              h2("Pengar"),
              intField("Guldmynt", _.currency.gold, (s, v) => s.copy(currency = s.currency.copy(gold = v))),
              intField("Silvermynt", _.currency.silver, (s, v) => s.copy(currency = s.currency.copy(silver = v))),
              intField("Kopparmynt", _.currency.copper, (s, v) => s.copy(currency = s.currency.copy(copper = v)))
            )
          )
      }
    )
