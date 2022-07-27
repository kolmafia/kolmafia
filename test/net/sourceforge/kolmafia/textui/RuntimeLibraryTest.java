package net.sourceforge.kolmafia.textui;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.textui.command.AbstractCommandTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class RuntimeLibraryTest extends AbstractCommandTestBase {

  @BeforeEach
  public void initEach() {
    Preferences.saveSettingsToFile = false;
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);
    Preferences.reset("testUser");
  }

  public RuntimeLibraryTest() {
    this.command = "ash";
  }

  @Test
  void normalMonsterExpectedDamage() {
    String output = execute("expected_damage($monster[blooper])");

    assertContinueState();
    assertThat(output, containsString("Returned: 35"));
  }

  @Test
  void ninjaSnowmanAssassinExpectedDamage() {
    String output = execute("expected_damage($monster[ninja snowman assassin])");

    assertContinueState();
    assertThat(output, containsString("Returned: 297"));
  }

  @Test
  void getPermedSkills() {
    CharSheetRequest.parseStatus(html("request/test_charsheet_normal.html"));

    String outputHardcore = execute("get_permed_skills()[$skill[Nimble Fingers]]");

    assertContinueState();
    assertThat(outputHardcore, containsString("Returned: true"));

    String outputSoftcore = execute("get_permed_skills()[$skill[Entangling Noodles]]");

    assertContinueState();
    assertThat(outputSoftcore, containsString("Returned: false"));

    String outputUnpermed =
        execute(
            "if (get_permed_skills() contains $skill[Emotionally Chipped]) {print(\"permed\");} else {print(\"unpermed\");}");

    assertContinueState();
    assertThat(outputUnpermed, containsString("unpermed"));
  }

  @Test
  void zapWandUnavailable() {
    String output = execute("get_zap_wand()");

    assertContinueState();
    assertThat(output, containsString("Returned: none"));
  }

  @Test
  void zapWandAvailable() {
    final var cleanups = new Cleanups(withItem("marble wand"));

    try (cleanups) {
      String output = execute("get_zap_wand()");

      assertContinueState();
      assertThat(output, containsString("name => marble wand"));
    }
  }

  @Test
  void floundryLocations() {
    // don't try to visit the fireworks shop
    Preferences.setBoolean("_fireworksShop", true);

    var cleanups = withNextResponse(200, html("request/test_clan_floundry.html"));

    try (cleanups) {
      String output = execute("get_fishing_locations()");

      assertContinueState();
      assertThat(output, containsString("Returned: aggregate location [string]"));
      assertThat(output, containsString("bass => Guano Junction"));
      assertThat(output, containsString("carp => Pirates of the Garbage Barges"));
      assertThat(output, containsString("cod => Thugnderdome"));
      assertThat(output, containsString("hatchetfish => The Skeleton Store"));
      assertThat(output, containsString("trout => The Haunted Conservatory"));
      assertThat(output, containsString("tuna => The Oasis"));
    }
  }

  @Nested
  class ExpectedCmc {
    @BeforeEach
    public void beforeEach() {
      HttpClientWrapper.setupFakeClient();
    }

    @Test
    void canVisitCabinet() {
      var cleanups =
          new Cleanups(withNextResponse(200, html("request/test_choice_cmc_frozen_jeans.html")));

      try (cleanups) {
        String output = execute("expected_cold_medicine_cabinet()");
        assertThat(
            output,
            equalTo(
                """
                Returned: aggregate item [string]
                booze => Doc's Fortifying Wine
                equipment => frozen jeans
                food => frozen tofu pop
                pill => Breathitin&trade;
                potion => anti-odor cream
                """));
      }
    }

    @Test
    void canHandleUnexpectedCabinetResponse() {
      var cleanups = new Cleanups(withNextResponse(200, "huh?"));

      try (cleanups) {
        String output = execute("expected_cold_medicine_cabinet()");
        assertThat(
            output,
            equalTo(
                """
                        Could not parse cabinet.
                        Returned: aggregate item [string]
                        booze => none
                        equipment => none
                        food => none
                        pill => none
                        potion => none
                        """));
      }
    }

    @Test
    void canGuessCabinet() {
      var cleanups =
          new Cleanups(withProperty("lastCombatEnvironments", "iiiiiiiiiiioooouuuuu"), withFight());

      try (cleanups) {
        String output = execute("expected_cold_medicine_cabinet()");
        assertThat(
            output,
            equalTo(
                """
                Returned: aggregate item [string]
                booze => Doc's Medical-Grade Wine
                equipment => ice crown
                food => none
                pill => Extrovermectin&trade;
                potion => none
                """));
      }
    }

    @Test
    void canGuessCabinetWithUnknownPill() {
      var cleanups =
          new Cleanups(withProperty("lastCombatEnvironments", "????????????????????"), withFight());

      try (cleanups) {
        String output = execute("expected_cold_medicine_cabinet()");
        assertThat(
            output,
            equalTo(
                """
                Returned: aggregate item [string]
                booze => Doc's Medical-Grade Wine
                equipment => ice crown
                food => none
                pill => none
                potion => none
                """));
      }
    }
  }
}
