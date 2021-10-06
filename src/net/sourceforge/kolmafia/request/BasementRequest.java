package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.BasementDecorator.StatBooster;

public class BasementRequest extends AdventureRequest {
  private enum TestType {
    NONE,
    MONSTER,
    REWARD,
    ELEMENT,
    MUSCLE,
    MYSTICALITY,
    MOXIE,
    MPDRAIN,
    HPDRAIN,
    ;
  }

  private static int basementLevel = 0;

  private static TestType basementTest = TestType.NONE;
  private static String basementTestString = "";

  private static double basementTestValue = 0;
  private static double basementTestCurrent = 0;

  public static String basementMonster = "";
  private static String gauntletString = "";

  private static int actualStatNeeded = 0;
  private static int primaryBoost = 0;
  private static int secondaryBoost = 0;

  private static double averageResistanceNeeded = 0.0;
  private static Element element1 = Element.NONE, element2 = Element.NONE;
  private static int vulnerability = 0;
  private static Element goodelement = Element.NONE;
  private static AdventureResult goodphial = null;
  private static AdventureResult goodeffect = null;
  private static Element badelement1 = Element.NONE,
      badelement2 = Element.NONE,
      badelement3 = Element.NONE;
  private static AdventureResult badeffect1 = null, badeffect2 = null, badeffect3 = null;

  private static final List<AdventureResult> desirableEffects = new ArrayList<>();

  private static int level1, level2;
  private static double resistance1, resistance2;
  private static double expected1, expected2;

  private static String lastResponseText = "";
  private static String basementErrorMessage = null;

  public static final AdventureResult MUS_EQUAL = EffectPool.get(EffectPool.STABILIZING_OILINESS);
  public static final AdventureResult MYS_EQUAL = EffectPool.get(EffectPool.EXPERT_OILINESS);
  public static final AdventureResult MOX_EQUAL = EffectPool.get(EffectPool.SLIPPERY_OILINESS);

  private static final AdventureResult BLACK_PAINT = EffectPool.get(EffectPool.RED_DOOR_SYNDROME);

  private static final AdventureResult HOT_PHIAL = ItemPool.get(ItemPool.PHIAL_OF_HOTNESS, 1);
  private static final AdventureResult COLD_PHIAL = ItemPool.get(ItemPool.PHIAL_OF_COLDNESS, 1);
  private static final AdventureResult SPOOKY_PHIAL = ItemPool.get(ItemPool.PHIAL_OF_SPOOKINESS, 1);
  private static final AdventureResult STENCH_PHIAL = ItemPool.get(ItemPool.PHIAL_OF_STENCH, 1);
  private static final AdventureResult SLEAZE_PHIAL = ItemPool.get(ItemPool.PHIAL_OF_SLEAZINESS, 1);

  public static final AdventureResult MAX_HOT = EffectPool.get(EffectPool.FIREPROOF_LIPS);
  public static final AdventureResult MAX_COLD = EffectPool.get(EffectPool.FEVER_FROM_THE_FLAVOR);
  public static final AdventureResult MAX_SPOOKY = EffectPool.get(EffectPool.HYPHEMARIFFIC);
  public static final AdventureResult MAX_STENCH = EffectPool.get(EffectPool.CANT_SMELL_NOTHING);
  public static final AdventureResult MAX_SLEAZE = EffectPool.get(EffectPool.HYPEROFFENDED);

  private static final AdventureResult HOT_FORM = EffectPool.get(EffectPool.HOTFORM);
  private static final AdventureResult COLD_FORM = EffectPool.get(EffectPool.COLDFORM);
  private static final AdventureResult SPOOKY_FORM = EffectPool.get(EffectPool.SPOOKYFORM);
  private static final AdventureResult STENCH_FORM = EffectPool.get(EffectPool.STENCHFORM);
  private static final AdventureResult SLEAZE_FORM = EffectPool.get(EffectPool.SLEAZEFORM);

  private static final Pattern BASEMENT_PATTERN = Pattern.compile("Level ([\\d,]+)");

  public static final AdventureResult[] ELEMENT_PHIALS =
      new AdventureResult[] {
        BasementRequest.HOT_PHIAL,
        BasementRequest.COLD_PHIAL,
        BasementRequest.SPOOKY_PHIAL,
        BasementRequest.STENCH_PHIAL,
        BasementRequest.SLEAZE_PHIAL
      };

  public static final AdventureResult[] ELEMENT_FORMS =
      new AdventureResult[] {
        BasementRequest.HOT_FORM,
        BasementRequest.COLD_FORM,
        BasementRequest.SPOOKY_FORM,
        BasementRequest.STENCH_FORM,
        BasementRequest.SLEAZE_FORM
      };

  public static final boolean isElementalImmunity(final String name) {
    for (int j = 0; j < BasementRequest.ELEMENT_FORMS.length; ++j) {
      if (name.equals(BasementRequest.ELEMENT_FORMS[j].getName())) {
        return true;
      }
    }

    return false;
  }

  public static final FamiliarData SANDWORM = new FamiliarData(FamiliarPool.SANDWORM);

  /**
   * Constructs a new <code>/code> which executes an
   * adventure in Fernswarthy's Basement by posting to the provided form,
   * notifying the givenof results (or errors).
   *
   * @param	adventureName	The name of the adventure location
   */
  public BasementRequest(final String adventureName) {
    super(adventureName, "basement.php", "0");
  }

  @Override
  public void run() {
    // Clear the data flags and probe the basement to see what we have.

    this.data.clear();
    super.run();

    // Load up the data variables and switch outfits if it's a fight.
    BasementRequest.checkBasement();

    // If we know we can't pass the test, give an error and bail out now.

    if (BasementRequest.basementErrorMessage != null) {
      KoLmafia.updateDisplay(MafiaState.ERROR, BasementRequest.basementErrorMessage);
      return;
    }

    // Decide which action to set. If it's a stat reward, always
    // boost prime stat.
    this.addFormField("action", BasementRequest.getBasementAction(this.responseText));

    // Attempt to pass the test.
    int lastBasementLevel = BasementRequest.basementLevel;

    super.run();

    // Handle redirection

    if (this.responseCode != 200) {
      // If it was a fight and we won, good.

      if (FightRequest.INSTANCE.responseCode == 200
          && FightRequest.lastResponseText.contains("<!--WINWINWIN-->")) {
        return;
      }

      // Otherwise ... what is this? Refetch the page and see if we passed test.

      this.data.clear();
      super.run();
    }

    // See what basement level we are on now and fail if we've not advanced.

    if (BasementRequest.basementLevel == lastBasementLevel) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to pass basement test.");
    }
  }

  @Override
  public void processResults() {
    BasementRequest.checkBasement(false, this.responseText);
  }

  public static final String getBasementAction(final String text) {
    if (text.contains("Got Silk?")) {
      return KoLCharacter.isMoxieClass() ? "1" : "2";
    }
    if (text.contains("Save the Dolls")) {
      return KoLCharacter.isMysticalityClass() ? "1" : "2";
    }
    if (text.contains("Take the Red Pill")) {
      return KoLCharacter.isMuscleClass() ? "1" : "2";
    }
    return "1";
  }

  public static final int getBasementLevel() {
    return BasementRequest.basementLevel;
  }

  public static final String getBasementLevelName() {
    return "Fernswarthy's Basement (Level " + BasementRequest.basementLevel + ")";
  }

  public static final String getBasementLevelSummary() {
    switch (BasementRequest.basementTest) {
      case NONE:
      case MONSTER:
        return "";
      case REWARD:
        return BasementRequest.basementTestString;
      case ELEMENT:
        {
          BasementRequest.updateElementalResistanceParameters();
          StringBuilder buffer = new StringBuilder(BasementRequest.basementTestString);
          buffer.append(" Test: +");
          buffer.append(BasementRequest.level1);
          buffer.append(" ");
          buffer.append(BasementRequest.element1.toString());
          buffer.append(" ");
          buffer.append(KoLConstants.COMMA_FORMAT.format(BasementRequest.resistance1));
          buffer.append("%");
          if (BasementRequest.vulnerability == 1) {
            buffer.append(" (vulnerable) ");
          }
          buffer.append(" (");
          buffer.append(KoLConstants.COMMA_FORMAT.format(BasementRequest.expected1));
          buffer.append(" hp), +");
          buffer.append(BasementRequest.level2);
          buffer.append(" ");
          buffer.append(BasementRequest.element2.toString());
          buffer.append(" ");
          buffer.append(KoLConstants.COMMA_FORMAT.format(BasementRequest.resistance2));
          buffer.append("%");
          if (BasementRequest.vulnerability == 2) {
            buffer.append(" (vulnerable) ");
          }
          buffer.append(" (");
          buffer.append(KoLConstants.COMMA_FORMAT.format(BasementRequest.expected2));
          buffer.append(" hp)");
          return buffer.toString();
        }
      case HPDRAIN:
        {
          BasementRequest.updateHPDrainParameters();
          return BasementRequest.basementTestString
              + " Test: "
              + KoLConstants.COMMA_FORMAT.format(BasementRequest.basementTestCurrent)
              + " current, "
              + BasementRequest.gauntletString
              + " needed";
        }

      case MUSCLE:
        BasementRequest.updateMuscleParameters();
        break;
      case MYSTICALITY:
        BasementRequest.updateMysticalityParameters();
        break;
      case MOXIE:
        BasementRequest.updateMoxieParameters();
        break;
      case MPDRAIN:
        BasementRequest.updateMPDrainParameters();
        break;
    }

    // Stat Test
    return BasementRequest.basementTestString
        + " Test: "
        + KoLConstants.COMMA_FORMAT.format(BasementRequest.basementTestCurrent)
        + " current, "
        + KoLConstants.COMMA_FORMAT.format(BasementRequest.basementTestValue)
        + " needed";
  }

  public static final String getRequirement() {
    if (BasementRequest.basementTestString.equals("Elemental Resist")) {
      return "<u>"
          + BasementRequest.basementTestString
          + "</u><br/>Current: +"
          + BasementRequest.level1
          + " "
          + BasementRequest.element1.toString()
          + " "
          + KoLConstants.COMMA_FORMAT.format(BasementRequest.resistance1)
          + "%"
          + (BasementRequest.vulnerability == 1 ? " (vulnerable) " : "")
          + " ("
          + KoLConstants.COMMA_FORMAT.format(BasementRequest.expected1)
          + " hp), +"
          + BasementRequest.level2
          + " "
          + BasementRequest.element2.toString()
          + " "
          + KoLConstants.COMMA_FORMAT.format(BasementRequest.resistance2)
          + "%"
          + (BasementRequest.vulnerability == 2 ? " (vulnerable) " : "")
          + " ("
          + KoLConstants.COMMA_FORMAT.format(BasementRequest.expected2)
          + " hp)</br>"
          + "Needed: "
          + KoLConstants.COMMA_FORMAT.format(BasementRequest.averageResistanceNeeded)
          + "% average resistance or "
          + BasementRequest.goodeffect.getName();
    }

    if (BasementRequest.basementTestString.startsWith("Monster")) {
      int index = BasementRequest.basementTestString.indexOf(": ");
      if (index == -1) {
        return "";
      }

      return "<u>Monster</u><br/>" + BasementRequest.basementTestString.substring(index + 2);
    }

    if (BasementRequest.basementTestString.equals("Maximum HP")) {
      return "<u>"
          + BasementRequest.basementTestString
          + "</u><br/>"
          + "Current: "
          + KoLConstants.COMMA_FORMAT.format(BasementRequest.basementTestCurrent)
          + "<br/>"
          + "Needed: "
          + BasementRequest.gauntletString;
    }

    return "<u>"
        + BasementRequest.basementTestString
        + "</u><br/>"
        + "Current: "
        + KoLConstants.COMMA_FORMAT.format(BasementRequest.basementTestCurrent)
        + "<br/>"
        + "Needed: "
        + KoLConstants.COMMA_FORMAT.format(BasementRequest.basementTestValue);
  }

  private static void changeBasementOutfit(final String name) {
    // Find desired outfit. Skip "No Change" entry at index 0.
    List<SpecialOutfit> available = EquipmentManager.getCustomOutfits();
    int count = available.size();
    for (int i = 1; i < count; ++i) {
      SpecialOutfit currentTest = available.get(i);
      String currentTestString = currentTest.toString().toLowerCase();

      if (currentTestString.contains(name)) {
        RequestThread.postRequest(new EquipmentRequest(currentTest));
        // Restoring to the original outfit after Basement auto-adventuring is
        // slow and pointless - you're going to want something related to the
        // current outfit to continue, not the original one.
        SpecialOutfit.forgetCheckpoints();
        return;
      }
    }
  }

  private static boolean checkForElementalTest(boolean autoSwitch, final String responseText) {
    if (responseText.contains("<b>Peace, Bra!</b>")) {
      BasementRequest.element1 = Element.STENCH;
      BasementRequest.element2 = Element.SLEAZE;

      BasementRequest.goodelement = BasementRequest.element2;
      BasementRequest.goodphial = BasementRequest.SLEAZE_PHIAL;
      BasementRequest.goodeffect = BasementRequest.SLEAZE_FORM;

      // Stench is vulnerable to Sleaze
      BasementRequest.badelement1 = Element.STENCH;
      BasementRequest.badeffect1 = BasementRequest.STENCH_FORM;

      // Spooky is vulnerable to Stench
      BasementRequest.badelement2 = Element.SPOOKY;
      BasementRequest.badeffect2 = BasementRequest.SPOOKY_FORM;

      // Hot is vulnerable to Sleaze and Stench
      BasementRequest.badelement3 = Element.HOT;
      BasementRequest.badeffect3 = BasementRequest.HOT_FORM;
    } else if (responseText.contains("<b>Singled Out</b>")) {
      BasementRequest.element1 = Element.COLD;
      BasementRequest.element2 = Element.SLEAZE;

      BasementRequest.goodelement = BasementRequest.element1;
      BasementRequest.goodphial = BasementRequest.COLD_PHIAL;
      BasementRequest.goodeffect = BasementRequest.COLD_FORM;

      // Sleaze is vulnerable to Cold
      BasementRequest.badelement1 = Element.SLEAZE;
      BasementRequest.badeffect1 = BasementRequest.SLEAZE_FORM;

      // Stench is vulnerable to Cold
      BasementRequest.badelement2 = Element.STENCH;
      BasementRequest.badeffect2 = BasementRequest.STENCH_FORM;

      // Hot is vulnerable to Sleaze
      BasementRequest.badelement3 = Element.HOT;
      BasementRequest.badeffect3 = BasementRequest.HOT_FORM;
    } else if (responseText.contains("<b>Still Better than Pistachio</b>")) {
      BasementRequest.element1 = Element.STENCH;
      BasementRequest.element2 = Element.HOT;

      BasementRequest.goodelement = BasementRequest.element1;
      BasementRequest.goodphial = BasementRequest.STENCH_PHIAL;
      BasementRequest.goodeffect = BasementRequest.STENCH_FORM;

      // Cold is vulnerable to Hot
      BasementRequest.badelement1 = Element.COLD;
      BasementRequest.badeffect1 = BasementRequest.COLD_FORM;

      // Spooky is vulnerable to Hot
      BasementRequest.badelement2 = Element.SPOOKY;
      BasementRequest.badeffect2 = BasementRequest.SPOOKY_FORM;

      // Hot is vulnerable to Stench
      BasementRequest.badelement3 = Element.HOT;
      BasementRequest.badeffect3 = BasementRequest.HOT_FORM;
    } else if (responseText.contains("<b>Unholy Writes</b>")) {
      BasementRequest.element1 = Element.HOT;
      BasementRequest.element2 = Element.SPOOKY;

      BasementRequest.goodelement = BasementRequest.element1;
      BasementRequest.goodphial = BasementRequest.HOT_PHIAL;
      BasementRequest.goodeffect = BasementRequest.HOT_FORM;

      // Cold is vulnerable to Spooky
      BasementRequest.badelement1 = Element.COLD;
      BasementRequest.badeffect1 = BasementRequest.COLD_FORM;

      // Spooky is vulnerable to Hot
      BasementRequest.badelement2 = Element.SPOOKY;
      BasementRequest.badeffect2 = BasementRequest.SPOOKY_FORM;

      // Sleaze is vulnerable to Spooky
      BasementRequest.badelement3 = Element.SLEAZE;
      BasementRequest.badeffect3 = BasementRequest.SLEAZE_FORM;
    } else if (responseText.contains("<b>The Unthawed</b>")) {
      BasementRequest.element1 = Element.COLD;
      BasementRequest.element2 = Element.SPOOKY;

      BasementRequest.goodelement = BasementRequest.element2;
      BasementRequest.goodphial = BasementRequest.SPOOKY_PHIAL;
      BasementRequest.goodeffect = BasementRequest.SPOOKY_FORM;

      // Cold is vulnerable to Spooky
      BasementRequest.badelement1 = Element.COLD;
      BasementRequest.badeffect1 = BasementRequest.COLD_FORM;

      // Stench is vulnerable to Cold
      BasementRequest.badelement2 = Element.STENCH;
      BasementRequest.badeffect2 = BasementRequest.STENCH_FORM;

      // Sleaze is vulnerable to Cold
      BasementRequest.badelement3 = Element.SLEAZE;
      BasementRequest.badeffect3 = BasementRequest.SLEAZE_FORM;
    } else {
      // Not a known elemental test
      return false;
    }

    BasementRequest.actualStatNeeded = Modifiers.HP;
    BasementRequest.primaryBoost = Modifiers.MUS_PCT;
    BasementRequest.secondaryBoost = Modifiers.MUS;

    // Add the only beneficial elemental form for this test

    boolean hasGoodEffect = KoLConstants.activeEffects.contains(BasementRequest.goodeffect);

    if (!hasGoodEffect) {
      BasementRequest.desirableEffects.add(BasementRequest.goodeffect);
    }

    BasementRequest.addDesiredEqualizer();

    // Add effects that resist the specific elements being tested
    // unless we have elemental immunity to that element.

    if (BasementRequest.element1 != BasementRequest.goodelement || !hasGoodEffect) {
      BasementRequest.addDesirableEffects(
          Modifiers.getPotentialChanges(Modifiers.elementalResistance(BasementRequest.element1)));
    }

    if (BasementRequest.element2 != BasementRequest.goodelement || !hasGoodEffect) {
      BasementRequest.addDesirableEffects(
          Modifiers.getPotentialChanges(Modifiers.elementalResistance(BasementRequest.element2)));
    }

    // Add some effects that resist all elements
    if (!KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.ASTRAL_SHELL))) {
      BasementRequest.desirableEffects.add(EffectPool.get(EffectPool.ASTRAL_SHELL));
    }

    if (!KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.ELEMENTAL_SPHERE))) {
      BasementRequest.desirableEffects.add(EffectPool.get(EffectPool.ELEMENTAL_SPHERE));
    }

    if (!KoLConstants.activeEffects.contains(BasementRequest.BLACK_PAINT)) {
      BasementRequest.desirableEffects.add(BasementRequest.BLACK_PAINT);
    }

    if (BasementRequest.canHandleElementTest(autoSwitch, false)) {
      return true;
    }

    if (!autoSwitch) {
      return true;
    }

    BasementRequest.changeBasementOutfit("element");
    BasementRequest.canHandleElementTest(true, true);
    return true;
  }

  public static float getSafetyMargin() {
    return Preferences.getFloat("basementSafetyMargin");
  }

  private static void updateElementalResistanceParameters() {
    BasementRequest.basementTestString = "Elemental Resist";

    // According to http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
    // total elemental damage is roughly 4.48 * x^1.4.  Assume the worst-case.

    double damage1 =
        (Math.pow(BasementRequest.basementLevel, 1.4) * 4.48 + 8.0) * getSafetyMargin();
    double damage2 = damage1;

    BasementRequest.level1 = KoLCharacter.getElementalResistanceLevels(BasementRequest.element1);
    BasementRequest.resistance1 = KoLCharacter.elementalResistanceByLevel(BasementRequest.level1);
    BasementRequest.level2 = KoLCharacter.getElementalResistanceLevels(BasementRequest.element2);
    BasementRequest.resistance2 = KoLCharacter.elementalResistanceByLevel(BasementRequest.level2);

    if (KoLConstants.activeEffects.contains(BasementRequest.goodeffect)) {
      if (BasementRequest.element1 == BasementRequest.goodelement) {
        BasementRequest.resistance1 = 100.0;
      } else {
        BasementRequest.resistance2 = 100.0;
      }
    }

    BasementRequest.vulnerability = 0;

    // If you have an elemental form which gives you vulnerability
    // to an element, you retain your elemental resistance (as
    // shown on the Character Sheet), but damage taken seems to be
    // quadrupled.
    if (KoLConstants.activeEffects.contains(BasementRequest.badeffect1)
        || KoLConstants.activeEffects.contains(BasementRequest.badeffect2)
        || KoLConstants.activeEffects.contains(BasementRequest.badeffect3)) {
      if (BasementRequest.element1 == BasementRequest.badelement1
          || BasementRequest.element1 == BasementRequest.badelement2
          || BasementRequest.element1 == BasementRequest.badelement3) {
        BasementRequest.vulnerability = 1;
        damage1 *= 4;
      } else {
        BasementRequest.vulnerability = 2;
        damage2 *= 4;
      }
    }

    BasementRequest.expected1 =
        Math.max(1.0, damage1 * (100.0 - BasementRequest.resistance1) / 100.0);
    BasementRequest.expected2 =
        Math.max(1.0, damage2 * (100.0 - BasementRequest.resistance2) / 100.0);

    BasementRequest.averageResistanceNeeded =
        Math.max(0.0, Math.ceil(100.0 * (1.0 - KoLCharacter.getMaximumHP() / (damage1 + damage2))));

    BasementRequest.basementTestValue = BasementRequest.expected1 + BasementRequest.expected2;
    BasementRequest.basementTestCurrent = KoLCharacter.getMaximumHP();
  }

  private static boolean canHandleElementTest(boolean autoSwitch, boolean switchedOutfits) {
    BasementRequest.updateElementalResistanceParameters();

    // If you can survive the current elemental test even without a phial,
    // then don't bother with any extra buffing.

    if (BasementRequest.expected1 + BasementRequest.expected2 < KoLCharacter.getCurrentHP()) {
      return true;
    }

    if (BasementRequest.expected1 + BasementRequest.expected2 < KoLCharacter.getMaximumHP()) {
      if (autoSwitch) {
        RecoveryManager.recoverHP((long) (BasementRequest.expected1 + BasementRequest.expected2));
      }

      return KoLmafia.permitsContinue();
    }

    // If you already have the right phial effect, check to see if
    // it's sufficient.

    if (KoLConstants.activeEffects.contains(BasementRequest.goodeffect)) {
      return false;
    }

    // If you haven't switched outfits yet, it's possible that a simple
    // outfit switch will be sufficient to buff up.

    if (!switchedOutfits) {
      return false;
    }

    // If you can't survive the test, even after an outfit switch, then
    // automatically fail.

    if (BasementRequest.expected1 >= BasementRequest.expected2) {
      if (1.0 + BasementRequest.expected2 >= KoLCharacter.getMaximumHP()) {
        BasementRequest.basementErrorMessage =
            "You must have at least "
                + BasementRequest.basementTestValue
                + "% elemental resistance.";
        return false;
      }
    } else if (1.0 + BasementRequest.expected1 >= KoLCharacter.getMaximumHP()) {
      BasementRequest.basementErrorMessage =
          "You must have at least " + BasementRequest.basementTestValue + "% elemental resistance.";
      return false;
    }

    if (!autoSwitch) {
      BasementRequest.basementErrorMessage =
          "You must have at least " + BasementRequest.basementTestValue + "% elemental resistance.";
      return false;
    }

    // You can survive, but you need an elemental phial in order to
    // do so.  Go ahead and use one, which will automatically
    // uneffect any competing phials, first

    RequestThread.postRequest(UseItemRequest.getInstance(BasementRequest.goodphial));

    double damage = Math.min(BasementRequest.expected1, BasementRequest.expected2);
    RecoveryManager.recoverHP((long) (1.0 + damage));

    return KoLmafia.permitsContinue();
  }

  private static AdventureResult getDesiredEqualizer() {
    if (KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMysticality()
        && KoLCharacter.getBaseMuscle() >= KoLCharacter.getBaseMoxie()) {
      return BasementRequest.MUS_EQUAL;
    }

    if (KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMuscle()
        && KoLCharacter.getBaseMysticality() >= KoLCharacter.getBaseMoxie()) {
      return BasementRequest.MYS_EQUAL;
    }

    return BasementRequest.MOX_EQUAL;
  }

  private static void addDesiredEqualizer() {
    AdventureResult equalizer = BasementRequest.getDesiredEqualizer();
    if (!KoLConstants.activeEffects.contains(equalizer)) {
      BasementRequest.desirableEffects.add(equalizer);
    }
  }

  private static double updateMuscleParameters() {
    // According to http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
    // stat requirement is x^1.4 + 2.  Assume the worst-case.

    double statRequirement =
        (Math.pow(BasementRequest.basementLevel, 1.4) + 2.0) * getSafetyMargin();

    BasementRequest.basementTestString = "Buffed Muscle";
    BasementRequest.basementTestCurrent = KoLCharacter.getAdjustedMuscle();
    BasementRequest.basementTestValue = (long) statRequirement;

    BasementRequest.actualStatNeeded = Modifiers.MUS;
    BasementRequest.primaryBoost = Modifiers.MUS_PCT;
    BasementRequest.secondaryBoost = Modifiers.MUS;

    return statRequirement;
  }

  private static double updateMysticalityParameters() {
    // According to http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
    // stat requirement is x^1.4 + 2.  Assume the worst-case.

    double statRequirement =
        (Math.pow(BasementRequest.basementLevel, 1.4) + 2.0) * getSafetyMargin();

    BasementRequest.basementTestString = "Buffed Mysticality";
    BasementRequest.basementTestCurrent = KoLCharacter.getAdjustedMysticality();
    BasementRequest.basementTestValue = (long) statRequirement;

    BasementRequest.actualStatNeeded = Modifiers.MYS;
    BasementRequest.primaryBoost = Modifiers.MYS_PCT;
    BasementRequest.secondaryBoost = Modifiers.MYS;

    return statRequirement;
  }

  private static double updateMoxieParameters() {
    // According to http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
    // stat requirement is x^1.4 + 2.  Assume the worst-case.

    double statRequirement =
        (Math.pow(BasementRequest.basementLevel, 1.4) + 2.0) * getSafetyMargin();

    BasementRequest.basementTestString = "Buffed Moxie";
    BasementRequest.basementTestCurrent = KoLCharacter.getAdjustedMoxie();
    BasementRequest.basementTestValue = (long) statRequirement;

    BasementRequest.actualStatNeeded = Modifiers.MOX;
    BasementRequest.primaryBoost = Modifiers.MOX_PCT;
    BasementRequest.secondaryBoost = Modifiers.MOX;

    return statRequirement;
  }

  private static boolean checkForStatTest(final boolean autoSwitch, final String responseText) {
    if (responseText.contains("Lift 'em")
        || responseText.contains("Push it Real Good")
        || responseText.contains("Ring that Bell")) {
      double statRequirement = BasementRequest.updateMuscleParameters();
      BasementRequest.basementTest = TestType.MUSCLE;
      BasementRequest.addDesiredEqualizer();

      if (KoLCharacter.getAdjustedMuscle() < statRequirement) {
        if (autoSwitch) {
          BasementRequest.changeBasementOutfit("muscle");
          if (KoLCharacter.getAdjustedMuscle() < statRequirement) {
            KoLmafiaCLI.DEFAULT_SHELL.executeCommand("maximize", "mus " + statRequirement + " min");
          }
        }

        if (KoLCharacter.getAdjustedMuscle() < statRequirement) {
          BasementRequest.basementErrorMessage =
              "You must have at least " + BasementRequest.basementTestValue + " muscle.";
        }
      }

      return true;
    }

    if (responseText.contains("Gathering:  The Magic")
        || responseText.contains("Mop the Floor")
        || responseText.contains("'doo")) {
      double statRequirement = BasementRequest.updateMysticalityParameters();
      BasementRequest.basementTest = TestType.MYSTICALITY;
      BasementRequest.addDesiredEqualizer();

      if (KoLCharacter.getAdjustedMysticality() < statRequirement) {
        if (autoSwitch) {
          BasementRequest.changeBasementOutfit("mysticality");
          if (KoLCharacter.getAdjustedMysticality() < statRequirement) {
            KoLmafiaCLI.DEFAULT_SHELL.executeCommand("maximize", "mys " + statRequirement + " min");
          }
        }

        if (KoLCharacter.getAdjustedMysticality() < statRequirement) {
          BasementRequest.basementErrorMessage =
              "You must have at least " + BasementRequest.basementTestValue + " mysticality.";
        }
      }

      return true;
    }

    if (responseText.contains("Don't Wake the Baby")
        || responseText.contains("Grab a cue")
        || responseText.contains("Smooth Moves")) {
      double statRequirement = BasementRequest.updateMoxieParameters();
      BasementRequest.basementTest = TestType.MOXIE;
      BasementRequest.addDesiredEqualizer();

      if (KoLCharacter.getAdjustedMoxie() < statRequirement) {
        if (autoSwitch) {
          BasementRequest.changeBasementOutfit("moxie");
          if (KoLCharacter.getAdjustedMoxie() < statRequirement) {
            KoLmafiaCLI.DEFAULT_SHELL.executeCommand("maximize", "mox " + statRequirement + " min");
          }
        }

        if (KoLCharacter.getAdjustedMoxie() < statRequirement) {
          BasementRequest.basementErrorMessage =
              "You must have at least " + BasementRequest.basementTestValue + " moxie.";
        }
      }

      return true;
    }

    return false;
  }

  private static double updateMPDrainParameters() {
    // According to
    // http://forums.hardcoreoxygenation.com/viewtopic.php?t=3973,
    // drain requirement is 1.67 * x^1.4 Assume worst-case.

    double drainRequirement =
        Math.pow(BasementRequest.basementLevel, 1.4) * 1.67 * getSafetyMargin();

    BasementRequest.basementTestString = "Maximum MP";
    BasementRequest.basementTestCurrent = KoLCharacter.getMaximumMP();
    BasementRequest.basementTestValue = (long) drainRequirement;

    BasementRequest.actualStatNeeded = Modifiers.MP;
    if (StatBooster.moxieControlsMP()) {
      BasementRequest.primaryBoost = Modifiers.MOX_PCT;
      BasementRequest.secondaryBoost = Modifiers.MOX;
    } else {
      BasementRequest.primaryBoost = Modifiers.MYS_PCT;
      BasementRequest.secondaryBoost = Modifiers.MYS;
    }

    return drainRequirement;
  }

  private static double updateHPDrainParameters() {
    // According to starwed at
    // http://forums.kingdomofloathing.com/viewtopic.php?t=83342&start=201
    // drain requirement is 10.0 * x^1.4. Assume worst-case.

    double drainRequirement =
        Math.pow(BasementRequest.basementLevel, 1.4) * 10.0 * getSafetyMargin();

    BasementRequest.basementTestString = "Maximum HP";
    BasementRequest.basementTestCurrent = KoLCharacter.getMaximumHP();

    BasementRequest.actualStatNeeded = Modifiers.HP;
    BasementRequest.primaryBoost = Modifiers.MUS_PCT;
    BasementRequest.secondaryBoost = Modifiers.MUS;

    double damageAbsorb =
        1.0 - (Math.sqrt(Math.min(1000, KoLCharacter.getDamageAbsorption()) / 10.0) - 1.0) / 10.0;
    double healthRequirement = drainRequirement * damageAbsorb;

    BasementRequest.basementTestValue = (long) healthRequirement;
    BasementRequest.gauntletString =
        (long) drainRequirement
            + " * "
            + KoLConstants.FLOAT_FORMAT.format(damageAbsorb)
            + " ("
            + KoLCharacter.getDamageAbsorption()
            + " DA) = "
            + KoLConstants.COMMA_FORMAT.format(healthRequirement);

    return drainRequirement;
  }

  private static boolean checkForDrainTest(final boolean autoSwitch, final String responseText) {
    if (responseText.contains("Grab the Handles")) {
      double drainRequirement = BasementRequest.updateMPDrainParameters();
      BasementRequest.basementTest = TestType.MPDRAIN;
      BasementRequest.addDesiredEqualizer();

      if (KoLCharacter.getMaximumMP() < drainRequirement) {
        if (autoSwitch) {
          BasementRequest.changeBasementOutfit("mpdrain");
          if (KoLCharacter.getMaximumMP() < drainRequirement) {
            KoLmafiaCLI.DEFAULT_SHELL.executeCommand("maximize", "MP " + drainRequirement + " min");
          }
        }

        if (KoLCharacter.getMaximumMP() < drainRequirement) {
          BasementRequest.basementErrorMessage = "Insufficient mana to continue.";
          return true;
        }
      }

      if (autoSwitch) {
        RecoveryManager.recoverMP((long) drainRequirement);
      }

      return true;
    }

    if (responseText.contains("Run the Gauntlet Gauntlet")) {
      double drainRequirement = BasementRequest.updateHPDrainParameters();
      BasementRequest.basementTest = TestType.HPDRAIN;
      BasementRequest.addDesiredEqualizer();

      // Add some effects that improve Damage Absorption
      if (!KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.ASTRAL_SHELL))) {
        BasementRequest.desirableEffects.add(EffectPool.get(EffectPool.ASTRAL_SHELL));
      }

      if (!KoLConstants.activeEffects.contains(EffectPool.get(EffectPool.GHOSTLY_SHELL))) {
        BasementRequest.desirableEffects.add(EffectPool.get(EffectPool.GHOSTLY_SHELL));
      }

      double damageAbsorb =
          1.0 - (Math.sqrt(Math.min(1000, KoLCharacter.getDamageAbsorption()) / 10.0) - 1.0) / 10.0;
      double healthRequirement = drainRequirement * damageAbsorb;

      if (KoLCharacter.getMaximumHP() < healthRequirement) {
        if (autoSwitch) {
          BasementRequest.changeBasementOutfit("gauntlet");

          damageAbsorb =
              1.0
                  - (Math.sqrt(Math.min(1000, KoLCharacter.getDamageAbsorption()) / 10.0) - 1.0)
                      / 10.0;
          healthRequirement = drainRequirement * damageAbsorb;
          BasementRequest.basementTestValue = (long) healthRequirement;
        }

        if (KoLCharacter.getMaximumHP() < healthRequirement) {
          BasementRequest.basementErrorMessage = "Insufficient health to continue.";
          return true;
        }
      }

      if (autoSwitch) {
        RecoveryManager.recoverHP((long) healthRequirement);
      }

      return true;
    }

    return false;
  }

  private static boolean checkForReward(final String responseText) {
    if (responseText.contains("De Los Dioses")) {
      BasementRequest.basementTestString = "Encounter: De Los Dioses";
      return true;
    }

    if (responseText.contains("The Dusk Zone")) {
      BasementRequest.basementTestString = "Encounter: The Dusk Zone";
      return true;
    }

    if (responseText.contains("Giggity Bobbity Boo!")) {
      BasementRequest.basementTestString = "Encounter: Giggity Bobbity Boo!";
      return true;
    }

    if (responseText.contains("No Good Deed")) {
      BasementRequest.basementTestString = "Encounter: No Good Deed";
      return true;
    }

    if (responseText.contains("<b>Fernswarthy's Basement, Level 500</b>")) {
      BasementRequest.basementTestString = "Encounter: Fernswarthy's Basement, Level 500";
      return true;
    }

    if (responseText.contains("Got Silk?")) {
      BasementRequest.basementTestString = "Encounter: Got Silk?/Leather is Betther";
      return true;
    }

    if (responseText.contains("Save the Dolls")) {
      BasementRequest.basementTestString = "Encounter: Save the Dolls/Save the Cardboard";
      return true;
    }

    if (responseText.contains("Take the Red Pill")) {
      BasementRequest.basementTestString = "Encounter: Take the Red Pill/Take the Blue Pill";
      return true;
    }

    return false;
  }

  private static String monsterLevelString() {
    double level =
        2.0 * Math.pow(BasementRequest.basementLevel, 1.4)
            + KoLCharacter.getMonsterLevelAdjustment();
    return "Monster: Attack/Defense = " + (long) level;
  }

  private static boolean checkForMonster(final String responseText) {
    if (responseText.contains("Don't Fear the Ear")) {
      // Beast with X Ears
      BasementRequest.basementMonster = "Beast with X Ears";
      BasementRequest.basementTestString = BasementRequest.monsterLevelString();
      return true;
    }

    if (responseText.contains("Commence to Pokin")) {
      // Beast with X Eyes
      BasementRequest.basementMonster = "Beast with X Eyes";
      BasementRequest.basementTestString = BasementRequest.monsterLevelString();
      return true;
    }

    if (responseText.contains("Stone Golem")) {
      // X Stone Golem
      BasementRequest.basementMonster = "X Stone Golem";
      BasementRequest.basementTestString = BasementRequest.monsterLevelString();
      return true;
    }

    if (responseText.contains("Hydra")) {
      // X-headed Hydra
      BasementRequest.basementMonster = "X-headed Hydra";
      BasementRequest.basementTestString = BasementRequest.monsterLevelString();
      return true;
    }

    if (responseText.contains("Toast that Ghost")) {
      // Ghost of Fernswarthy's Grandfather
      BasementRequest.basementMonster = "Ghost of Fernswarthy's Grandfather";
      BasementRequest.basementTestString =
          BasementRequest.monsterLevelString() + "<br>Physically resistant";
      return true;
    }

    if (responseText.contains("Bottles of Beer on a Golem")) {
      // X Bottles of Beer on a Golem
      BasementRequest.basementMonster = "X Bottles of Beer on a Golem";
      BasementRequest.basementTestString =
          BasementRequest.monsterLevelString() + "<br>Blocks most spells";
      return true;
    }

    if (responseText.contains("Collapse That Waveform")) {
      // X-dimensional horror
      BasementRequest.basementMonster = "X-dimensional horror";
      BasementRequest.basementTestString =
          BasementRequest.monsterLevelString() + "<br>Blocks physical attacks";
      return true;
    }

    return false;
  }

  private static void newBasementLevel(final String responseText) {
    BasementRequest.basementErrorMessage = null;
    BasementRequest.basementTestString = "None";
    BasementRequest.basementTestValue = 0;

    BasementRequest.element1 = Element.NONE;
    BasementRequest.element2 = Element.NONE;
    BasementRequest.vulnerability = 0;

    BasementRequest.goodelement = Element.NONE;
    BasementRequest.goodphial = null;
    BasementRequest.goodeffect = null;

    BasementRequest.badeffect1 = null;
    BasementRequest.badeffect2 = null;
    BasementRequest.badeffect3 = null;
    BasementRequest.badelement1 = Element.NONE;
    BasementRequest.badelement2 = Element.NONE;
    BasementRequest.badelement3 = Element.NONE;

    Matcher levelMatcher = BasementRequest.BASEMENT_PATTERN.matcher(responseText);
    if (!levelMatcher.find()) {
      return;
    }

    BasementRequest.basementLevel = StringUtilities.parseInt(levelMatcher.group(1));
    KoLAdventure.lastLocationName = BasementRequest.getBasementLevelName();
  }

  public static final void checkBasement() {
    BasementRequest.checkBasement(true, BasementRequest.lastResponseText);
  }

  public static final void checkBasement(final String responseText) {
    BasementRequest.checkBasement(false, responseText);
  }

  public static final void checkBasement(final boolean autoSwitch, final String responseText) {
    BasementRequest.lastResponseText = responseText;

    BasementRequest.desirableEffects.clear();
    BasementRequest.newBasementLevel(responseText);

    if (BasementRequest.checkForReward(responseText)) {
      BasementRequest.basementTest = TestType.REWARD;
      return;
    }

    if (BasementRequest.checkForElementalTest(autoSwitch, responseText)) {
      BasementRequest.basementTest = TestType.ELEMENT;
      return;
    }

    if (BasementRequest.checkForStatTest(autoSwitch, responseText)) {
      return;
    }

    if (BasementRequest.checkForDrainTest(autoSwitch, responseText)) {
      return;
    }

    if (!BasementRequest.checkForMonster(responseText)) {
      return;
    }

    BasementRequest.basementTest = TestType.MONSTER;

    BasementRequest.basementTestCurrent = 0;
    BasementRequest.basementTestValue = 0;

    BasementRequest.actualStatNeeded = Modifiers.HP;
    BasementRequest.primaryBoost = Modifiers.MUS_PCT;
    BasementRequest.secondaryBoost = Modifiers.MUS;

    BasementRequest.addDesiredEqualizer();

    if (autoSwitch) {
      BasementRequest.changeBasementOutfit("damage");
    }
  }

  private static void getStatBoosters(
      final List<AdventureResult> sourceList, final List<StatBooster> targetList) {
    // Cache skills to avoid lots of string lookups
    StatBooster.checkSkills();

    for (AdventureResult effect : sourceList) {
      if (!BasementRequest.wantEffect(effect)) {
        continue;
      }

      StatBooster addition = new StatBooster(effect.getName());

      if (!targetList.contains(addition)) {
        targetList.add(addition);
      }
    }
  }

  private static void addDesirableEffects(final List<AdventureResult> sourceList) {
    for (AdventureResult effect : sourceList) {
      if (BasementRequest.wantEffect(effect)
          && !BasementRequest.desirableEffects.contains(effect)) {
        BasementRequest.desirableEffects.add(effect);
      }
    }
  }

  private static boolean wantEffect(final AdventureResult effect) {
    String action = MoodManager.getDefaultAction("lose_effect", effect.getName());
    if (action.equals("")) {
      return false;
    }

    if (action.startsWith("cast")) {
      return KoLCharacter.hasSkill(UneffectRequest.effectToSkill(effect.getName()));
    }

    return true;
  }

  public static final List<StatBooster> getStatBoosters() {
    List<StatBooster> targetList = new ArrayList<>();

    BasementRequest.getStatBoosters(BasementRequest.desirableEffects, targetList);

    BasementRequest.getStatBoosters(
        Modifiers.getPotentialChanges(BasementRequest.primaryBoost), targetList);
    BasementRequest.getStatBoosters(
        Modifiers.getPotentialChanges(BasementRequest.secondaryBoost), targetList);

    if (BasementRequest.actualStatNeeded == Modifiers.HP) {
      BasementRequest.getStatBoosters(Modifiers.getPotentialChanges(Modifiers.HP_PCT), targetList);
      BasementRequest.getStatBoosters(Modifiers.getPotentialChanges(Modifiers.HP), targetList);
    } else if (BasementRequest.actualStatNeeded == Modifiers.MP) {
      BasementRequest.getStatBoosters(Modifiers.getPotentialChanges(Modifiers.MP_PCT), targetList);
      BasementRequest.getStatBoosters(Modifiers.getPotentialChanges(Modifiers.MP), targetList);
    }

    Collections.sort(targetList);
    return targetList;
  }

  public static long getBasementTestCurrent() {
    return Double.valueOf(BasementRequest.basementTestCurrent).longValue();
  }

  public static long getBasementTestValue() {
    return Double.valueOf(BasementRequest.basementTestValue).longValue();
  }

  public static int getActualStatNeeded() {
    return BasementRequest.actualStatNeeded;
  }

  public static int getPrimaryBoost() {
    return BasementRequest.primaryBoost;
  }

  public static int getSecondaryBoost() {
    return BasementRequest.secondaryBoost;
  }
}
