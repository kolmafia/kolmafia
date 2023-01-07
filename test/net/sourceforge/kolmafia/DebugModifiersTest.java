package net.sourceforge.kolmafia;

import static internal.helpers.CliCaller.withCliOutput;
import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.helpers.Cleanups;
import java.io.ByteArrayOutputStream;
import java.time.Month;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DebugModifiersTest {
  private static Matcher<String> containsDebugRow(
      String type, String name, double value, double total) {
    return matchesPattern(
        Pattern.compile(
            ".*<td>"
                + Pattern.quote(type)
                + "</td><td>"
                + Pattern.quote(name)
                + "</td>(<td></td>)*<td>.*"
                + Pattern.quote(KoLConstants.ROUNDED_MODIFIER_FORMAT.format(value))
                + "</td><td>=&nbsp;"
                + Pattern.quote(KoLConstants.ROUNDED_MODIFIER_FORMAT.format(total))
                + "</td>.*",
            Pattern.DOTALL));
  }

  @BeforeAll
  public static void beforeAll() {
    Preferences.reset("DebugModifiersTest");
  }

  private ByteArrayOutputStream outputStream;
  private Cleanups cliCleanups = null;

  @BeforeEach
  public void beforeEach() {
    this.outputStream = new ByteArrayOutputStream();
    this.cliCleanups = withCliOutput(this.outputStream);
  }

  @AfterEach
  public void afterEach() {
    this.cliCleanups.close();
  }

  private String output() {
    return outputStream.toString();
  }

  private void evaluateDebugModifiers(String name) {
    DebugModifiers.setup(name.toLowerCase());
    KoLCharacter.recalculateAdjustments(true);
  }

  private void evaluateDebugModifiers(int index) {
    evaluateDebugModifiers(Modifiers.getModifierName(index));
  }

  @Test
  void listsEffect() {
    try (var cleanups = withEffect(EffectPool.SYNTHESIS_COLLECTION)) {
      evaluateDebugModifiers(Modifiers.ITEMDROP);
    }
    assertThat(output(), containsDebugRow("Effect", "Synthesis: Collection", 150.0, 150.0));
  }

  @Test
  void listsEquipment() {
    try (var cleanups = withEquipped(EquipmentManager.HAT, ItemPool.WAD_OF_TAPE)) {
      evaluateDebugModifiers(Modifiers.ITEMDROP);
    }
    assertThat(output(), containsDebugRow("Item", "wad of used tape", 15.0, 15.0));
  }

  @Test
  void listsPassiveSkill() {
    try (var cleanups = withSkill(SkillPool.COSMIC_UNDERSTANDING)) {
      evaluateDebugModifiers(Modifiers.MP_PCT);
      assertThat(output(), containsDebugRow("Skill", "Cosmic Ugnderstanding", 5.0, 5.0));
    }
  }

  @Test
  void listsMCD() {
    try (var cleanups = withMCD(10)) {
      evaluateDebugModifiers(Modifiers.MONSTER_LEVEL);
    }
    assertThat(output(), containsDebugRow("Mcd", "Monster Control Device", 10.0, 10.0));
  }

  @Test
  void listsSign() {
    try (var cleanups = withSign(ZodiacSign.PACKRAT)) {
      evaluateDebugModifiers(Modifiers.ITEMDROP);
    }
    assertThat(output(), containsDebugRow("Sign", "Packrat", 10.0, 10.0));
  }

  @Test
  void listsSquint() {
    try (var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.HAT, ItemPool.WAD_OF_TAPE),
            withEffect(EffectPool.STEELY_EYED_SQUINT))) {
      evaluateDebugModifiers(Modifiers.ITEMDROP);
    }
    assertThat(output(), containsDebugRow("Item", "wad of used tape", 15.0, 15.0));
    assertThat(output(), containsDebugRow("Effect", "Steely-Eyed Squint", 15.0, 30.0));
  }

  @Test
  void listsMultipleInOneRow() {
    try (var cleanups = withEffect(EffectPool.ELEMENTAL_SPHERE)) {
      evaluateDebugModifiers("Resistance");
    }
    assertThat(output().split("<tr>"), arrayWithSize(3));
    assertThat(
        output(),
        stringContainsInOrder(
            "<td>+2.00</td><td>=&nbsp;+2.00</td>",
            "<td>+2.00</td><td>=&nbsp;+2.00</td>",
            "<td>+2.00</td><td>=&nbsp;+2.00</td>",
            "<td>+2.00</td><td>=&nbsp;+2.00</td>",
            "<td>+2.00</td><td>=&nbsp;+2.00</td>"));
  }

  @Test
  void listsZoneLoc() {
    try (var cleanups = withLocation("The Briniest Deepests")) {
      evaluateDebugModifiers(Modifiers.ITEMDROP);
    }
    assertThat(output(), containsDebugRow("Loc", "The Briniest Deepests", 25.0, 25.0));
    assertThat(output(), containsDebugRow("Zone", "The Sea", -100.0, -75.0));
  }

  @Test
  void listsStatDay() {
    try (var cleanups =
        new Cleanups(withInteractivity(true), withStatDay(KoLConstants.Stat.MUSCLE))) {
      evaluateDebugModifiers(Modifiers.MUS_EXPERIENCE_PCT);
    }
    assertThat(output(), containsDebugRow("Event", "Muscle Day", 25.0, 25.0));
  }

  @Test
  void listsOutfit() {
    try (var cleanups = withOutfit(OutfitPool.WAR_FRAT_OUTFIT)) {

      evaluateDebugModifiers(Modifiers.SLEAZE_DAMAGE);
      assertThat(output(), containsDebugRow("Outfit", "Frat Warrior Fatigues", 15.0, 15.0));
    }
  }

  @Test
  void listsElVibrato() {
    try (var cleanups =
        new Cleanups(
            withOutfit(OutfitPool.VIBRATO_RELICS),
            withAscensions(1),
            withProperty("lastEVHelmetReset", 1),
            withProperty("lastEVHelmetValue", 5))) {
      evaluateDebugModifiers(Modifiers.DAMAGE_REDUCTION);
      assertThat(output(), containsDebugRow("El Vibrato", "WALL", 15.0, 15.0));
    }
  }

  @Test
  void listsFakeHands() {
    try (var cleanups = withFakeHands(10)) {
      evaluateDebugModifiers(Modifiers.WEAPON_DAMAGE);
      assertThat(output(), containsDebugRow("Fake Hands", "fake hand (10)", -10.0, -10.0));
    }
  }

  @Test
  void listsBrimstone() {
    try (var cleanups = withEquipped(ItemPool.BRIMSTONE_BERET)) {
      evaluateDebugModifiers(Modifiers.ITEMDROP);
      assertThat(output(), containsDebugRow("Outfit", "Brimstone", 2.0, 2.0));
    }
  }

  @Test
  void listsCloathing() {
    try (var cleanups = withEquipped(ItemPool.POCKET_SQUARE)) {
      evaluateDebugModifiers(Modifiers.MUS_PCT);
      assertThat(output(), containsDebugRow("Outfit", "Cloathing", 2.0, 2.0));
    }
  }

  @Test
  void listsCampground() {
    try (var cleanups = withCampgroundItem(ItemPool.CLOCKWORK_MAID)) {
      evaluateDebugModifiers(Modifiers.ADVENTURES);
      assertThat(output(), containsDebugRow("Item", "clockwork maid", 8.0, 8.0));
    }
  }

  @Test
  void listsDwelling() {
    try (var cleanups = withDwelling(ItemPool.NEWBIESPORT_TENT)) {
      evaluateDebugModifiers(Modifiers.BASE_RESTING_HP);
      assertThat(output(), containsDebugRow("Item", "Newbiesport™ tent", 9.0, 9.0));
    }
  }

  @Test
  void listsRonaldPhase() {
    try (var cleanups = withDay(2023, Month.JANUARY, 3)) {
      evaluateDebugModifiers(Modifiers.RESTING_MP_PCT);
      assertThat(output(), containsDebugRow("Event", "Moons (Ronald full)", 100.0, 100.0));
    }
  }

  @Test
  void listsGrimacePhase() {
    try (var cleanups = withDay(2023, Month.JANUARY, 15)) {
      evaluateDebugModifiers(Modifiers.RESTING_HP_PCT);
      assertThat(output(), containsDebugRow("Event", "Moons (Grimace full)", 100.0, 100.0));
    }
  }

  @Test
  void listsChateau() {
    try (var cleanups = withChateau(ItemPool.CHATEAU_SKYLIGHT)) {
      evaluateDebugModifiers(Modifiers.ADVENTURES);
      assertThat(output(), containsDebugRow("Item", "artificial skylight", 3.0, 3.0));
    }
  }

  @Test
  void listsRumpus() {
    try (var cleanups =
        new Cleanups(withInteractivity(true), withClanFurniture("Girls of Loathing Calendar"))) {
      evaluateDebugModifiers(Modifiers.ADVENTURES);
      assertThat(output(), containsDebugRow("Rumpus", "Girls of Loathing Calendar", 3.0, 3.0));
    }
  }

  @Test
  void listsSynergy() {
    try (var cleanups = withAllEquipped(ItemPool.BEWITCHING_BOOTS, ItemPool.BITTER_BOWTIE)) {
      evaluateDebugModifiers(Modifiers.MEATDROP);
      assertThat(output(), containsDebugRow("Item", "bewitching boots", 10.0, 10.0));
      assertThat(output(), containsDebugRow("Item", "bitter bowtie", 10.0, 20.0));
      assertThat(
          output(), containsDebugRow("Synergy", "bewitching boots/bitter bowtie", 10.0, 30.0));
    }
  }

  @Test
  void listsFamiliar() {
    try (var cleanups = withFamiliar(FamiliarPool.WOIM, 400)) {
      evaluateDebugModifiers(Modifiers.INITIATIVE);
      assertThat(output(), containsDebugRow("Familiar", "Oily Woim", 40.0, 40.0));
    }
  }

  @Test
  void listsBallroom() {
    try (var cleanups =
        new Cleanups(
            withAscensions(1),
            withProperty("lastQuartetAscension", 1),
            withProperty("lastQuartetRequest", 1))) {
      evaluateDebugModifiers(Modifiers.MONSTER_LEVEL);
      assertThat(output(), containsDebugRow("Ballroom", "ML", 5.0, 5.0));
    }
  }

  @Test
  void listsMummery() {
    try (var cleanups = withProperty("_mummeryMods", "Item Drop: +25")) {
      evaluateDebugModifiers(Modifiers.ITEMDROP);
      assertThat(output(), containsDebugRow("Mummery", "_mummeryMods", 25.0, 25.0));
    }
  }

  @Test
  void listsInventory() {
    try (var cleanups = withItems(ItemPool.FISHING_POLE, ItemPool.ANTIQUE_TACKLEBOX)) {
      evaluateDebugModifiers(Modifiers.FISHING_SKILL);
      assertThat(output(), containsDebugRow("Inventory Item", "fishin' pole", 20.0, 20.0));
      assertThat(output(), containsDebugRow("Inventory Item", "antique tacklebox", 5.0, 25.0));
    }
  }

  @Test
  void listsBoomBox() {
    try (var cleanups = withProperty("boomBoxSong", "Total Eclipse of Your Meat")) {
      evaluateDebugModifiers(Modifiers.MEATDROP);
      assertThat(output(), containsDebugRow("Boom Box", "Total Eclipse of Your Meat", 30.0, 30.0));
    }
  }

  @Test
  void listsHorsery() {
    try (var cleanups = withProperty("_horsery", "dark horse")) {
      evaluateDebugModifiers(Modifiers.COMBAT_RATE);
      assertThat(output(), containsDebugRow("Horsery", "dark horse", -5.0, -5.0));
    }
  }

  @Test
  void listsVote() {
    try (var cleanups = withProperty("_voteModifier", "Item Drop: +10")) {
      evaluateDebugModifiers(Modifiers.ITEMDROP);
      assertThat(output(), containsDebugRow("Local Vote", "_voteModifier", 10.0, 10.0));
    }
  }

  @Test
  void listsPath() {
    try (var cleanups = withPath(AscensionPath.Path.YOU_ROBOT)) {
      evaluateDebugModifiers(Modifiers.ENERGY);
      assertThat(output(), containsDebugRow("Path", "You, Robot", 1.0, 1.0));
    }
  }

  // TODO: Thrall, Autumnaton, Florist, Generated, Hobo Power, Smithsness, Slime Hates It, all
  // specific path effects/path companions, VYKEA
}
