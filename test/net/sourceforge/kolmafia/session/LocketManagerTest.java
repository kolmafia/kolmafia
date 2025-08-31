package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocketManagerTest {
  @BeforeAll
  public static void beforeAll() {
    Preferences.reset("LocketManagerTest");
  }

  @BeforeEach
  public void beforeEach() {
    LocketManager.clear();
  }

  @Test
  public void canHandleLargeListOfMonsters() {
    String html = html("request/test_choice_reminisce_full.html");
    LocketManager.parseMonsters(html);
    assertThat(LocketManager.getMonsters(), hasSize(276));
  }

  @Test
  public void detectsLocketFight() {
    var html = html("request/test_fight_start_locket_fight_with_horror.html");
    assertThat(LocketManager.isLocketFight(html), is(true));
  }

  @Test
  public void addsFightToMonsterList() {
    try (var cleanups = withProperty("_locketMonstersFought", "5")) {
      var monster = MonsterDatabase.findMonster("alielf");
      LocketManager.parseFight(monster);

      assertThat("_locketMonstersFought", isSetTo("5,1092"));
    }
  }

  @Test
  public void addsFightToMonsterListWithoutDuplicating() {
    try (var cleanups = withProperty("_locketMonstersFought", "5,1092")) {
      var monster = MonsterDatabase.findMonster("alielf");
      LocketManager.parseFight(monster);

      assertThat("_locketMonstersFought", isSetTo("5,1092"));
    }
  }

  @Test
  public void foughtMonstersResetsOnNewDay() {
    try (var cleanups = withProperty("_locketMonstersFought", "5")) {
      LocketManager.getFoughtMonsters();
      try (var cleanups2 = withProperty("_locketMonstersFought", "")) {
        assertThat(LocketManager.getFoughtMonsters(), empty());
      }
    }
  }

  @Test
  public void canDecorateAmbiguousMonsters() {
    String html = html("request/test_choice_reminisce_decorate_gremlins.html");
    StringBuffer buffer = new StringBuffer(html);

    // <option value="548" >batwinged gremlin</option>
    // <option value="549" >batwinged gremlin</option>
    // <option value="546" >erudite gremlin</option>
    // <option value="547" >erudite gremlin</option>
    // <option value="552" >spider gremlin</option>
    // <option value="553" >spider gremlin</option>
    // <option value="550" >vegetable gremlin</option>
    // <option value="551" >vegetable gremlin</option>

    assertTrue(buffer.indexOf("<option value=\"549\" >batwinged gremlin</option>") != -1);
    assertTrue(buffer.indexOf("<option value=\"547\" >erudite gremlin</option>") != -1);
    assertTrue(buffer.indexOf("<option value=\"553\" >spider gremlin</option>") != -1);
    assertTrue(buffer.indexOf("<option value=\"551\" >vegetable gremlin</option>") != -1);

    // Ultimately calls LocketManager.decorateMonsterDropdown(buffer);
    ChoiceAdventures.decorateChoice(1463, buffer, true);

    assertTrue(buffer.indexOf("<option value=\"549\" >batwinged gremlin (tool)</option>") != -1);
    assertTrue(buffer.indexOf("<option value=\"547\" >erudite gremlin (tool)</option>") != -1);
    assertTrue(buffer.indexOf("<option value=\"553\" >spider gremlin (tool)</option>") != -1);
    assertTrue(buffer.indexOf("<option value=\"551\" >vegetable gremlin (tool)</option>") != -1);
  }
}
