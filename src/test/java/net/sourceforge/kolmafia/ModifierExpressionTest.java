package net.sourceforge.kolmafia;

import static internal.helpers.Player.withAscensions;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFullness;
import static internal.helpers.Player.withInebriety;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withIntrinsicEffect;
import static internal.helpers.Player.withLevel;
import static internal.helpers.Player.withLocation;
import static internal.helpers.Player.withMoxie;
import static internal.helpers.Player.withMuscle;
import static internal.helpers.Player.withMysticality;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withTurnsPlayed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.time.Month;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ModifierExpressionTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("Expression");
    Preferences.reset("Expression");
  }

  @ParameterizedTest
  @EnumSource(
      value = MonsterDatabase.Element.class,
      names = {"NONE", "SHADOW", "BADSPELLING"},
      mode = EnumSource.Mode.EXCLUDE)
  public void canReadElementalResistance(MonsterDatabase.Element element) {
    ModifierDatabase.overrideModifier(
        ModifierType.GENERATED, "_userMods", element.toTitle() + " Resistance: +10");
    KoLCharacter.recalculateAdjustments();
    var exp = new ModifierExpression("res(" + element + ")", element.toTitle());
    assertEquals(10, exp.eval());
  }

  @ParameterizedTest
  @CsvSource({
    "Natural Born Scrabbler, 1",
    "Thrift and Grift, 0",
    "38, 1",
    "39, 0",
  })
  public void canDetectSkill(String skill, String expected) {
    try (var _ = withSkill("Natural Born Scrabbler")) {
      var exp = new ModifierExpression("skill(" + skill + ")", "Detect skill");
      assertEquals(Double.parseDouble(expected), exp.eval());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Confused, 1",
    "Embarrassed, 0",
    "3, 1",
    "4, 0",
  })
  public void canDetectEffect(String effect, String expected) {
    try (var _ = withEffect("Confused")) {
      var exp = new ModifierExpression("effect(" + effect + ")", "Detect effect");
      assertEquals(Double.parseDouble(expected), exp.eval());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "seal-clubbing club, 1",
    "turtle totem, 0",
  })
  public void canDetectEquip(String item, String expected) {
    try (var _ = withEquipped(Slot.WEAPON, "seal-clubbing club")) {
      var exp = new ModifierExpression("equipped(" + item + ")", "Detect equip");
      assertEquals(Double.parseDouble(expected), exp.eval());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "club, 1",
    "totem, 0",
  })
  public void canDetectMainhandClass(String itemType, String expected) {
    try (var _ = withEquipped(Slot.WEAPON, "seal-clubbing club")) {
      KoLCharacter.recalculateAdjustments();
      var exp = new ModifierExpression("mainhand(" + itemType + ")", "Detect mainhand class");
      assertThat(itemType, exp.eval(), is(Double.parseDouble(expected)));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Adorable Seal Larva, 1",
    "83, 1",
    "Psychedelic Bear, 0",
    "95, 0",
  })
  public void canDetectFamiliar(String familiar, String expected) {
    try (var _ = withFamiliar(FamiliarPool.ADORABLE_SEAL_LARVA)) {
      var exp = new ModifierExpression("fam(" + familiar + ")", "Detect familiar attribute");
      assertThat(familiar, exp.eval(), is(Double.parseDouble(expected)));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "animal, 1",
    "haseyes, 1",
    "flies, 0",
  })
  public void canDetectFamiliarAttribute(String attr, String expected) {
    try (var _ = withFamiliar(FamiliarPool.ADORABLE_SEAL_LARVA)) {
      var exp = new ModifierExpression("famattr(" + attr + ")", "Detect familiar attribute");
      assertThat(attr, exp.eval(), is(Double.parseDouble(expected)));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Noob Cave, 1",
    "The Smut Orc Logging Camp, 0",
    "The Hidden Bowling Alley, 0",
  })
  public void canDetectLocation(String location, String expected) {
    try (var _ = withLocation("Noob Cave")) {
      var exp = new ModifierExpression("loc(" + location + ")", "Detect location");
      assertThat(location, exp.eval(), is(Double.parseDouble(expected)));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Noob Cave, underground, 1",
    "Noob Cave, indoor, 0",
    "The Smut Orc Logging Camp, outdoor, 1",
    "The Smut Orc Logging Camp, underwater, 0",
    "The Hidden Bowling Alley, indoor, 1",
    "The Hidden Bowling Alley, outdoor, 0",
    "The Briny Deeps, underwater, 1",
    "The Briny Deeps, outdoor, 0",
  })
  public void canDetectEnvironment(String location, String env, double expected) {
    try (var _ = withLocation(location)) {
      var exp = new ModifierExpression("env(" + env + ")", "Detect env");
      assertThat(location, exp.eval(), is(expected));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Noob Cave, Mountain, 1",
    "Noob Cave, The Sea, 0",
    "The Smut Orc Logging Camp, Mountain, 1",
    "The Smut Orc Logging Camp, HiddenCity, 0",
    "The Hidden Bowling Alley, HiddenCity, 1",
    "The Hidden Bowling Alley, Mountain, 0",
    "The Briny Deeps, The Sea, 1",
    "The Briny Deeps, Mountain, 0",
  })
  public void canDetectZone(String location, String zone, double expected) {
    try (var _ = withLocation(location)) {
      var exp = new ModifierExpression("zone(" + zone + ")", "Detect zone");
      assertThat(location, exp.eval(), is(expected));
    }
  }

  @ParameterizedTest
  @EnumSource(AscensionClass.class)
  public void canDetectClass(AscensionClass ascensionClass) {
    try (var _ = withClass(AscensionClass.ACCORDION_THIEF)) {
      double expected = ascensionClass == AscensionClass.ACCORDION_THIEF ? 1.0 : 0.0;

      var exp = new ModifierExpression("class(" + ascensionClass.toString() + ")", "Detect class");
      assertThat(ascensionClass.toString(), exp.eval(), is(expected));
    }
  }

  @Test
  public void canDetectHoliday() {
    try (var _ = withDay(2008, Month.FEBRUARY, 17, 12, 0)) {
      var exp =
          new ModifierExpression(
              "event(Sneaky Pete's Day)", ModifierType.EVENT, "Sneaky Pete's day");
      assertThat(exp.eval(), is(1.0));
    }
  }

  @Test
  public void canDetectDecember() {
    try (var _ = withDay(2021, Month.DECEMBER, 3)) {
      var exp = new ModifierExpression("event(December)", ModifierType.EVENT, "December");
      assertThat(exp.eval(), is(1.0));
    }
  }

  @Test
  public void canDetectSaturday() {
    try (var _ = withDay(2023, Month.JUNE, 10)) {
      var exp = new ModifierExpression("event(Saturday)", ModifierType.EVENT, "Saturday");
      assertThat(exp.eval(), is(1.0));
    }
  }

  @Test
  public void canDetectMainstat() {
    try (var _ = withClass(AscensionClass.SEAL_CLUBBER)) {
      var exp = new ModifierExpression("mainstat(muscle)", "Muscle");
      assertThat(exp.eval(), is(1.0));
      exp = new ModifierExpression("mainstat(moxie)", "Moxie");
      assertThat(exp.eval(), is(0.0));
    }
  }

  @ParameterizedTest
  @EnumSource(AscensionPath.Path.class)
  public void canDetectPath(AscensionPath.Path path) {
    try (var _ = withPath(AscensionPath.Path.YOU_ROBOT)) {
      double expected = path == AscensionPath.Path.YOU_ROBOT ? 1.0 : 0.0;

      var exp = new ModifierExpression("path(" + path.toString() + ")", "Detect class");
      assertThat(path.toString(), exp.eval(), is(expected));
    }
  }

  @Test
  public void canUseModFunction() {
    // This special function only returns a meaningful result during the evaluation of
    // recalculateAdjustments so to test it we need to look at an effect that actually uses the
    // mod().

    try (var _ = new Cleanups(withEffect("Bone Springs"), withEffect("Bow-Legged Swagger"))) {
      assertThat(
          ModifierDatabase.getStringModifier(
              ModifierType.EFFECT, "Bow-Legged Swagger", StringModifier.MODIFIERS),
          containsString("mod("));
      KoLCharacter.recalculateAdjustments();

      assertThat(KoLCharacter.getCurrentModifiers().getDouble(DoubleModifier.INITIATIVE), is(40.0));
    }
  }

  @ParameterizedTest
  @CsvSource({"interact, true", "interact(), true", "interact, false", "interact(), false"})
  public void canDetectInteractiveAndAliases(String expression, boolean interact) {
    try (var _ = withInteractivity(interact)) {
      var exp = new ModifierExpression(expression, "Interact");
      assertThat(exp.eval(), is(interact ? 1.0 : 0.0));
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void canDetectRestricted(boolean restricted) {
    try (var _ = withRestricted(restricted)) {
      var exp = new ModifierExpression("restricted", "Restricted");
      assertThat(exp.eval(), is(restricted ? 1.0 : 0.0));
    }
  }

  @Test
  public void canStripCommas() {
    var exp = new ModifierExpression("stripcommas(1,000,000)", "Strip commas");
    assertThat(exp.eval(), is(1000000.0));
  }

  @Test
  public void canDetectAscensions() {
    try (var _ = withAscensions(23)) {
      var exp = new ModifierExpression("A", "Ascensions");
      assertThat(exp.eval(), is(23.0));
    }
  }

  @Test
  public void canDetectBloodofWeresealEffect() {
    double blood = HolidayDatabase.getBloodEffect();
    var exp = new ModifierExpression("B", "Blood of Wereseal Effect");
    assertThat(exp.eval(), is(blood));
  }

  @Test
  public void canDetectMinstrelLevel() {
    KoLCharacter.setMinstrelLevel(43);
    var exp = new ModifierExpression("C", "Clancy's Level");
    assertThat(exp.eval(), is(43.0));
  }

  @Test
  public void canDetectDrunkenness() {
    try (var _ = withInebriety(101)) {
      var exp = new ModifierExpression("D", "Drunkenness");
      assertThat(exp.eval(), is(101.0));
    }
  }

  @Test
  public void canDetectActiveEffectCount() {
    try (var _ =
        new Cleanups(
            withEffect("Leash of Linguini"),
            withEffect("Saucemastery"),
            withEffect("Green Tongue"),
            withIntrinsicEffect("Spirit of Peppermint"))) {
      var exp = new ModifierExpression("E", "Effect Count");
      assertThat(exp.eval(), is(3.0));
    }
  }

  @Test
  public void canDetectFullness() {
    try (var _ = withFullness(202)) {
      var exp = new ModifierExpression("F", "Fullness");
      assertThat(exp.eval(), is(202.0));
    }
  }

  @Test
  public void canDetectGrimaceDarkness() {
    double grimaciteEffect = HolidayDatabase.getGrimaciteEffect() / 10.0;
    var exp = new ModifierExpression("G", "Grimacite Darkness");
    assertThat(exp.eval(), is(grimaciteEffect));
  }

  @Test
  public void canDetectHoboPower() {
    ModifierDatabase.overrideModifier(ModifierType.GENERATED, "_userMods", "Hobo Power: +21");
    KoLCharacter.recalculateAdjustments();

    var exp = new ModifierExpression("H", "Hobo Power");
    assertThat(exp.eval(), is(21.0));
  }

  @Test
  public void canDetectDiscoMomentum() {
    KoLCharacter.setDiscoMomentum(303);
    var exp = new ModifierExpression("I", "Disco Momentum");
    assertThat(exp.eval(), is(303.0));
  }

  @Test
  public void canDetectFestivalOfJarlsberg() {
    try (var _ = withDay(2020, Month.JANUARY, 1)) {
      var exp = new ModifierExpression("J", "Festival of Jarlsberg");
      assertThat(exp.eval(), is(1.0));
    }
  }

  @Test
  public void canDetectSmithness() {
    try (var _ = withEffect("Smithsness Presence")) {
      KoLCharacter.recalculateAdjustments();

      var exp = new ModifierExpression("K", "Smithsness");
      assertThat(exp.eval(), is(10.0));
    }
  }

  @Test
  public void canDetectLevel() {
    try (var _ = new Cleanups(withLevel(3), withClass(AscensionClass.SEAL_CLUBBER))) {
      var exp = new ModifierExpression("L", "Level");
      assertThat(exp.eval(), is(3.0));
    }
  }

  @Test
  public void canDetectMoonlight() {
    double moonlight = HolidayDatabase.getMoonlight();
    var exp = new ModifierExpression("M", "Moonlight");
    assertThat(exp.eval(), is(moonlight));
  }

  @Test
  public void canDetectAudience() {
    KoLCharacter.setAudience(30);
    var exp = new ModifierExpression("N", "Audience");
    assertThat(exp.eval(), is(30.0));
  }

  @Test
  public void canDetectPastaLevel() {
    var thrall = new PastaThrallData(PastaThrallData.PASTA_THRALLS[0]);
    thrall.update(4, "ian");
    KoLCharacter.setPastaThrall(thrall);
    var exp = new ModifierExpression("P", "Pasta Thrall Level");
    assertThat(exp.eval(), is(4.0));
  }

  @ParameterizedTest
  @CsvSource({
    "Impetuous Sauciness, Sauceror, 15",
    "Impetuous Sauciness, Seal Clubber, 10",
    "Drinking to Drink, Sauceror, 10",
    "Drinking to Drink, Accordion Thief, 5",
  })
  public void canDetectReagentPotionDuration(String skill, String cls, String expected) {
    try (var _ = new Cleanups(withClass(AscensionClass.find(cls)), withSkill(skill))) {
      var exp = new ModifierExpression("R", "Reagent potion duration");
      assertThat(exp.eval(), is(Double.parseDouble(expected)));
    }
  }

  @Test
  public void canDetectSpleenUse() {
    KoLCharacter.setSpleenUse(999);
    var exp = new ModifierExpression("S", "Spleen use");
    assertThat(exp.eval(), is(999.0));
  }

  @Test
  public void canDetectEffectDuration() {
    try (var _ = withEffect("Bad Luck", 123)) {
      var exp = new ModifierExpression("T", ModifierType.EFFECT, "Bad Luck");
      assertThat(exp.eval(), is(123.0));
    }
  }

  @Test
  public void invalidEffectHasNoDuration() {
    try (var _ = withEffect("Bad Luck", 123)) {
      var exp = new ModifierExpression("T", "No effect described here");
      assertThat(exp.eval(), is(0.0));
    }
  }

  @Test
  public void canDetectTelescopeUpgrades() {
    KoLCharacter.setTelescopeUpgrades(7);
    var exp = new ModifierExpression("U", "Telescope");
    assertThat(exp.eval(), is(7.0));
  }

  @Test
  public void canDetectFamiliarWeight() {
    try (var _ = withFamiliar(FamiliarPool.ADORABLE_SEAL_LARVA, 100)) {
      var exp = new ModifierExpression("W", "Familiar Weight");
      assertThat(exp.eval(), is(10.0));
    }
  }

  @Test
  public void canDetectGender() {
    KoLCharacter.setGender(Gender.MALE);
    var exp = new ModifierExpression("X", "Gender");
    assertThat(exp.eval(), is(-1.0));
  }

  @Test
  public void canDetectFury() {
    KoLCharacter.setFuryNoCheck(69);
    var exp = new ModifierExpression("Y", "Fury");
    assertThat(exp.eval(), is(69.0));
  }

  @Test
  public void canHandleInvalidBytecode() {
    var exp = new ModifierExpression("Z", "Invalid");
    var result = exp.eval();
    assertThat(result, is(0.0));
    assertTrue(exp.hasErrors());
  }

  @Test
  public void canDetectWarbearFoilHatRobots() {
    try (var _ = new Cleanups(withFamiliar(FamiliarPool.DATASPIDER))) {
      var exp = new ModifierExpression("warbearfoilhat", "Warbear Foil Hat");
      assertThat(exp.eval(), is(1.0));
    }
  }

  @Test
  public void canDetectTotalTurnsPlayed() {
    try (var _ = withTurnsPlayed(665)) {
      var exp = new ModifierExpression("totalturnsplayed", "Total Turns Played");
      assertThat(exp.eval(), is(665.0));
    }
  }

  @Test
  public void canDetectBaseMuscle() {
    try (var _ = new Cleanups(withMuscle(4, 60))) {
      var exp = new ModifierExpression("basemus", "Base muscle");
      assertThat(exp.eval(), is(4.0));
    }
  }

  @Test
  public void canDetectBaseMysticality() {
    try (var _ = new Cleanups(withMysticality(3, 50))) {
      var exp = new ModifierExpression("basemys", "Base mysticality");
      assertThat(exp.eval(), is(3.0));
    }
  }

  @Test
  public void canDetectBaseMoxie() {
    try (var _ = new Cleanups(withMoxie(2, 40))) {
      var exp = new ModifierExpression("basemox", "Base moxie");
      assertThat(exp.eval(), is(2.0));
    }
  }

  @Nested
  class Overrides {
    @Test
    void overrideUnarmedTrue() {
      try (var _ = new Cleanups(withEquipped(Slot.WEAPON, ItemPool.SEAL_CLUB))) {
        var exp = new ModifierExpression("10*unarmed", "unarmed test");
        ExpressionOverrides overrides = new ExpressionOverrides();
        overrides.setUnarmed(true);
        assertThat(exp.eval(), is(0.0));
        assertThat(exp.eval(overrides), is(10.0));

        overrides.setUnarmed(null);
        assertThat(exp.eval(), is(0.0));
        assertThat(exp.eval(overrides), is(0.0));
      }
    }

    @Test
    void overrideUnarmedFalse() {
      try (var _ = new Cleanups()) {
        var exp = new ModifierExpression("10*unarmed", "unarmed test");
        ExpressionOverrides overrides = new ExpressionOverrides();
        overrides.setUnarmed(false);
        assertThat(exp.eval(), is(10.0));
        assertThat(exp.eval(overrides), is(0.0));

        overrides.setUnarmed(null);
        assertThat(exp.eval(), is(10.0));
        assertThat(exp.eval(overrides), is(10.0));
      }
    }
  }

  @Nested
  class SwordOfSwords {
    @ParameterizedTest
    @CsvSource({
      "beer-soaked mop, 0",
      "seal-clubbing club, 1",
      "ancient stone fist, 1",
      "Stick-Knife of Loathing, 1",
      "spooky staff, 2",
      "star sword, 2",
      "legendary pasta wand, 0",
      "legendary seal-clubbing club, 1",
    })
    public void canDetectSwordOfSwords(String item, String expected) {
      try (var _ = withEquipped(Slot.WEAPON, item)) {
        var exp = new ModifierExpression("swordofswords", "Sword of S Words");
        assertThat(item, exp.eval(), is(Double.parseDouble(expected)));
      }
    }

    @Test
    public void canDetectSwordOfSwordsAcrossSlots() {
      try (var _ =
          new Cleanups(
              withEquipped(Slot.WEAPON, "star sword"), withEquipped(Slot.PANTS, "snow pants"))) {
        var exp = new ModifierExpression("swordofswords", "Sword of S Words");
        assertThat(exp.eval(), is(3.0));
      }
    }
  }
}
