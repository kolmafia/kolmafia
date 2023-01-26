package net.sourceforge.kolmafia.webui;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MallPriceManager;

public class BasementDecorator {
  private BasementDecorator() {}

  public static final void decorate(final StringBuffer buffer) {
    addBasementButtons(buffer);

    if (buffer.indexOf("Got Silk?") != -1) {
      BasementDecorator.addBasementChoiceSpoilers(buffer, "Moxie", "Muscle");
    } else if (buffer.indexOf("Save the Dolls") != -1) {
      BasementDecorator.addBasementChoiceSpoilers(buffer, "Mysticality", "Moxie");
    } else if (buffer.indexOf("Take the Red Pill") != -1) {
      BasementDecorator.addBasementChoiceSpoilers(buffer, "Muscle", "Mysticality");
    }

    addBasementSpoilers(buffer);
  }

  private static void addBasementButtons(final StringBuffer buffer) {
    if (!Preferences.getBoolean("relayAddsCustomCombat")) {
      return;
    }

    int insertionPoint = buffer.indexOf("<tr");
    if (insertionPoint == -1) {
      return;
    }

    StringBuffer actionBuffer = new StringBuffer("<tr><td align=left>");

    String autoAction =
        "document.location.href='basement.php?action="
            + BasementRequest.getBasementAction(buffer.toString())
            + "'; void(0);";
    BasementDecorator.addBasementButton("auto", autoAction, actionBuffer, true);
    BasementDecorator.addBasementButton(
        "rebuff", "runBasementScript(); void(0);", actionBuffer, false);
    BasementDecorator.addBasementButton(
        "refresh", "document.location.href='basement.php'; void(0);", actionBuffer, true);

    actionBuffer.append("</td></tr><tr><td><font size=1>&nbsp;</font></td></tr>");
    buffer.insert(insertionPoint, actionBuffer.toString());
  }

  private static void addBasementButton(
      final String label, final String action, final StringBuffer buffer, final boolean isEnabled) {
    buffer.append("<input type=\"button\" onClick=\"");
    buffer.append(action);
    buffer.append("\" value=\"");
    buffer.append(label);
    buffer.append("\"");
    if (!isEnabled) {
      buffer.append(" disabled");
    }
    buffer.append(">&nbsp;");
  }

  public static final void addBasementSpoilers(final StringBuffer buffer) {
    buffer.insert(
        buffer.indexOf("</head>"),
        "<script language=\"Javascript\" src=\"/"
            + KoLConstants.BASEMENT_JS
            + "\"></script></head>");

    StringBuffer changes = new StringBuffer();
    changes.append("<table id=\"basementhelper\" style=\"width:100%;\">");
    changes.append(
        "<tr><td style=\"width:90%;\"><select id=\"gear\" style=\"width: 100%;\"><option value=\"none\">- change your equipment -</option>");

    // Add outfits. Skip the "No Change" entry at index 0.

    for (SpecialOutfit outfit : EquipmentManager.getCustomOutfits()) {
      changes.append("<option value=\"outfit ");
      changes.append(outfit.getName());
      changes.append("\">outfit ");
      changes.append(outfit.getName());
      changes.append("</option>");
    }

    for (FamiliarData fam : KoLCharacter.usableFamiliars()) {
      boolean useful =
          switch (fam.getId()) {
            case FamiliarPool.HAND,
                FamiliarPool.SANDWORM,
                FamiliarPool.PARROT,
                FamiliarPool.PRESSIE,
                FamiliarPool.RIFTLET,
                FamiliarPool.GIBBERER,
                FamiliarPool.HARE -> true;
            case FamiliarPool.SOMBRERO -> !KoLCharacter.usableFamiliars()
                .contains(BasementRequest.SANDWORM);
            default -> false;
          };

      if (fam.hasDrop()) {
        useful = fam.dropsToday() < fam.dropDailyCap();
      }

      if (!useful) continue;

      changes.append("<option value=\"familiar ");
      changes.append(fam.getRace());
      changes.append("\">familiar ");
      changes.append(fam.getRace());
      changes.append("</option>");
    }

    changes.append(
        "</select></td><td>&nbsp;</td><td style=\"vertical-align:top; text-align:left;\"><input type=\"button\" value=\"exec\" onClick=\"changeBasementGear();\"></td></tr>");

    // Add effects

    List<StatBooster> listedEffects = BasementRequest.getStatBoosters();

    if (!listedEffects.isEmpty()) {
      String computeFunction =
          "computeNetBoost("
              + BasementRequest.getBasementTestCurrent()
              + ","
              + BasementRequest.getBasementTestValue()
              + ");";

      changes.append("<tr><td style=\"width:90%;\"><select onchange=\"");
      changes.append(computeFunction);
      changes.append("\" id=\"potion\" style=\"width: 100%;\" multiple size=5>");

      if (KoLCharacter.getCurrentHP() < KoLCharacter.getMaximumHP()) {
        if (KoLCharacter.hasSkill(SkillPool.COCOON)) {
          changes.append("<option value=0>cast Cannelloni Cocoon (hp restore)</option>");
        } else {
          changes.append("<option value=0>use 1 scroll of drastic healing (hp restore)</option>");
        }
      }

      if (KoLCharacter.getCurrentMP() < KoLCharacter.getMaximumMP()) {
        changes.append("<option value=0");

        if (KoLCharacter.getFullness() == KoLCharacter.getFullnessLimit()) {
          changes.append(" disabled");
        }

        changes.append(">eat 1 Jumbo Dr. Lucifer (mp restore)</option>");
      }

      for (StatBooster booster : listedEffects) {
        BasementDecorator.appendBasementEffect(changes, booster);
      }

      changes.append(
          "</select></td><td>&nbsp;</td><td style=\"vertical-align:top; text-align:left;\">");
      changes.append("<input type=\"button\" value=\"exec\" onClick=\"changeBasementEffects();\">");
      changes.append("<br/><br/><font size=-1><nobr id=\"changevalue\">");
      changes.append(BasementRequest.getBasementTestCurrent());
      changes.append("</nobr><br/><nobr id=\"changetarget\">");
      changes.append(BasementRequest.getBasementTestValue());
      changes.append("</nobr></td></tr>");
    }

    changes.append("</table>");
    buffer.insert(buffer.indexOf("</center><blockquote>"), changes.toString());

    String checkString = BasementRequest.getRequirement();
    buffer.insert(buffer.lastIndexOf("</b>") + 4, "<br/>");
    buffer.insert(buffer.lastIndexOf("<img"), "<table><tr><td>");
    buffer.insert(
        buffer.indexOf(">", buffer.lastIndexOf("<img")) + 1,
        "</td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td><td><font id=\"spoiler\" size=2>"
            + checkString
            + "</font></td></tr></table>");
  }

  private static void addBasementChoiceSpoilers(
      final StringBuffer buffer, final String choice1, final String choice2) {
    String text = buffer.toString();

    buffer.setLength(0);

    int index1 = 0, index2;

    // Add first choice spoiler
    index2 = text.indexOf("</form>", index1);
    buffer.append(text, index1, index2);
    buffer.append("<br><font size=-1>(" + choice1 + ")</font><br/></form>");
    index1 = index2 + 7;

    // Add second choice spoiler
    index2 = text.indexOf("</form>", index1);
    buffer.append(text, index1, index2);
    buffer.append("<br><font size=-1>(" + choice2 + ")</font><br/></form>");
    index1 = index2 + 7;

    // Append remainder of buffer
    buffer.append(text.substring(index1));
  }

  private static void appendBasementEffect(final StringBuffer changes, final StatBooster effect) {
    changes.append("<option value=");
    changes.append(effect.getEffectiveBoost());

    if (effect.disabled()) {
      changes.append(" disabled");
    }

    changes.append(">");

    if (!effect.itemAvailable()) {
      if (Preferences.getInteger("basementMallPrices") > 0) {
        changes.append("acquire (~");
        changes.append(
            KoLConstants.COMMA_FORMAT.format(effect.getItemPrice() * effect.getItem().getCount()));
        changes.append(" meat) &amp; ");
      } else {
        changes.append("acquire &amp; ");
      }
    } else if (effect.getItem() != null && Preferences.getInteger("basementMallPrices") > 1) {
      changes.append("(~");
      changes.append(
          KoLConstants.COMMA_FORMAT.format(effect.getItemPrice() * effect.getItem().getCount()));
      changes.append(" meat) ");
    }

    changes.append(effect.getAction());
    changes.append(" (");

    String effectName = effect.getName();

    if (effect.getComputedBoost() == 0.0) {
      if (effectName.equals(EffectDatabase.getEffectName(EffectPool.ASTRAL_SHELL))) {
        changes.append("damage absorption/element resist");
      } else if (effect.isStatEqualizer()) {
        changes.append("stat equalizer");
      } else if (effect.isDamageAbsorption()) {
        changes.append("damage absorption");
      } else if (effect.isElementalImmunity()) {
        changes.append("element immunity");
      } else {
        changes.append("element resist");
      }
    } else {
      changes.append("+");
      changes.append(KoLConstants.COMMA_FORMAT.format(effect.getEffectiveBoost()));
    }

    changes.append(")</option>");
  }

  public static class StatBooster implements Comparable<StatBooster> {
    private final String name, action;
    private final int computedBoost;
    private final int effectiveBoost;
    private AdventureResult item;
    private boolean itemAvailable;
    private int fullness;
    private int spleen;
    private int inebriety;
    private final boolean isDamageAbsorption;
    private final boolean isElementalImmunity;
    private final boolean isStatEqualizer;

    private static boolean moxieControlsMP = false;

    private static boolean absOfTin = false;
    private static boolean gnomishHardigness = false;
    private static boolean gnomishUgnderstanding = false;
    private static boolean marginallyInsane = false;
    private static boolean spiritOfRavioli = false;
    private static boolean wisdomOfTheElderTortoise = false;

    private static final AdventureResult MOXIE_MAGNET = ItemPool.get(ItemPool.MOXIE_MAGNET, 1);
    private static final AdventureResult TRAVOLTAN_TROUSERS =
        ItemPool.get(ItemPool.TRAVOLTAN_TROUSERS, 1);

    public StatBooster(final String name) {
      this.name = name;

      this.computedBoost = this.computeBoost();
      this.effectiveBoost = this.computedBoost > 0.0 ? this.computedBoost : 0 - this.computedBoost;

      this.action =
          this.computedBoost < 0
              ? "uneffect " + name
              : MoodManager.getDefaultAction("lose_effect", name);

      this.item = null;
      this.itemAvailable = true;
      this.fullness = 0;
      this.spleen = 0;
      this.inebriety = 0;
      this.isDamageAbsorption =
          this.name.equals(EffectDatabase.getEffectName(EffectPool.ASTRAL_SHELL))
              || this.name.equals(EffectDatabase.getEffectName(EffectPool.GHOSTLY_SHELL));
      this.isElementalImmunity = BasementRequest.isElementalImmunity(this.name);
      this.isStatEqualizer =
          this.name.equals(EffectDatabase.getEffectName(EffectPool.EXPERT_OILINESS))
              || this.name.equals(EffectDatabase.getEffectName(EffectPool.SLIPPERY_OILINESS))
              || this.name.equals(EffectDatabase.getEffectName(EffectPool.STABILIZING_OILINESS));

      if (this.action.startsWith("use")
          || this.action.startsWith("chew")
          || this.action.startsWith("drink")
          || this.action.startsWith("eat")) {
        int index = this.action.indexOf(" ") + 1;
        this.item = ItemFinder.getFirstMatchingItem(this.action.substring(index).trim(), false);
        if (this.item != null) {
          this.itemAvailable = InventoryManager.hasItem(this.item);
          this.fullness = ConsumablesDatabase.getFullness(item.getName());
          this.spleen = ConsumablesDatabase.getSpleenHit(item.getName());
          this.inebriety = ConsumablesDatabase.getInebriety(item.getName());
        }
      }
    }

    public static final boolean moxieControlsMP() {
      // With Moxie Magnet, uses Moxie, not Mysticality
      if (KoLCharacter.hasEquipped(MOXIE_MAGNET)) return true;

      // Ditto if Travoltan trousers and Mox > Mys
      if (KoLCharacter.hasEquipped(TRAVOLTAN_TROUSERS))
        return KoLCharacter.getAdjustedMoxie() > KoLCharacter.getAdjustedMysticality();

      return false;
    }

    public final boolean isDamageAbsorption() {
      return this.isDamageAbsorption;
    }

    public final boolean isElementalImmunity() {
      return this.isElementalImmunity;
    }

    public final boolean isStatEqualizer() {
      return this.isStatEqualizer;
    }

    public static void checkSkills() {
      StatBooster.moxieControlsMP = moxieControlsMP();

      StatBooster.absOfTin = KoLCharacter.hasSkill(SkillPool.ABS_OF_TIN);
      StatBooster.gnomishHardigness = KoLCharacter.hasSkill(SkillPool.GNOMISH_HARDINESS);
      StatBooster.gnomishUgnderstanding = KoLCharacter.hasSkill(SkillPool.COSMIC_UNDERSTANDING);
      StatBooster.marginallyInsane = KoLCharacter.hasSkill(SkillPool.MARGINALLY_INSANE);
      StatBooster.spiritOfRavioli = KoLCharacter.hasSkill(SkillPool.SPIRIT_OF_RAVIOLI);
      StatBooster.wisdomOfTheElderTortoise =
          KoLCharacter.hasSkill(SkillPool.WISDOM_OF_THE_ELDER_TORTOISES);
    }

    @Override
    public boolean equals(final Object o) {
      return o instanceof StatBooster sb && this.name.equals(sb.name);
    }

    @Override
    public int hashCode() {
      return this.name != null ? this.name.hashCode() : 0;
    }

    @Override
    public int compareTo(final StatBooster o) {
      if (this.effectiveBoost == 0.0) {
        if (o.effectiveBoost != 0.0) {
          return -1;
        }
        if (this.isElementalImmunity) {
          return -1;
        }
        if (o.isElementalImmunity) {
          return 1;
        }
        return this.name.compareToIgnoreCase(o.name);
      }

      if (o.effectiveBoost == 0.0) {
        return 1;
      }

      if (this.effectiveBoost != o.effectiveBoost) {
        return this.effectiveBoost > o.effectiveBoost ? -1 : 1;
      }

      return this.name.compareToIgnoreCase(o.name);
    }

    public String getName() {
      return name;
    }

    public AdventureResult getItem() {
      return item;
    }

    public int getItemPrice() {
      if (this.item == null) return 0;
      int itemId = this.item.getItemId();
      if (MallPriceDatabase.getAge(itemId) > 7.0) {
        MallPriceManager.getMallPrice(itemId);
      }
      return MallPriceDatabase.getPrice(itemId);
    }

    public boolean itemAvailable() {
      return itemAvailable;
    }

    public int getFullness() {
      return spleen;
    }

    public int getSpleen() {
      return spleen;
    }

    public int getInebriety() {
      return inebriety;
    }

    public boolean disabled() {
      if (item == null) {
        if (this.action.startsWith("concert ") && Preferences.getBoolean("concertVisited")) {
          return true;
        }

        if (this.action.startsWith("telescope ") && Preferences.getBoolean("telescopeLookedHigh")) {
          return true;
        }

        return false;
      }

      if (this.fullness > 0
          && (KoLCharacter.getFullness() + this.fullness) > KoLCharacter.getFullnessLimit()) {
        return true;
      }

      if (this.spleen > 0
          && (KoLCharacter.getSpleenUse() + this.spleen) > KoLCharacter.getSpleenLimit()) {
        return true;
      }

      if (this.inebriety > 0
          && (KoLCharacter.getInebriety() + this.inebriety) > KoLCharacter.getInebrietyLimit()) {
        return true;
      }

      return false;
    }

    public int getComputedBoost() {
      return computedBoost;
    }

    public int getEffectiveBoost() {
      return effectiveBoost;
    }

    public String getAction() {
      return action;
    }

    public int computeBoost() {
      Modifiers m = ModifierDatabase.getModifiers(ModifierType.EFFECT, this.name);
      if (m == null) {
        return 0;
      }

      if (BasementRequest.getActualStatNeeded() == DoubleModifier.HP) {
        return StatBooster.boostMaxHP(m);
      }

      if (BasementRequest.getActualStatNeeded() == DoubleModifier.MP) {
        return StatBooster.boostMaxMP(m);
      }

      double base = StatBooster.getEqualizedStat(BasementRequest.getPrimaryBoost());
      double boost =
          m.get(BasementRequest.getSecondaryBoost())
              + m.get(BasementRequest.getPrimaryBoost()) * base / 100.0;

      return (int) Math.ceil(boost);
    }

    public static double getEqualizedStat(final DoubleModifier mod) {
      double currentStat = 0.0;

      switch (mod) {
        case MUS_PCT:
          currentStat = KoLCharacter.getBaseMuscle();
          break;
        case MYS_PCT:
          currentStat = KoLCharacter.getBaseMysticality();
          break;
        case MOX_PCT:
          currentStat = KoLCharacter.getBaseMoxie();
          break;
        default:
          return 0.0;
      }

      if (KoLConstants.activeEffects.contains(BasementRequest.MUS_EQUAL)) {
        currentStat = Math.max(KoLCharacter.getBaseMuscle(), currentStat);
      }

      if (KoLConstants.activeEffects.contains(BasementRequest.MYS_EQUAL)) {
        currentStat = Math.max(KoLCharacter.getBaseMysticality(), currentStat);
      }

      if (KoLConstants.activeEffects.contains(BasementRequest.MOX_EQUAL)) {
        currentStat = Math.max(KoLCharacter.getBaseMoxie(), currentStat);
      }

      return currentStat;
    }

    public static int boostMaxHP(final Modifiers m) {
      double addedMuscleFixed = m.get(DoubleModifier.MUS);
      double addedMusclePercent = m.get(DoubleModifier.MUS_PCT);
      int addedHealthFixed = (int) m.get(DoubleModifier.HP);

      if (addedMuscleFixed == 0.0 && addedMusclePercent == 0.0 && addedHealthFixed == 0) {
        return 0;
      }

      double muscleBase = StatBooster.getEqualizedStat(DoubleModifier.MUS_PCT);
      double muscleBonus = addedMuscleFixed + Math.floor(addedMusclePercent * muscleBase / 100.0);
      double muscleMultiplicator = 1.0;

      if (KoLCharacter.isMuscleClass()) {
        muscleMultiplicator += 0.5;
      }

      if (StatBooster.absOfTin) {
        muscleMultiplicator += 0.10;
      }

      if (StatBooster.gnomishHardigness) {
        muscleMultiplicator += 0.05;
      }

      if (StatBooster.spiritOfRavioli) {
        muscleMultiplicator += 0.25;
      }

      return (int) Math.ceil(muscleBonus * muscleMultiplicator) + addedHealthFixed;
    }

    public static int boostMaxMP(final Modifiers m) {
      DoubleModifier statModifier;
      DoubleModifier statPercentModifier;

      if (StatBooster.moxieControlsMP) {
        statModifier = DoubleModifier.MOX;
        statPercentModifier = DoubleModifier.MOX_PCT;
      } else {
        statModifier = DoubleModifier.MYS;
        statPercentModifier = DoubleModifier.MYS_PCT;
      }

      double addedStatFixed = m.get(statModifier);
      double addedStatPercent = m.get(statPercentModifier);
      int addedManaFixed = (int) m.get(DoubleModifier.MP);

      if (addedStatFixed == 0.0 && addedStatPercent == 0.0 && addedManaFixed == 0.0) {
        return 0;
      }

      double statBase = StatBooster.getEqualizedStat(statPercentModifier);
      double manaBonus = addedStatFixed + addedStatPercent * statBase / 100.0;
      double manaMultiplicator = 1.0;

      if (KoLCharacter.isMysticalityClass()) {
        manaMultiplicator += 0.5;
      }

      if (StatBooster.gnomishUgnderstanding) {
        manaMultiplicator += 0.05;
      }

      if (StatBooster.marginallyInsane) {
        manaMultiplicator += 0.1;
      }

      if (StatBooster.wisdomOfTheElderTortoise) {
        manaMultiplicator += 0.5;
      }

      return (int) Math.ceil(manaBonus * manaMultiplicator) + addedManaFixed;
    }
  }
}
