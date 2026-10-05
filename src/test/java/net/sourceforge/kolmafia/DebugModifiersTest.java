package net.sourceforge.kolmafia;

import static internal.helpers.CliCaller.withCliOutput;
import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.helpers.Cleanups;
import java.io.ByteArrayOutputStream;
import java.time.Month;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.VYKEACompanionData.VYKEACompanionType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.session.YouRobotManager;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class DebugModifiersTest {

  private static Matcher<String> containsDebugRow(
      String type, String name, double value, double total) {

    // The generated Pattern is incorrect.
    //
    // table rows are wrapped in <tr></tr>
    // The type and name are the first two <td>...</td> in a row
    // We are looking for a specific value/total pair in the row.
    //
    // The Pattern as created, will search past the </tr>

    String pair =
        "<td>"
            + Pattern.quote(KoLConstants.ROUNDED_MODIFIER_FORMAT.format(value))
            + "</td><td>=&nbsp;"
            + Pattern.quote(KoLConstants.ROUNDED_MODIFIER_FORMAT.format(total))
            + "</td>";

    return matchesPattern(
        Pattern.compile(
            ".*<tr><td>"
                + Pattern.quote(type)
                + "</td><td>"
                + Pattern.quote(name)
                + "</td>(<td></td>)*"
                + ".*?"
                + pair
                + ".*",
            Pattern.DOTALL));
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("DebugModifiersTest");
  }

  private ByteArrayOutputStream outputStream;
  private Cleanups cliCleanups = null;

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("DebugModifiersTest");
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
    this.outputStream.reset();
    DebugModifiers.setup(name.toLowerCase());
    KoLCharacter.recalculateAdjustments(true);
  }

  private void evaluateDebugModifiers(DoubleModifier index) {
    evaluateDebugModifiers(index.getName());
  }

  @Test
  void listsEffect() {
    try (var _ = withEffect(EffectPool.SYNTHESIS_COLLECTION)) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
    }
    assertThat(output(), containsDebugRow("Effect", "Synthesis: Collection", 150.0, 150.0));
  }

  @Test
  void listsEquipment() {
    try (var _ = withEquipped(Slot.HAT, ItemPool.WAD_OF_TAPE)) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
    }
    assertThat(output(), containsDebugRow("Item", "wad of used tape", 15.0, 15.0));
  }

  @Test
  void listsAllResistanceForIndividualElement() {
    try (var _ = withEquipped(Slot.HAT, "tin foil hat")) {
      evaluateDebugModifiers("sleaze res");
    }
    assertThat(output(), containsDebugRow("Item", "tin foil hat", 2.0, 2.0));
  }

  @Test
  void listsPassiveSkill() {
    try (var _ = withSkill(SkillPool.COSMIC_UNDERSTANDING)) {
      evaluateDebugModifiers(DoubleModifier.MP_PCT);
      assertThat(output(), containsDebugRow("Skill", "Cosmic Ugnderstanding", 5.0, 5.0));
    }
  }

  @Test
  void listsMCD() {
    try (var _ = withMCD(10)) {
      evaluateDebugModifiers(DoubleModifier.MONSTER_LEVEL);
    }
    assertThat(output(), containsDebugRow("Mcd", "Monster Control Device", 10.0, 10.0));
  }

  @Test
  void listsSign() {
    try (var _ = withSign(ZodiacSign.PACKRAT)) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
    }
    assertThat(output(), containsDebugRow("Sign", "Packrat", 10.0, 10.0));
  }

  @Test
  void listsSquint() {
    try (var _ =
        new Cleanups(
            withEquipped(Slot.HAT, ItemPool.WAD_OF_TAPE),
            withEffect(EffectPool.STEELY_EYED_SQUINT))) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
    }
    assertThat(output(), containsDebugRow("Item", "wad of used tape", 15.0, 15.0));
    assertThat(output(), containsDebugRow("Effect", "Steely-Eyed Squint", 15.0, 30.0));
  }

  @Test
  void listsMultipleInOneRow() {
    try (var _ = withEffect(EffectPool.AVATAR_OF_SHE_WHO_WAS)) {
      evaluateDebugModifiers("Resistance");
    }
    assertThat(output().split("<tr>"), arrayWithSize(3));
    assertThat(
        output(),
        stringContainsInOrder(
            "<td>+5.00</td><td>=&nbsp;+5.00</td>",
            "<td>+5.00</td><td>=&nbsp;+5.00</td>",
            "<td>+5.00</td><td>=&nbsp;+5.00</td>",
            "<td>+5.00</td><td>=&nbsp;+5.00</td>",
            "<td>+5.00</td><td>=&nbsp;+5.00</td>"));
  }

  @Test
  void listsLoc() {
    try (var _ = withLocation("The Briniest Deepests")) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
    }
    assertThat(output(), containsDebugRow("Loc", "The Briniest Deepests", -75.0, -75.0));
  }

  @Test
  void listsZone() {
    try (var _ =
        new Cleanups(
            withLocation("Shadow Rift (Desert Beach)"), withEffect(EffectPool.SPIRIT_OF_TAKING))) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
    }
    assertThat(output(), containsDebugRow("Zone", "Shadow Rift", -8.0, 2.0));
  }

  @Test
  void listsStatDay() {
    try (var _ = new Cleanups(withInteractivity(true), withDay(2023, Month.JULY, 10))) {
      evaluateDebugModifiers(DoubleModifier.MUS_EXPERIENCE_PCT);
    }
    assertThat(output(), containsDebugRow("Event", "Muscle Day", 25.0, 25.0));
  }

  @Test
  void listsOutfit() {
    try (var _ = withOutfit(OutfitPool.WAR_FRAT_OUTFIT)) {
      evaluateDebugModifiers(DoubleModifier.SLEAZE_DAMAGE);
      assertThat(output(), containsDebugRow("Outfit", "Frat Warrior Fatigues", 15.0, 15.0));
    }
  }

  @Test
  void listsElVibrato() {
    try (var _ =
        new Cleanups(
            withOutfit(OutfitPool.VIBRATO_RELICS),
            withAscensions(1),
            withProperty("lastEVHelmetReset", 1),
            withProperty("lastEVHelmetValue", 5))) {
      evaluateDebugModifiers(DoubleModifier.DAMAGE_REDUCTION);
      assertThat(output(), containsDebugRow("El Vibrato", "WALL", 15.0, 15.0));
    }
  }

  @Test
  void listsFakeHands() {
    try (var _ = withFakeHands(10)) {
      evaluateDebugModifiers(DoubleModifier.WEAPON_DAMAGE);
      assertThat(output(), containsDebugRow("Fake Hands", "fake hand (10)", -10.0, -10.0));
    }
  }

  @Test
  void listsBrimstone() {
    try (var _ = withEquipped(ItemPool.BRIMSTONE_BERET)) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
      assertThat(output(), containsDebugRow("Outfit", "Brimstone", 2.0, 2.0));
    }
  }

  @Test
  void listsCloathing() {
    try (var _ = withEquipped(ItemPool.POCKET_SQUARE)) {
      evaluateDebugModifiers(DoubleModifier.MUS_PCT);
      assertThat(output(), containsDebugRow("Outfit", "Cloathing", 2.0, 2.0));
    }
  }

  @Test
  void listsCampground() {
    try (var _ = withCampgroundItem(ItemPool.CLOCKWORK_MAID)) {
      evaluateDebugModifiers(DoubleModifier.ADVENTURES);
      assertThat(output(), containsDebugRow("Item", "clockwork maid", 8.0, 8.0));
    }
  }

  @Test
  void listsDwelling() {
    try (var _ = withDwelling(ItemPool.NEWBIESPORT_TENT)) {
      evaluateDebugModifiers(DoubleModifier.BASE_RESTING_HP);
      assertThat(output(), containsDebugRow("Item", "Newbiesport™ tent", 9.0, 9.0));
    }
  }

  @Test
  void listsRonaldPhase() {
    try (var _ = withDay(2023, Month.JANUARY, 3)) {
      evaluateDebugModifiers(DoubleModifier.RESTING_MP_PCT);
      assertThat(output(), containsDebugRow("Event", "Moons (Ronald full)", 100.0, 100.0));
    }
  }

  @Test
  void listsGrimacePhase() {
    try (var _ = withDay(2023, Month.JANUARY, 15)) {
      evaluateDebugModifiers(DoubleModifier.RESTING_HP_PCT);
      assertThat(output(), containsDebugRow("Event", "Moons (Grimace full)", 100.0, 100.0));
    }
  }

  @Test
  void listsChateau() {
    try (var _ = withChateau(ItemPool.CHATEAU_SKYLIGHT)) {
      evaluateDebugModifiers(DoubleModifier.ADVENTURES);
      assertThat(output(), containsDebugRow("Item", "artificial skylight", 3.0, 3.0));
    }
  }

  @Test
  void listsSynergy() {
    try (var _ = withAllEquipped(ItemPool.BEWITCHING_BOOTS, ItemPool.BITTER_BOWTIE)) {
      evaluateDebugModifiers(DoubleModifier.MEATDROP);
      assertThat(output(), containsDebugRow("Item", "bewitching boots", 10.0, 10.0));
      assertThat(output(), containsDebugRow("Item", "bitter bowtie", 10.0, 20.0));
      assertThat(
          output(), containsDebugRow("Synergy", "bewitching boots/bitter bowtie", 10.0, 30.0));
    }
  }

  @Test
  void listsFamiliar() {
    try (var _ = withFamiliar(FamiliarPool.WOIM, 400)) {
      evaluateDebugModifiers(DoubleModifier.INITIATIVE);
      assertThat(output(), containsDebugRow("Familiar", "Oily Woim", 40.0, 40.0));
    }
  }

  @Test
  void listsThrall() {
    try (var _ = withThrall(SkillPool.BIND_SPICE_GHOST, 10)) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
      assertThat(output(), containsDebugRow("Thrall", "Spice Ghost", 20.0, 20.0));
    }
  }

  @Test
  void listsBallroom() {
    try (var _ =
        new Cleanups(
            withAscensions(1),
            withProperty("lastQuartetAscension", 1),
            withProperty("lastQuartetRequest", 1))) {
      evaluateDebugModifiers(DoubleModifier.MONSTER_LEVEL);
      assertThat(output(), containsDebugRow("Ballroom", "ML", 5.0, 5.0));
    }
  }

  @Test
  void listsMummery() {
    try (var _ = withProperty("_mummeryMods", "Item Drop: +25")) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
      assertThat(output(), containsDebugRow("Mummery", "_mummeryMods", 25.0, 25.0));
    }
  }

  @Test
  void listsInventory() {
    try (var _ = withItems(ItemPool.FISHING_POLE, ItemPool.ANTIQUE_TACKLEBOX)) {
      evaluateDebugModifiers(DoubleModifier.FISHING_SKILL);
      assertThat(output(), containsDebugRow("Inventory Item", "fishin' pole", 20.0, 20.0));
      assertThat(output(), containsDebugRow("Inventory Item", "antique tacklebox", 5.0, 25.0));
    }
  }

  @Test
  void listsBoomBox() {
    try (var _ = withProperty("boomBoxSong", "Total Eclipse of Your Meat")) {
      evaluateDebugModifiers(DoubleModifier.MEATDROP);
      assertThat(output(), containsDebugRow("Boom Box", "Total Eclipse of Your Meat", 30.0, 30.0));
    }
  }

  @Test
  void listsAutumnaton() {
    try (var _ =
        new Cleanups(
            withItem(ItemPool.AUTUMNATON),
            withProperty("autumnatonQuestLocation", "Noob Cave"),
            withProperty("autumnatonQuestTurn", 11),
            withTurnsPlayed(0),
            withLocation("Noob Cave"))) {
      evaluateDebugModifiers(DoubleModifier.EXPERIENCE);
      assertThat(output(), containsDebugRow("Autumnaton", "", 1.0, 1.0));
    }
  }

  @Test
  void listsFlorist() {
    try (var _ =
        new Cleanups(
            withProperty("ownsFloristFriar", true),
            withQuestProgress(QuestDatabase.Quest.LARVA, QuestDatabase.STARTED),
            withFlorist(AdventurePool.NOOB_CAVE, FloristRequest.Florist.HORN_OF_PLENTY))) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
      assertThat(output(), containsDebugRow("Florist", "Horn of Plenty", 25.0, 25.0));
    }
  }

  @Test
  void listsHorsery() {
    try (var _ = withProperty("_horsery", "dark horse")) {
      evaluateDebugModifiers(DoubleModifier.COMBAT_RATE);
      assertThat(output(), containsDebugRow("Horsery", "dark horse", -5.0, -5.0));
    }
  }

  @Test
  void listsVote() {
    try (var _ = withProperty("_voteModifier", "Item Drop: +10")) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
      assertThat(output(), containsDebugRow("Local Vote", "_voteModifier", 10.0, 10.0));
    }
  }

  @Test
  void listsGenerated() {
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.GENERATED, "_userMods", "Item Drop: +50"),
            withOverrideModifiers(ModifierType.GENERATED, "fightMods", "Item Drop: +200"))) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
      assertThat(output(), containsDebugRow("Generated", "_userMods", 50.0, 50.0));
      assertThat(output(), containsDebugRow("Generated", "fightMods", 200.0, 250.0));
    }
  }

  @Test
  void listsHoboPower() {
    try (var _ = withEquipped(ItemPool.HODGMANS_LOBSTERSKIN_PANTS)) {
      evaluateDebugModifiers(DoubleModifier.HOBO_POWER);
      assertThat(output(), containsDebugRow("Item", "Hodgman's lobsterskin pants", 25.0, 25.0));
      assertThat(Modifiers.hoboPower, equalTo(25.0));
    }
  }

  @Test
  void listsSmithsness() {
    try (var _ = withEffect(EffectPool.MERRY_SMITHSNESS)) {
      evaluateDebugModifiers(DoubleModifier.SMITHSNESS);
      assertThat(output(), containsDebugRow("Effect", "Merry Smithsness", 25.0, 25.0));
      assertThat(Modifiers.smithsness, equalTo(25.0));
    }
  }

  @Test
  void listsSlimeHatesIt() {
    try (var _ =
        new Cleanups(withLocation("The Slime Tube"), withEquipped(ItemPool.GRISLY_SHIELD))) {
      evaluateDebugModifiers(DoubleModifier.MONSTER_LEVEL);
      assertThat(output(), containsDebugRow("Outfit", "Slime Hatred", 45.0, 45.0));
    }
  }

  @Test
  void listsPath() {
    try (var _ = withPath(AscensionPath.Path.YOU_ROBOT)) {
      evaluateDebugModifiers(DoubleModifier.ENERGY);
      assertThat(output(), containsDebugRow("Path", "You, Robot", 1.0, 1.0));
    }
  }

  @Test
  void listsSneakyPeteMotorbike() {
    try (var _ =
        new Cleanups(
            withPath(AscensionPath.Path.AVATAR_OF_SNEAKY_PETE),
            withClass(AscensionClass.AVATAR_OF_SNEAKY_PETE),
            withProperty("peteMotorbikeHeadlight", "Sweepy Red Light"))) {
      evaluateDebugModifiers(DoubleModifier.EXPERIENCE);
      assertThat(output(), containsDebugRow("Motorbike", "Sweepy Red Light", 5.0, 5.0));
    }
  }

  @Test
  void listsNuclearAutumnRadSickness() {
    var oldRads = KoLCharacter.getRadSickness();
    KoLCharacter.setRadSickness(100);
    var radCleanups = new Cleanups(() -> KoLCharacter.setRadSickness(oldRads));
    try (var _ = new Cleanups(withPath(AscensionPath.Path.NUCLEAR_AUTUMN), radCleanups)) {
      evaluateDebugModifiers(DoubleModifier.MUS);
      assertThat(output(), containsDebugRow("Path", "Rads", -100.0, -100.0));
    }
  }

  @Test
  void listsBorisMinstrel() {
    try (var _ =
        new Cleanups(
            withPath(AscensionPath.Path.AVATAR_OF_BORIS),
            withClass(AscensionClass.AVATAR_OF_BORIS))) {
      var oldLevel = KoLCharacter.getMinstrelLevel();
      var oldInstrument = KoLCharacter.getCurrentInstrument();
      KoLCharacter.setMinstrelLevel(5);
      KoLCharacter.setCurrentInstrument(ItemPool.get(ItemPool.CLANCY_LUTE, 1));
      try (var _ =
          new Cleanups(
              () -> {
                KoLCharacter.setMinstrelLevel(oldLevel);
                KoLCharacter.setCurrentInstrument(oldInstrument);
              })) {
        evaluateDebugModifiers(DoubleModifier.ITEMDROP);
        assertThat(output(), containsDebugRow("Clancy", "Clancy's lute", 59.08, 59.08));
      }
    }
  }

  @Test
  void listsJarlsbergCompanion() {
    var oldCompanion = KoLCharacter.getCompanion();
    KoLCharacter.setCompanion(CharPaneRequest.Companion.EGGMAN);
    var companionCleanups = new Cleanups(() -> KoLCharacter.setCompanion(oldCompanion));
    try (var _ =
        new Cleanups(
            withPath(AscensionPath.Path.AVATAR_OF_JARLSBERG),
            withClass(AscensionClass.AVATAR_OF_JARLSBERG),
            companionCleanups)) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
      assertThat(output(), containsDebugRow("Companion", "Eggman", 50.0, 50.0));
    }
  }

  @Test
  void listsEdServant() {
    EdServantData.initialize();
    EdServantData.testSetupEdServant("Maid", "xxx", 196);
    var servantCleanups = new Cleanups(EdServantData::initialize);
    try (var _ =
        new Cleanups(
            withPath(AscensionPath.Path.ACTUALLY_ED_THE_UNDYING),
            withClass(AscensionClass.ED),
            servantCleanups)) {
      evaluateDebugModifiers(DoubleModifier.MEATDROP);
      assertThat(output(), containsDebugRow("Servant", "Maid", 77.50, 77.50));
    }
  }

  @Test
  void listsGelatinousNoobAbsorbs() {
    try (var _ =
        new Cleanups(
            withPath(AscensionPath.Path.GELATINOUS_NOOB),
            withOverrideModifiers(
                ModifierType.GENERATED, "Enchantments Absorbed", "Item Drop: +50"))) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
      assertThat(output(), containsDebugRow("Generated", "Enchantments Absorbed", 50.0, 50.0));
    }
  }

  @Test
  void listsDisguisesDelimitMask() {
    var oldMask = KoLCharacter.getMask();
    KoLCharacter.setMask("protest mask");
    var maskCleanups = new Cleanups(() -> KoLCharacter.setMask(oldMask));
    try (var _ = new Cleanups(withPath(AscensionPath.Path.DISGUISES_DELIMIT), maskCleanups)) {
      evaluateDebugModifiers(DoubleModifier.MONSTER_LEVEL);
      assertThat(output(), containsDebugRow("Mask", "protest mask", 30.0, 30.0));
    }
  }

  @Test
  void listsEnsorcel() {
    try (var _ =
        new Cleanups(
            withClass(AscensionClass.VAMPYRE),
            withEquipped(ItemPool.VAMPYRIC_CLOAKE),
            withProperty("ensorcelee", "Spant soldier"),
            withProperty("ensorceleeLevel", 1500))) {
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
      assertThat(output(), containsDebugRow("Item", "vampyric cloake", 15.0, 15.0));
      assertThat(output(), containsDebugRow("Item", "vampyric cloake", 75.0, 90.0));
      assertThat(output(), containsDebugRow("Ensorcel", "bug", 300.0, 390.0));
    }
  }

  @Test
  void listsRobot() {
    var robotCleanups = new Cleanups(YouRobotManager::reset);
    try (var _ = new Cleanups(withPath(AscensionPath.Path.YOU_ROBOT), robotCleanups)) {
      YouRobotManager.reset();
      YouRobotManager.testInstallUpgrade(YouRobotManager.RobotUpgrade.IMPROVED_OPTICAL_PROCESSING);
      evaluateDebugModifiers(DoubleModifier.ITEMDROP);
      assertThat(output(), containsDebugRow("Robot", "Improved Optical Processing", 20.0, 20.0));
    }
  }

  @Test
  void listsVykea() {
    try (var _ = withVykea(VYKEACompanionType.COUCH, 5)) {
      evaluateDebugModifiers(DoubleModifier.MEATDROP);
      assertThat(output(), containsDebugRow("Vykea", "Couch", 50.0, 50.0));
    }
  }

  @Test
  void listsWaterLevelExp() {
    try (var _ =
        new Cleanups(withPath(AscensionPath.Path.HEAVY_RAINS), withLocation("Noob Cave"))) {
      evaluateDebugModifiers(DoubleModifier.EXPERIENCE);
      assertThat(output(), containsDebugRow("Path", "Water Level*10/3", 16.67, 16.67));
    }
  }

  @Test
  void listsStatExpNormal() {
    try (var _ =
        new Cleanups(
            withLocation("Noob Cave"),
            withClass(AscensionClass.SEAL_CLUBBER),
            withEquipped(ItemPool.SUGAR_SHIRT))) {
      evaluateDebugModifiers(DoubleModifier.MUS_EXPERIENCE);
      assertThat(output(), containsDebugRow("Class", "EXP/2", 3.0, 3.0));
      evaluateDebugModifiers(DoubleModifier.MYS_EXPERIENCE);
      assertThat(output(), containsDebugRow("Class", "EXP/4", 1.0, 1.0));
      evaluateDebugModifiers(DoubleModifier.MOX_EXPERIENCE);
      assertThat(output(), containsDebugRow("Class", "EXP/4", 1.0, 1.0));
    }
  }

  @Test
  void listsStatExpTuned() {
    try (var _ =
        new Cleanups(
            withLocation("Noob Cave"),
            withClass(AscensionClass.SEAL_CLUBBER),
            withAllEquipped(ItemPool.SUGAR_SHIRT, ItemPool.MIME_ARMY_INSIGNIA_INFANTRY))) {
      evaluateDebugModifiers(DoubleModifier.MUS_EXPERIENCE);
      assertThat(output(), containsDebugRow("Class", "EXP", 5.0, 5.0));
    }
  }

  @ParameterizedTest
  @CsvSource({"_hareAdv,Wild Hare", "_gibbererAdv,Squamous Gibberer"})
  void listsFamiliarAdventures(String preference, String familiar) {
    try (var _ = new Cleanups(withProperty(preference, 6))) {
      evaluateDebugModifiers(DoubleModifier.ADVENTURES);
      assertThat(output(), containsDebugRow("Base", "", 40.0, 40.0));
      assertThat(output(), containsDebugRow("Familiar", familiar, 6.0, 46.0));
    }
  }

  @Test
  void discoNapGivesFreeRests() {
    try (var _ = withSkill(SkillPool.DISCO_NAP)) {
      evaluateDebugModifiers(DoubleModifier.FREE_RESTS);
      assertThat(output(), containsDebugRow("Skill", "Disco Nap", 1.0, 1.0));
    }
  }
}
