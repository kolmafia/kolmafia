package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JourneyCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);
  }

  public JourneyCommandTest() {
    this.command = "journey";
  }

  @Test
  void mustProvideCommand() {
    String output = execute("");

    assertThat(
        output,
        containsString(
            "Usage: journey zones [SC | TT | PM | S | AT | DB]| find [all | SC | TT | PM | S | AT | DB] <skill> - Journeyman skill utility."));
  }

  @Test
  void mustProvideValidCommand() {
    String output = execute("lose");

    assertThat(
        output,
        containsString(
            "Usage: journey zones [SC | TT | PM | S | AT | DB]| find [all | SC | TT | PM | S | AT | DB] <skill> - Journeyman skill utility."));
  }

  @Test
  void mustProvideValidSkillOnPath() {
    var cleanups =
        new Cleanups(
            Player.isClass(AscensionClass.ACCORDION_THIEF),
            Player.isSign(ZodiacSign.VOLE),
            Player.inPath(Path.JOURNEYMAN));

    try (cleanups) {
      String output = execute("find booga booga");
      assertThat(output, containsString("I don't know a skill named \"booga booga\""));
    }
  }

  @Test
  void canFindNotKnownSkillOnPath() {
    var cleanups =
        new Cleanups(
            Player.isClass(AscensionClass.ACCORDION_THIEF),
            Player.isSign(ZodiacSign.VOLE),
            Player.inPath(Path.JOURNEYMAN),
            Player.addItem(ItemPool.LAB_KEY));

    try (cleanups) {
      String output = execute("find pulverize");
      assertThat(
          output,
          containsString("You can learn \"Pulverize\" after 4 turns in Cobb's Knob Laboratory."));
    }
  }

  @Test
  void canFindNotKnownInaccessibleSkillOnPath() {
    var cleanups =
        new Cleanups(
            Player.isClass(AscensionClass.ACCORDION_THIEF),
            Player.isSign(ZodiacSign.VOLE),
            Player.inPath(Path.JOURNEYMAN),
            Player.setProperty("questL07Cyrptic", "unstarted"));

    try (cleanups) {
      String output = execute("find springy fusilli");
      assertThat(
          output,
          containsString(
              "You can learn \"Springy Fusilli\" after 8 turns in The VERY Unquiet Garves (which is not currently accessible to you)."));
    }
  }

  @Test
  void canFindZoneRestrictedOnPath() {
    var cleanups =
        new Cleanups(
            Player.isClass(AscensionClass.ACCORDION_THIEF),
            Player.isSign(ZodiacSign.VOLE),
            Player.inPath(Path.JOURNEYMAN),
            Player.setProperty("questL07Cyrptic", "unstarted"));

    try (cleanups) {
      String output = execute("find wave of sauce");
      assertThat(
          output,
          containsString(
              "You can learn \"Wave of Sauce\" after 12 turns in Camp Logging Camp (which is not currently accessible to you)."));
    }
  }

  @Test
  void canFindKnownSkillOnPath() {
    var cleanups =
        new Cleanups(
            Player.isClass(AscensionClass.ACCORDION_THIEF),
            Player.isSign(ZodiacSign.VOLE),
            Player.inPath(Path.JOURNEYMAN),
            Player.addSkill("Advanced Saucecrafting"));

    try (cleanups) {
      String output = execute("find advanced sauce");
      assertThat(
          output,
          containsString(
              "You already learned \"Advanced Saucecrafting\" after 12 turns in The Valley of Rof L'm Fao."));
    }
  }

  @Test
  void canIdentifyUnavailableSkillOnPath() {
    var cleanups =
        new Cleanups(
            Player.isClass(AscensionClass.ACCORDION_THIEF),
            Player.isSign(ZodiacSign.VOLE),
            Player.inPath(Path.JOURNEYMAN));

    try (cleanups) {
      String output = execute("find sing");
      assertThat(output, containsString("The \"Sing\" skill is not available to Journeymen."));
    }
  }

  @Test
  void canFindSkillNotOnPath() {
    // No Cleanups needed if not on class; you don't even need to be logged in.
    String output = execute("find S saucegeyser");
    assertThat(
        output,
        containsString(
            "A Journeyman Sauceror can learn \"Saucegeyser\" after 16 turns in Cobb's Knob Laboratory."));
  }

  @Test
  void canFindSkillForAllClassesNotOnPath() {
    // No Cleanups needed if not on class; you don't even need to be logged in.
    String output = execute("find all saucegeyser");
    assertThat(
        output,
        containsString(
            "A Journeyman Seal Clubber can learn \"Saucegeyser\" after 24 turns in The Obligatory Pirate's Cove."));
    assertThat(
        output,
        containsString(
            "A Journeyman Turtle Tamer can learn \"Saucegeyser\" after 16 turns in Infernal Rackets Backstage."));
    assertThat(
        output,
        containsString(
            "A Journeyman Pastamancer can learn \"Saucegeyser\" after 16 turns in The Haiku Dungeon."));
    assertThat(
        output,
        containsString(
            "A Journeyman Sauceror can learn \"Saucegeyser\" after 16 turns in Cobb's Knob Laboratory."));
    assertThat(
        output,
        containsString(
            "A Journeyman Disco Bandit can learn \"Saucegeyser\" after 8 turns in The Dire Warren."));
    assertThat(
        output,
        containsString(
            "A Journeyman Accordion Thief can learn \"Saucegeyser\" after 12 turns in Cobb's Knob Menagerie, Level 1."));
  }
}
