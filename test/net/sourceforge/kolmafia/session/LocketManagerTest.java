package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LocketManagerTest {
  @BeforeEach
  private void beforeEach() {
    KoLCharacter.reset("LocketManagerTest");
    Preferences.reset("LocketManagerTest");
  }

  @Test
  public void canHandleLargeListOfMonsters() {
    String html = html("request/test_choice_reminisce_full.html");
    LocketManager.parseMonsters(html);
    assertThat(LocketManager.getMonsters(), hasSize(276));
  }

  @Test
  public void addsFightToMonsterList() {
    Preferences.setString("_locketMonstersFought", "5");

    var monster = MonsterDatabase.findMonster("alielf");
    var html = html("request/test_fight_start_locket_fight_with_horror.html");
    LocketManager.parseFight(monster, html);

    assertThat("_locketMonstersFought", isSetTo("5,1092"));
  }

  @Test
  public void addsFightToMonsterListWithoutDuplicating() {
    Preferences.setString("_locketMonstersFought", "5,1092");

    var monster = MonsterDatabase.findMonster("alielf");
    var html = html("request/test_fight_start_locket_fight_with_horror.html");
    LocketManager.parseFight(monster, html);

    assertThat("_locketMonstersFought", isSetTo("5,1092"));
  }
}
