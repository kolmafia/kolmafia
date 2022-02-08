package net.sourceforge.kolmafia.session;

import static internal.helpers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
  public void canHandleLargeListOfMonsters() throws IOException {
    String html = Files.readString(Path.of("request/test_choice_reminisce_full.html"));
    LocketManager.parseMonsters(html);
    assertThat(LocketManager.getMonsters(), hasSize(276));
  }

  @Test
  public void addsFightToMonsterList() throws IOException {
    Preferences.setString("_locketMonstersFought", "5");

    var monster = MonsterDatabase.findMonster("alielf");
    var html = Files.readString(Path.of("request/test_fight_start_locket_fight_with_horror.html"));
    LocketManager.parseFight(monster, html);

    assertThat("_locketMonstersFought", isSetTo("5,1092"));
  }

  @Test
  public void addsFightToMonsterListWithoutDuplicating() throws IOException {
    Preferences.setString("_locketMonstersFought", "5,1092");

    var monster = MonsterDatabase.findMonster("alielf");
    var html = Files.readString(Path.of("request/test_fight_start_locket_fight_with_horror.html"));
    LocketManager.parseFight(monster, html);

    assertThat("_locketMonstersFought", isSetTo("5,1092"));
  }
}
