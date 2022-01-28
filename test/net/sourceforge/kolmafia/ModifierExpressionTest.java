package net.sourceforge.kolmafia;

import static internal.helpers.Player.addEffect;
import static internal.helpers.Player.addIntrinsic;
import static internal.helpers.Player.addSkill;
import static internal.helpers.Player.equip;
import static internal.helpers.Player.inLocation;
import static internal.helpers.Player.inPath;
import static internal.helpers.Player.isClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
      names = {"NONE"},
      mode = EnumSource.Mode.EXCLUDE)
  public void canReadElementalResistance(MonsterDatabase.Element element) {
    Modifiers.overrideModifier("Generated:_userMods", element.toTitle() + " Resistance: +10");
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
    addSkill("Natural Born Scrabbler");
    var exp = new ModifierExpression("skill(" + skill + ")", "Detect skill");
    assertEquals(Double.parseDouble(expected), exp.eval());
  }

  @ParameterizedTest
  @CsvSource({
    "Confused, 1",
    "Embarrassed, 0",
    "3, 1",
    "4, 0",
  })
  public void canDetectEffect(String effect, String expected) {
    addEffect("Confused");
    var exp = new ModifierExpression("effect(" + effect + ")", "Detect effect");
    assertEquals(Double.parseDouble(expected), exp.eval());
  }

  @ParameterizedTest
  @CsvSource({
    "seal-clubbing club, 1",
    "turtle totem, 0",
  })
  public void canDetectEquip(String item, String expected) {
    equip(EquipmentManager.WEAPON, "seal-clubbing club");
    var exp = new ModifierExpression("equipped(" + item + ")", "Detect equip");
    assertEquals(Double.parseDouble(expected), exp.eval());
  }

  @ParameterizedTest
  @CsvSource({
    "club, 1",
    "totem, 0",
  })
  public void canDetectMainhandClass(String itemType, String expected) {
    equip(EquipmentManager.WEAPON, "seal-clubbing club");
    KoLCharacter.recalculateAdjustments();
    var exp = new ModifierExpression("mainhand(" + itemType + ")", "Detect mainhand class");
    assertThat(itemType, exp.eval(), is(Double.parseDouble(expected)));
  }

  @ParameterizedTest
  @CsvSource({
    "Adorable Seal Larva, 1",
    "83, 1",
    "Psychedelic Bear, 0",
    "95, 0",
  })
  public void canDetectFamiliar(String familiar, String expected) {
    KoLCharacter.setFamiliar(FamiliarData.registerFamiliar(FamiliarPool.ADORABLE_SEAL_LARVA, 1));

    var exp = new ModifierExpression("fam(" + familiar + ")", "Detect familiar attribute");
    assertThat(familiar, exp.eval(), is(Double.parseDouble(expected)));
  }

  @ParameterizedTest
  @CsvSource({
    "animal, 1",
    "eyes, 1",
    "flying, 0",
  })
  public void canDetectFamiliarAttribute(String attr, String expected) {
    var familiar = FamiliarData.registerFamiliar(FamiliarPool.ADORABLE_SEAL_LARVA, 1);
    KoLCharacter.setFamiliar(familiar);

    var exp = new ModifierExpression("famattr(" + attr + ")", "Detect familiar attribute");
    assertThat(attr, exp.eval(), is(Double.parseDouble(expected)));
  }

  @ParameterizedTest
  @CsvSource({
    "Noob Cave, 1",
    "The Smut Orc Logging Camp, 0",
    "The Hidden Bowling Alley, 0",
  })
  public void canDetectLocation(String location, String expected) {
    inLocation("Noob Cave");

    var exp = new ModifierExpression("loc(" + location + ")", "Detect location");
    assertThat(location, exp.eval(), is(Double.parseDouble(expected)));
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
  public void canDetectEnvironment(String location, String env, String expected) {
    inLocation(location);

    var exp = new ModifierExpression("env(" + env + ")", "Detect env");
    assertThat(location, exp.eval(), is(Double.parseDouble(expected)));
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
  public void canDetectZone(String location, String zone, String expected) {
    inLocation(location);

    var exp = new ModifierExpression("zone(" + zone + ")", "Detect zone");
    assertThat(location, exp.eval(), is(Double.parseDouble(expected)));
  }

  @ParameterizedTest
  @EnumSource(AscensionClass.class)
  public void canDetectClass(AscensionClass ascensionClass) {
    isClass(AscensionClass.ACCORDION_THIEF);

    double expected = ascensionClass == AscensionClass.ACCORDION_THIEF ? 1.0 : 0.0;

    var exp = new ModifierExpression("class(" + ascensionClass.toString() + ")", "Detect class");
    assertThat(ascensionClass.toString(), exp.eval(), is(expected));
  }

  @Disabled // This is not really possible to test right now
  @Test
  public void canDetectEvent() {}

  @ParameterizedTest
  @EnumSource(AscensionPath.Path.class)
  public void canDetectPath(AscensionPath.Path path) {
    inPath(AscensionPath.Path.YOU_ROBOT);

    double expected = path == AscensionPath.Path.YOU_ROBOT ? 1.0 : 0.0;

    var exp = new ModifierExpression("path(" + path.toString() + ")", "Detect class");
    assertThat(path.toString(), exp.eval(), is(expected));
  }

  @Test
  public void canUseModFunction() {
    // This special function only returns a meaningful result during the evaluation of
    // recalculateAdjustments so to test it we need to look at an effect that actually uses the
    // mod().

    addEffect("Bone Springs");
    addEffect("Bow-Legged Swagger");
    assertThat(
        Modifiers.getStringModifier("Effect", "Bow-Legged Swagger", "Modifiers"),
        containsString("mod("));
    KoLCharacter.recalculateAdjustments();

    assertThat(KoLCharacter.getCurrentModifiers().get(Modifiers.INITIATIVE), is(40.0));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void canDetectInteractive(boolean interact) {
    CharPaneRequest.setInteraction(interact);

    var exp = new ModifierExpression("interact()", "Interact");
    assertThat(exp.eval(), is(interact ? 1.0 : 0.0));
  }

  @Test
  public void canStripCommas() {
    var exp = new ModifierExpression("stripcommas(1,000,000)", "Strip commas");
    assertThat(exp.eval(), is(1000000.0));
  }

  @Test
  public void canDetectAscensions() {
    KoLCharacter.setAscensions(23);
    var exp = new ModifierExpression("A", "Ascensions");
    assertThat(exp.eval(), is(23.0));
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
    KoLCharacter.setInebriety(101);
    var exp = new ModifierExpression("D", "Drunkenness");
    assertThat(exp.eval(), is(101.0));
  }

  @Test
  public void canDetectActiveEffectCount() {
    addEffect("Leash of Linguini");
    addEffect("Saucemastery");
    addEffect("Green Tongue");
    addIntrinsic("Spirit of Peppermint");

    KoLCharacter.setInebriety(3);
    var exp = new ModifierExpression("E", "Effect Count");
    assertThat(exp.eval(), is(3.0));
  }

  @Test
  public void canDetectFullness() {
    KoLCharacter.setFullness(202);
    var exp = new ModifierExpression("F", "Fullness");
    assertThat(exp.eval(), is(202.0));
  }

  @Test
  public void canDetectGrimaceDarkness() {
    double grimaciteEffect = HolidayDatabase.getGrimaciteEffect() / 10.0;
    var exp = new ModifierExpression("G", "Grimacite Darkness");
    assertThat(exp.eval(), is(grimaciteEffect));
  }

  @Test
  public void canDetectHoboPower() {
    Modifiers.overrideModifier("Generated:_userMods", "Hobo Power: +21");
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

  @Disabled // This is not really possible to test right now
  @Test
  public void canDetectFestivalOfJarlsberg() {}

  @Test
  public void canDetectSmithness() {
    addEffect("Smithsness Presence");
    KoLCharacter.recalculateAdjustments();

    var exp = new ModifierExpression("K", "Smithsness");
    assertThat(exp.eval(), is(10.0));
  }

  @Test
  public void canDetectLevel() {
    isClass(AscensionClass.SEAL_CLUBBER);
    KoLCharacter.setStatPoints(100, 100, 0, 0, 0, 0);
    var exp = new ModifierExpression("L", "Level");
    assertThat(exp.eval(), is(3.0));
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
    isClass(AscensionClass.find(cls));
    addSkill(skill);

    var exp = new ModifierExpression("R", "Reagent potion duration");
    assertThat(exp.eval(), is(Double.parseDouble(expected)));
  }

  @Test
  public void canDetectSpleenUse() {
    KoLCharacter.setSpleenUse(999);
    var exp = new ModifierExpression("S", "Spleen use");
    assertThat(exp.eval(), is(999.0));
  }

  @Test
  public void canDetectEffectDuration() {
    addEffect("Bad Luck", 123);
    var exp = new ModifierExpression("T", "Effect:Bad Luck");
    assertThat(exp.eval(), is(123.0));
  }

  @Test
  public void invalidEffectHasNoDuration() {
    addEffect("Bad Luck", 123);
    var exp = new ModifierExpression("T", "No effect described here");
    assertThat(exp.eval(), is(0.0));
  }

  @Test
  public void canDetectTelescopeUpgrades() {
    KoLCharacter.setTelescopeUpgrades(7);
    var exp = new ModifierExpression("U", "Telescope");
    assertThat(exp.eval(), is(7.0));
  }

  @Test
  public void canDetectFamiliarWeight() {
    KoLCharacter.setFamiliar(FamiliarData.registerFamiliar(FamiliarPool.ADORABLE_SEAL_LARVA, 100));
    var exp = new ModifierExpression("W", "Familiar Weight");
    assertThat(exp.eval(), is(10.0));
  }

  @Test
  public void canDetectGender() {
    KoLCharacter.setGender(KoLCharacter.MALE);
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
}
