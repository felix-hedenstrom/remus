package dnd.client
package views

import com.raquo.laminar.api.L.*
import dnd.shared.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.autoRefine
import io.github.iltotore.iron.constraint.all.*
import org.scalajs.dom

object CharacterEditorView:

  private val PortraitMaxBytes = 15L * 1024 * 1024

  private def parseNonNegative(v: String): NonNegativeInt =
    v.toIntOption.getOrElse(0).max(0).refineUnsafe[GreaterEqual[0]]

  private def parseAttributeValue(v: String): AttributeValue =
    v.toIntOption.getOrElse(0).max(0).min(20).refineUnsafe[Interval.Closed[0, 20]]

  // Trusted-default fallback so clearing a text field mid-edit can't crash
  // on an empty NonEmptyString - "-" stands in until the user types again.
  private def parseNonEmpty(v: String): NonEmptyString =
    (if v.trim.isEmpty then "-" else v).refineUnsafe[Not[Blank]]

  def apply(id: Long): Element =
    val sheetVar: Var[Option[CharacterSheet]] = Var(None)
    val saving                                = Var(false)
    val savedJustNow                          = Var(false)
    // Separate from sheetVar (which changes on every keystroke) and set
    // exactly once, so the signal driving the form's mount below can never
    // re-fire mid-edit regardless of Signal dedup semantics.
    val loaded: Var[Boolean] = Var(false)

    def load(): Unit =
      AppRuntime.run(Api.getCharacter(id))(
        onSuccess = {
          case Right(character) =>
            sheetVar.set(Some(character.sheet))
            loaded.set(true)
          case Left(err) => AppState.showError(err.message)
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

    def nonEmptyTextField(labelText: String, get: CharacterSheet => NonEmptyString, set: (CharacterSheet, NonEmptyString) => CharacterSheet) =
      div(
        cls := "field",
        label(labelText),
        input(
          typ := "text",
          value <-- sheetVar.signal.map(_.map(get).map(_.toString).getOrElse("")),
          onInput.mapToValue --> (v => update(s => set(s, parseNonEmpty(v))))
        )
      )

    def nonEmptyTextAreaField(labelText: String, get: CharacterSheet => NonEmptyString, set: (CharacterSheet, NonEmptyString) => CharacterSheet) =
      div(
        cls := "field field-textarea",
        label(labelText),
        textArea(
          value <-- sheetVar.signal.map(_.map(get).map(_.toString).getOrElse("")),
          onInput.mapToValue --> (v => update(s => set(s, parseNonEmpty(v))))
        )
      )

    // The hidden file input is read via `ev.target`, not a self-reference to
    // `fileInput`, to sidestep the forward-reference restriction on local
    // vals; the visible box only ever triggers it via `.ref.click()`.
    //
    // Resizing is deliberately NOT done here via <canvas>: reading pixel data
    // back out of a canvas (drawImage + toDataURL) is exactly what Firefox's
    // privacy.resistFingerprinting (and similar protections in Brave/Tor)
    // silently corrupts into rainbow/striped garbage, since that readback
    // never happens synchronously inside the user's click gesture. The raw
    // file bytes are sent to the server as-is; the server downscales and
    // recompresses using its own (non-browser) image libraries instead.
    def nameTitleAndPortrait() =
      val fileInput = input(
        typ := "file",
        cls := "portrait-file-input",
        accept := "image/*",
        onChange --> { ev =>
          val target = ev.target.asInstanceOf[dom.html.Input]
          for
            files <- Option(target.files)
            if files.length > 0
          do
            val file = files(0)
            if file.size > PortraitMaxBytes then
              AppState.showError("Bilden är för stor (max 15 MB).")
            else
              val reader = new dom.FileReader
              reader.onload = _ =>
                update(s => s.copy(header = s.header.copy(portrait = Some(reader.result.asInstanceOf[String]))))
              reader.readAsDataURL(file)
            target.value = ""
        }
      )
      div(
        cls := "header-top",
        div(
          cls := "portrait-box",
          onClick --> (_ => fileInput.ref.click()),
          fileInput,
          child <-- sheetVar.signal.map(_.flatMap(_.header.portrait)).map {
            case Some(dataUrl) => img(cls := "portrait-img", src := dataUrl)
            case None          => span(cls := "portrait-placeholder", "Lägg till bild")
          }
        ),
        input(
          cls := "name-title",
          typ := "text",
          placeholder := "Namn",
          value <-- sheetVar.signal.map(_.map(_.header.name).map(_.toString).getOrElse("")),
          onInput.mapToValue --> (v => update(s => s.copy(header = s.header.copy(name = parseNonEmpty(v)))))
        )
      )

    val customMarker = "__custom__"

    def speciesField(labelText: String) =
      // The custom-name input is created once (not inside a signal-driven
      // child block) so typing into it doesn't tear down and rebuild the
      // element - and lose focus - on every keystroke.
      val customInput = input(
        typ := "text",
        placeholder := "Släkte",
        value <-- sheetVar.signal.map(_.map(_.header.species) collect { case Species.Custom(name) => name.toString } getOrElse ""),
        onInput.mapToValue --> (v =>
          update(s => s.copy(header = s.header.copy(species = Species.Custom(parseNonEmpty(v)))))
        )
      )
      div(
        cls := "field field-select-custom",
        label(labelText),
        select(
          value <-- sheetVar.signal.map(_.map(_.header.species).collect { case Species.Custom(_) => customMarker }.getOrElse(Species.Human.label)),
          onChange.mapToValue --> { v =>
            val newSpecies = if v == customMarker then Species.Custom("Annat".refineUnsafe[Not[Blank]]) else Species.Human
            update(s => s.copy(header = s.header.copy(species = newSpecies)))
          },
          option(value := Species.Human.label, Labels.species(Species.Human)),
          option(value := customMarker, "Annat...")
        ),
        child <-- sheetVar.signal.map(_.exists(_.header.species.isInstanceOf[Species.Custom])).map {
          if _ then customInput else emptyNode
        }
      )

    def professionField(labelText: String) =
      val customInput = input(
        typ := "text",
        placeholder := "Yrke",
        value <-- sheetVar.signal.map(_.map(_.header.profession) collect { case Profession.Custom(name) => name.toString } getOrElse ""),
        onInput.mapToValue --> (v =>
          update(s => s.copy(header = s.header.copy(profession = Profession.Custom(parseNonEmpty(v)))))
        )
      )
      div(
        cls := "field field-select-custom",
        label(labelText),
        select(
          value <-- sheetVar.signal.map(_.map(_.header.profession).collect { case Profession.Custom(_) => customMarker }.getOrElse(Profession.Warrior.label)),
          onChange.mapToValue --> { v =>
            val newProfession = if v == customMarker then Profession.Custom("Annat".refineUnsafe[Not[Blank]]) else Profession.Warrior
            update(s => s.copy(header = s.header.copy(profession = newProfession)))
          },
          option(value := Profession.Warrior.label, Labels.profession(Profession.Warrior)),
          option(value := customMarker, "Annat...")
        ),
        child <-- sheetVar.signal.map(_.exists(_.header.profession.isInstanceOf[Profession.Custom])).map {
          if _ then customInput else emptyNode
        }
      )

    def damageBonusField(labelText: String, get: CharacterSheet => DamageBonus, set: (CharacterSheet, DamageBonus) => CharacterSheet) =
      div(
        cls := "field field-narrow",
        label(labelText),
        select(
          value <-- sheetVar.signal.map(_.map(get).map(_.ordinal.toString).getOrElse("0")),
          onChange.mapToValue --> (v => update(s => set(s, DamageBonus.fromOrdinal(v.toInt)))),
          DamageBonus.values.map(d => option(value := d.ordinal.toString, Labels.damageBonus(d)))
        )
      )

    def ageCategoryField(labelText: String) =
      div(
        cls := "field",
        label(labelText),
        select(
          value <-- sheetVar.signal.map(_.map(_.header.ageCategory).map(_.ordinal.toString).getOrElse("0")),
          onChange.mapToValue --> (v =>
            update(s => s.copy(header = s.header.copy(ageCategory = AgeCategory.fromOrdinal(v.toInt))))
          ),
          AgeCategory.values.map(a => option(value := a.ordinal.toString, Labels.ageCategory(a)))
        )
      )

    // Wraps a number input with themed +/- buttons that read/write through
    // the same get/set the input itself uses, so every call site stays the
    // source of truth for how its value is read and clamped (parseNonNegative
    // already floors at 0, so a decrement below 0 settles at 0 for free).
    def sectionHeader(title: String, iconClass: String) =
      h2(span(cls := s"h2-icon $iconClass"), title)

    def stepButton(symbol: String, onClick0: () => Unit) =
      button(tpe := "button", cls := "num-step", symbol, onClick --> (_ => onClick0()))

    def intField(labelText: String, get: CharacterSheet => NonNegativeInt, set: (CharacterSheet, NonNegativeInt) => CharacterSheet) =
      def bump(delta: Int): Unit = update(s => set(s, parseNonNegative((get(s).toInt + delta).toString)))
      div(
        cls := "field field-narrow",
        label(labelText),
        div(
          cls := "num-stepper",
          stepButton("−", () => bump(-1)),
          input(
            typ := "number",
            value <-- sheetVar.signal.map(_.map(s => get(s).toString).getOrElse("0")),
            onInput.mapToValue --> (v => update(s => set(s, parseNonNegative(v))))
          ),
          stepButton("+", () => bump(1))
        )
      )

    def currencyRow(iconClass: String, labelText: String, get: CharacterSheet => NonNegativeInt, set: (CharacterSheet, NonNegativeInt) => CharacterSheet) =
      def bump(delta: Int): Unit = update(s => set(s, parseNonNegative((get(s).toInt + delta).toString)))
      div(
        cls := "currency-row",
        div(cls := s"currency-icon $iconClass", title := labelText),
        div(
          cls := "num-stepper",
          stepButton("−", () => bump(-1)),
          input(
            typ := "number",
            value <-- sheetVar.signal.map(_.map(s => get(s).toString).getOrElse("0")),
            onInput.mapToValue --> (v => update(s => set(s, parseNonNegative(v))))
          ),
          stepButton("+", () => bump(1))
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

    def attributeBlock(
      name: String,
      conditionLabel: String,
      get: Attributes => AttributeScore,
      set: (Attributes, AttributeScore) => Attributes
    ) =
      div(
        cls := "attribute",
        div(cls := "attribute-name", name),
        div(
          cls := "attribute-ring",
          input(
            typ := "number",
            cls := "attribute-value",
            value <-- sheetVar.signal.map(_.map(s => get(s.attributes).value.toString).getOrElse("0")),
            onInput.mapToValue --> (v =>
              update(s => s.copy(attributes = set(s.attributes, get(s.attributes).copy(value = parseAttributeValue(v)))))
            )
          )
        ),
        label(
          cls := "attribute-toggle-field",
          input(
            typ := "checkbox",
            cls := "attribute-toggle",
            checked <-- sheetVar.signal.map(_.exists(s => get(s.attributes).distressed)),
            onClick.mapToChecked --> (v =>
              update(s => s.copy(attributes = set(s.attributes, get(s.attributes).copy(distressed = v))))
            )
          ),
          conditionLabel
        )
      )

    def resourceTrack(
      name: String,
      pipClass: String,
      get: Resources => ResourceTrack,
      set: (Resources, ResourceTrack) => Resources,
      extra: Modifier[Div]*
    ) =
      val mods: Seq[Modifier[Div]] = Seq(
        cls := s"resource-track $pipClass",
        div(cls := "resource-name", name),
        div(
          cls := "pip-row",
          children <-- sheetVar.signal.map { sheetOpt =>
            val track   = sheetOpt.map(s => get(s.resources))
            val max     = track.map(_.max.toInt).getOrElse(0)
            val current = track.map(_.current.toInt).getOrElse(0)
            (1 to max).map { i =>
              def toggle(): Unit =
                update { s =>
                  val t          = get(s.resources)
                  val newCurrent = if t.current.toInt == i then i - 1 else i
                  s.copy(resources = set(s.resources, t.copy(current = parseNonNegative(newCurrent.toString))))
                }
              div(
                cls := ("pip" + (if i <= current then " filled" else "")),
                tabIndex := 0,
                onClick --> (_ => toggle()),
                onKeyDown --> { ev =>
                  if ev.key == "Enter" || ev.key == " " then
                    ev.preventDefault()
                    toggle()
                }
              )
            }
          }
        ),
        div(
          cls := "field field-narrow",
          label("Max"),
          div(
            cls := "num-stepper",
            stepButton(
              "−",
              () => update(s => s.copy(resources = set(s.resources, get(s.resources).copy(max = parseNonNegative((get(s.resources).max.toInt - 1).toString)))))
            ),
            input(
              typ := "number",
              cls := "resource-max",
              value <-- sheetVar.signal.map(_.map(s => get(s.resources).max.toString).getOrElse("0")),
              onInput.mapToValue --> (v =>
                update(s => s.copy(resources = set(s.resources, get(s.resources).copy(max = parseNonNegative(v)))))
              )
            ),
            stepButton(
              "+",
              () => update(s => s.copy(resources = set(s.resources, get(s.resources).copy(max = parseNonNegative((get(s.resources).max.toInt + 1).toString)))))
            )
          )
        )
      ) ++ extra
      div(mods*)

    // Toggled by clicking a skill's mark - the sheet's record that the skill
    // was rolled with a 1 or 20 (dragon/demon), making it eligible to
    // increase at the next level-up.
    def toggleSkillMark(skill: Skill): Unit =
      update(s => s.copy(skills = s.skills.map(sv => if sv.skill == skill then sv.copy(markedForAdvancement = !sv.markedForAdvancement) else sv)))

    def skillRow(skill: Skill) =
      div(
        cls := "skill-row",
        cls("has-bane") <-- sheetVar.signal.map(_.exists(s => s.attributes.score(skill.attribute).distressed)),
        span(
          cls := "skill-mark",
          cls("marked") <-- sheetVar.signal.map(_.flatMap(_.skills.find(_.skill == skill)).exists(_.markedForAdvancement)),
          tabIndex := 0,
          title := "Markera för färdighetsökning (vid ett resultat av 1 eller 20)",
          onClick --> (_ => toggleSkillMark(skill)),
          onKeyDown --> { ev =>
            if ev.key == "Enter" || ev.key == " " then
              ev.preventDefault()
              toggleSkillMark(skill)
          }
        ),
        span(cls := "skill-name", Labels.skill(skill)),
        span(cls := "skill-attr", Labels.attribute(skill.attribute)),
        {
          def currentValue(sheetOpt: Option[CharacterSheet]): Int =
            sheetOpt.flatMap(_.skills.find(_.skill == skill)).map(_.value.toInt).getOrElse(0)
          def setValue(newValue: NonNegativeInt): Unit =
            update(s => s.copy(skills = s.skills.map(sv => if sv.skill == skill then sv.copy(value = newValue) else sv)))
          def bump(delta: Int): Unit = setValue(parseNonNegative((currentValue(sheetVar.now()) + delta).toString))
          div(
            cls := "num-stepper",
            stepButton("−", () => bump(-1)),
            input(
              typ := "number",
              cls := "skill-value",
              value <-- sheetVar.signal.map(s => currentValue(s).toString),
              onInput.mapToValue --> (v => setValue(parseNonNegative(v)))
            ),
            stepButton("+", () => bump(1))
          )
        }
      )

    // Rows below are rendered via `.split` on a signal of *indices* (not of
    // the row data itself), keyed by index - so a DOM row is created once
    // per index and reused across edits instead of being torn down on every
    // keystroke anywhere in the sheet. Each row then reads its own live
    // value via an index lookup into sheetVar, the same way skillRow does.
    // (Rendering these rows directly off `sheetVar.signal.map(_.list.zipWithIndex.map(rowFn))`
    // - as this used to - rebuilds every row's DOM on every keystroke, which
    // is what caused the focus-loss bug in "Sekundära färdigheter".)

    def weaponRow(index: Int) =
      def weaponAt(sheetOpt: Option[CharacterSheet]): Option[Weapon] = sheetOpt.flatMap(_.weapons.lift(index))
      def field(get: Weapon => String, set: (Weapon, String) => Weapon, placeholderText: String) =
        input(
          typ := "text",
          placeholder := placeholderText,
          value <-- sheetVar.signal.map(weaponAt(_).map(get).getOrElse("")),
          onInput.mapToValue --> (v =>
            update(s => s.copy(weapons = s.weapons.updated(index, set(s.weapons(index), v))))
          )
        )
      val gripSelect =
        select(
          value <-- sheetVar.signal.map(weaponAt(_).map(_.grip.ordinal.toString).getOrElse("0")),
          onChange.mapToValue --> (v =>
            update(s => s.copy(weapons = s.weapons.updated(index, s.weapons(index).copy(grip = Grip.fromOrdinal(v.toInt)))))
          ),
          Grip.values.map(g => option(value := g.ordinal.toString, Labels.grip(g)))
        )
      val selectedProperties = sheetVar.signal.map(weaponAt(_).map(_.properties).getOrElse(Set.empty[WeaponProperty]))
      def toggleProperty(p: WeaponProperty): Unit =
        update { s =>
          val w        = s.weapons(index)
          val newProps = if w.properties.contains(p) then w.properties - p else w.properties + p
          s.copy(weapons = s.weapons.updated(index, w.copy(properties = newProps)))
        }
      val propertiesPicker =
        detailsTag(
          cls := "weapon-properties",
          summaryTag(
            child.text <-- selectedProperties.map(ps =>
              if ps.isEmpty then "Välj..." else WeaponProperty.values.filter(ps).map(Labels.weaponProperty).mkString(", ")
            )
          ),
          div(
            cls := "weapon-properties-menu",
            WeaponProperty.values.map { p =>
              label(
                cls := "checkbox-field",
                input(
                  typ := "checkbox",
                  checked <-- selectedProperties.map(_.contains(p)),
                  onClick --> (_ => toggleProperty(p))
                ),
                Labels.weaponProperty(p)
              )
            }
          )
        )
      div(
        cls := "weapon-row",
        field(_.name, (w, v) => w.copy(name = v), "Vapen/Sköld"),
        gripSelect,
        field(_.range, (w, v) => w.copy(range = v), "Räckvidd"),
        field(_.damage, (w, v) => w.copy(damage = v), "Skada"),
        field(_.breakValue, (w, v) => w.copy(breakValue = v), "Brytvärde"),
        propertiesPicker,
        button(tpe := "button", cls := "remove-row", "×", onClick --> (_ => update(s => s.copy(weapons = s.weapons.patch(index, Nil, 1)))))
      )

    val weaponTableHeader =
      div(
        cls := "weapon-row weapon-table-header",
        span("Vapen/Sköld"),
        span("Grepp"),
        span("Räckvidd"),
        span("Skada"),
        span("Brytvärde"),
        span("Egenskaper"),
        span()
      )

    def itemRow(index: Int) =
      div(
        cls := "item-row",
        input(
          typ := "text",
          value <-- sheetVar.signal.map(_.flatMap(_.inventory.items.lift(index)).map(_.text).getOrElse("")),
          onInput.mapToValue --> (v => update(s => s.copy(inventory = s.inventory.copy(items = s.inventory.items.updated(index, InventoryItem(v))))))
        ),
        button(tpe := "button", cls := "remove-row", "×", onClick --> (_ => update(s => s.copy(inventory = s.inventory.copy(items = s.inventory.items.patch(index, Nil, 1))))))
      )

    def abilityRow(index: Int) =
      div(
        cls := "item-row",
        input(
          typ := "text",
          value <-- sheetVar.signal.map(_.flatMap(_.abilities.lift(index)).map(_.text).getOrElse("")),
          onInput.mapToValue --> (v => update(s => s.copy(abilities = s.abilities.updated(index, Ability(v)))))
        ),
        button(tpe := "button", cls := "remove-row", "×", onClick --> (_ => update(s => s.copy(abilities = s.abilities.patch(index, Nil, 1)))))
      )

    def updateSecondarySkill(index: Int)(f: SecondarySkill => SecondarySkill): Unit =
      update(s => s.copy(secondarySkills = s.secondarySkills.updated(index, f(s.secondarySkills(index)))))

    def secondarySkillRow(index: Int) =
      def skillAt(sheetOpt: Option[CharacterSheet]): Option[SecondarySkill] = sheetOpt.flatMap(_.secondarySkills.lift(index))
      div(
        cls := "secondary-skill-row",
        cls("has-bane") <-- sheetVar.signal.map { sheetOpt =>
          (sheetOpt, skillAt(sheetOpt)) match
            case (Some(sheet), Some(sk)) => sheet.attributes.score(sk.attribute).distressed
            case _                       => false
        },
        span(
          cls := "skill-mark",
          cls("marked") <-- sheetVar.signal.map(skillAt(_).exists(_.markedForAdvancement)),
          tabIndex := 0,
          title := "Markera för färdighetsökning (vid ett resultat av 1 eller 20)",
          onClick --> (_ => updateSecondarySkill(index)(sk => sk.copy(markedForAdvancement = !sk.markedForAdvancement))),
          onKeyDown --> { ev =>
            if ev.key == "Enter" || ev.key == " " then
              ev.preventDefault()
              updateSecondarySkill(index)(sk => sk.copy(markedForAdvancement = !sk.markedForAdvancement))
          }
        ),
        input(
          typ := "text",
          placeholder := "Namn",
          cls := "secondary-skill-name",
          value <-- sheetVar.signal.map(skillAt(_).map(_.name).getOrElse("")),
          onInput.mapToValue --> (v => updateSecondarySkill(index)(_.copy(name = v)))
        ),
        select(
          cls := "secondary-skill-attr",
          value <-- sheetVar.signal.map(skillAt(_).map(_.attribute.ordinal.toString).getOrElse("0")),
          onChange.mapToValue --> (v => updateSecondarySkill(index)(_.copy(attribute = Attribute.fromOrdinal(v.toInt)))),
          Attribute.values.map(a => option(value := a.ordinal.toString, Labels.attribute(a)))
        ),
        {
          def bump(delta: Int): Unit =
            val current = skillAt(sheetVar.now()).map(_.value.toInt).getOrElse(0)
            updateSecondarySkill(index)(_.copy(value = parseNonNegative((current + delta).toString)))
          div(
            cls := "num-stepper",
            stepButton("−", () => bump(-1)),
            input(
              typ := "number",
              cls := "skill-value",
              value <-- sheetVar.signal.map(skillAt(_).map(_.value.toString).getOrElse("0")),
              onInput.mapToValue --> (v => updateSecondarySkill(index)(_.copy(value = parseNonNegative(v))))
            ),
            stepButton("+", () => bump(1))
          )
        },
        button(tpe := "button", cls := "remove-row", "×", onClick --> (_ => update(s => s.copy(secondarySkills = s.secondarySkills.patch(index, Nil, 1)))))
      )

    val (generalSkillsCol1, generalSkillsCol2) =
      Skill.generalSkillsOrdered.splitAt((Skill.generalSkillsOrdered.size + 1) / 2)
    val (weaponSkillsCol1, weaponSkillsCol2) =
      Skill.weaponSkillsOrdered.splitAt((Skill.weaponSkillsOrdered.size + 1) / 2)

    div(
      cls := "character-editor-view",
      onMountCallback(_ => load()),
      div(
        cls := "toolbar",
        button(tpe := "button", "Tillbaka", onClick --> (_ => AppState.goToList())),
        // Grouped so the status text appearing/disappearing next to "Spara"
        // can't shift the button's own position - previously both were
        // direct siblings of this space-between toolbar, so the button
        // visibly jumped whenever "Sparat!" appeared or cleared.
        div(
          cls := "save-group",
          button(tpe := "button", cls := "primary", disabled <-- saving.signal, "Spara", onClick --> (_ => save())),
          span(cls := "save-status", child.text <-- savedJustNow.signal.map(if _ then "Sparat!" else ""))
        )
      ),
      // Keyed on `loaded` (set exactly once in load()), not on sheetVar's
      // value, so this whole form is built once and never torn down again.
      // Every field below reads/writes sheetVar independently via its own
      // `value <--`/`onInput` binding on a stable element; if this outer
      // block were keyed on sheetVar (which changes on every keystroke),
      // every edit in any field would rebuild the entire form and drop
      // input focus - which is exactly what happened before this fix.
      child <-- loaded.signal.map {
        case false => div("Laddar...")
        case true =>
          div(
            cls := "sheet-grid",
            div(
              cls := "top-row full-width",
              div(
                cls := "header-section",
                nameTitleAndPortrait(),
                div(
                  cls := "header-compact-row",
                  speciesField("Släkte"),
                  ageCategoryField("Ålder"),
                  professionField("Yrke")
                ),
                nonEmptyTextAreaField("Svaghet", _.header.weakness, (s, v) => s.copy(header = s.header.copy(weakness = v))),
                nonEmptyTextAreaField("Utseende", _.header.appearance, (s, v) => s.copy(header = s.header.copy(appearance = v)))
              ),
              div(
                cls := "weapons-section",
                sectionHeader("Vapen", "icon-sword"),
                weaponTableHeader,
                div(
                  children <-- sheetVar.signal
                    .map(_.map(_.weapons.indices.toList).getOrElse(Nil))
                    .split(identity)((idx, _, _) => weaponRow(idx))
                ),
                button(
                  tpe := "button",
                  "Lägg till vapen",
                  onClick --> (_ => update(s => s.copy(weapons = s.weapons :+ Weapon("", Grip.OneHanded, "", "", "", Set.empty))))
                )
              )
            ),
            div(
              cls := "attributes-section full-width",
              h2("Egenskaper"),
              attributeBlock("STY", "Utmattad", _.strength, (a, v) => a.copy(strength = v)),
              attributeBlock("FYS", "Krasslig", _.constitution, (a, v) => a.copy(constitution = v)),
              attributeBlock("SMI", "Omtöcknad", _.agility, (a, v) => a.copy(agility = v)),
              attributeBlock("INT", "Arg", _.intelligence, (a, v) => a.copy(intelligence = v)),
              attributeBlock("PSY", "Rädd", _.will, (a, v) => a.copy(will = v)),
              attributeBlock("KAR", "Uppgiven", _.charisma, (a, v) => a.copy(charisma = v)),
              div(
                cls := "combat-strip",
                damageBonusField("Skadebonus STY", _.combatStats.damageBonusStr, (s, v) => s.copy(combatStats = s.combatStats.copy(damageBonusStr = v))),
                damageBonusField("Skadebonus SMI", _.combatStats.damageBonusAgl, (s, v) => s.copy(combatStats = s.combatStats.copy(damageBonusAgl = v))),
                intField("Förflyttning", _.combatStats.movement, (s, v) => s.copy(combatStats = s.combatStats.copy(movement = v)))
              )
            ),
            div(
              cls := "col-3 stack",
              div(
                cls := "abilities-section",
                sectionHeader("Förmågor & besvärjelser", "icon-book"),
                div(
                  children <-- sheetVar.signal
                    .map(_.map(_.abilities.indices.toList).getOrElse(Nil))
                    .split(identity)((idx, _, _) => abilityRow(idx))
                ),
                button(
                  tpe := "button",
                  "Lägg till förmåga",
                  onClick --> (_ => update(s => s.copy(abilities = s.abilities :+ Ability(""))))
                ),
                resourceTrack("Viljepoäng", "willpower", _.willpower, (r, v) => r.copy(willpower = v)),
                resourceTrack(
                  "Kroppspoäng",
                  "body-points",
                  _.bodyPoints,
                  (r, v) => r.copy(bodyPoints = v),
                  div(
                    cls := "death-rolls",
                    // Death rolls only come into play at 0 body points, so
                    // keep them out of the way otherwise instead of always
                    // taking up space.
                    cls("hidden") <-- sheetVar.signal.map(_.exists(_.resources.bodyPoints.current.toInt > 0)),
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
                  )
                )
              )
            ),
            div(
              cls := "skills-section col-6",
              sectionHeader("Färdigheter", "icon-scroll"),
              div(
                cls := "skills-grid",
                div(cls := "skills-col", generalSkillsCol1.map(skillRow)),
                div(cls := "skills-col", generalSkillsCol2.map(skillRow))
              ),
              sectionHeader("Vapenfärdigheter", "icon-scroll"),
              div(
                cls := "skills-grid",
                div(cls := "skills-col", weaponSkillsCol1.map(skillRow)),
                div(cls := "skills-col", weaponSkillsCol2.map(skillRow))
              ),
              sectionHeader("Sekundära färdigheter", "icon-scroll"),
              div(
                children <-- sheetVar.signal
                  .map(_.map(_.secondarySkills.indices.toList).getOrElse(Nil))
                  .split(identity)((idx, _, _) => secondarySkillRow(idx))
              ),
              button(
                tpe := "button",
                "Lägg till sekundär färdighet",
                onClick --> (_ => update(s => s.copy(secondarySkills = s.secondarySkills :+ SecondarySkill("", 0, Attribute.Strength, false))))
              )
            ),
            div(
              cls := "col-3 stack",
              div(
                cls := "inventory-section",
                sectionHeader("Packning", "icon-backpack"),
                intField("Bärförmåga", _.inventory.carryCapacity, (s, v) => s.copy(inventory = s.inventory.copy(carryCapacity = v))),
                div(
                  children <-- sheetVar.signal
                    .map(_.map(_.inventory.items.indices.toList).getOrElse(Nil))
                    .split(identity)((idx, _, _) => itemRow(idx))
                ),
                button(
                  tpe := "button",
                  "Lägg till sak",
                  onClick --> (_ => update(s => s.copy(inventory = s.inventory.copy(items = s.inventory.items :+ InventoryItem("")))))
                ),
                textField("Minnessak", _.inventory.keepsake, (s, v) => s.copy(inventory = s.inventory.copy(keepsake = v))),
                div(
                  cls := "currency-rows",
                  currencyRow("gold", "Guldmynt", _.currency.gold, (s, v) => s.copy(currency = s.currency.copy(gold = v))),
                  currencyRow("silver", "Silvermynt", _.currency.silver, (s, v) => s.copy(currency = s.currency.copy(silver = v))),
                  currencyRow("copper", "Kopparmynt", _.currency.copper, (s, v) => s.copy(currency = s.currency.copy(copper = v)))
                )
              ),
              div(
                cls := "armor-section",
                sectionHeader("Rustning", "icon-shield"),
                textField("Rustningstyp", _.armor.armorType, (s, v) => s.copy(armor = s.armor.copy(armorType = v))),
                div(
                  cls := "armor-row",
                  div(cls := "icon-shield"),
                  intField("Skyddsvärde rustning", _.armor.protection, (s, v) => s.copy(armor = s.armor.copy(protection = v))),
                  checkboxField("Nackdel: Smyga", _.armor.penalties.sneaking, (s, v) => s.copy(armor = s.armor.copy(penalties = s.armor.penalties.copy(sneaking = v)))),
                  checkboxField("Nackdel: Undvika", _.armor.penalties.evade, (s, v) => s.copy(armor = s.armor.copy(penalties = s.armor.penalties.copy(evade = v)))),
                  checkboxField(
                    "Nackdel: Hoppa & klättra",
                    _.armor.penalties.acrobatics,
                    (s, v) => s.copy(armor = s.armor.copy(penalties = s.armor.penalties.copy(acrobatics = v)))
                  )
                )
              ),
              div(
                cls := "helmet-section",
                sectionHeader("Hjälm", "icon-helmet"),
                textField("Hjälmtyp", _.armor.helmetType, (s, v) => s.copy(armor = s.armor.copy(helmetType = v))),
                div(
                  cls := "armor-row",
                  div(cls := "icon-helmet"),
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
                )
              )
            )
          )
      }
    )
