package net.sourceforge.kolmafia;

import static internal.helpers.Player.equip;
import static internal.helpers.Player.inPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BasementRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

public class MonsterExpressionTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("Expression");
    Preferences.reset("Expression");
  }

  @Test
  public void canDetectAdjustedMuscle() {
    KoLCharacter.setStatPoints(60, 10, 0, 0, 0, 0);

    var exp = new MonsterExpression("MUS", "Adjusted muscle");
    assertThat(exp.eval(), is(60.0));
  }

  @Test
  public void canDetectAdjustedMysticality() {
    KoLCharacter.setStatPoints(0, 0, 50, 10, 0, 0);

    var exp = new MonsterExpression("MYS", "Adjusted mysticality");
    assertThat(exp.eval(), is(50.0));
  }

  @Test
  public void canDetectAdjustedMoxie() {
    KoLCharacter.setStatPoints(0, 0, 0, 0, 40, 10);

    var exp = new MonsterExpression("MOX", "Adjusted moxie");
    assertThat(exp.eval(), is(40.0));
  }

  @Test
  public void canDetectMonsterLevel() {
    Modifiers.overrideModifier("Generated:_userMods", "Monster Level: +9");
    KoLCharacter.recalculateAdjustments();

    var exp = new MonsterExpression("ML", "Monster Level");
    assertThat(exp.eval(), is(9.0));
  }

  @Test
  public void canDetectMindControllingDevice() {
    KoLCharacter.setMindControlLevel(4);

    var exp = new MonsterExpression("MCD", "Mind Controlling Device");
    assertThat(exp.eval(), is(4.0));
  }

  @Test
  public void canDetectMaximumHP() {
    KoLCharacter.setHP(10, 50, 40);

    var exp = new MonsterExpression("HP", "Maximum HP");
    assertThat(exp.eval(), is(50.0));
  }

  @Test
  public void canDetectBasementLevel() throws IOException {
    BasementRequest.checkBasement(
        Files.readString(Path.of("request/test_basement_level_1234.html")));
    var exp = new MonsterExpression("BL", "Basement Level");
    assertThat(exp.eval(), is(1234.0));
  }

  @Test
  public void canDetectKissesInDreadWoods() throws IOException {
    KoLAdventure.setLastAdventure(AdventureDatabase.getAdventure("Dreadsylvanian Woods"));
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("stench bugbear"));

    var req = new GenericRequest("fight.php");
    req.setHasResult(true);
    req.responseText = Files.readString(Path.of("request/test_fight_stench_bugbear_4_kisses.html"));
    req.processResponse();

    var exp = new MonsterExpression("KW", "Kisses in Dread Woods");
    assertThat(exp.eval(), is(4.0));
  }

  @Test
  public void canDetectKissesInDreadVillage() throws IOException {
    KoLAdventure.setLastAdventure(AdventureDatabase.getAdventure("Dreadsylvanian Village"));
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("spooky zombie"));

    var req = new GenericRequest("fight.php");
    req.setHasResult(true);
    req.responseText = Files.readString(Path.of("request/test_fight_spooky_zombie_1_kiss.html"));
    req.processResponse();

    var exp = new MonsterExpression("KW", "Kisses in Dread Village");
    assertThat(exp.eval(), is(1.0));
  }

  @Test
  public void canDetectKissesInDreadCastle() throws IOException {
    KoLAdventure.setLastAdventure(AdventureDatabase.getAdventure("Dreadsylvanian Castle"));
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("stench vampire"));

    var req = new GenericRequest("fight.php");
    req.setHasResult(true);
    req.responseText = Files.readString(Path.of("request/test_fight_stench_vampire_2_kisses.html"));
    req.processResponse();

    var exp = new MonsterExpression("KC", "Kisses in Dread Castle");
    assertThat(exp.eval(), is(2.0));
  }

  @Test
  public void canDetectAdjustedHighestStat() {
    KoLCharacter.setStatPoints(1, 9, 2, 8, 3, 7);

    var exp = new MonsterExpression("STAT", "Adjusted Highest Stat");
    assertThat(exp.eval(), is(3.0));
  }

  @ParameterizedTest
  @EnumSource(AscensionPath.Path.class)
  public void canDetectPath(AscensionPath.Path path) {
    inPath(AscensionPath.Path.YOU_ROBOT);

    double expected = path == AscensionPath.Path.YOU_ROBOT ? 1.0 : 0.0;
    var exp = new MonsterExpression("path(" + path.toString() + ")", "Detect class");

    assertThat(path.toString(), exp.eval(), is(expected));
  }

  @ParameterizedTest
  @CsvSource({
    "seal-clubbing club, 1",
    "turtle totem, 0",
  })
  public void canDetectEquip(String item, String expected) {
    equip(EquipmentManager.WEAPON, "seal-clubbing club");
    var exp = new MonsterExpression("equipped(" + item + ")", "Detect equip");

    assertEquals(Double.parseDouble(expected), exp.eval());
  }
}
