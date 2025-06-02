package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.hasIntegerValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QuestLogRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("QuestLogRequestTest");
    Preferences.reset("QuestLogRequestTest");
    KoLConstants.inventory.clear();
    AdventureSpentDatabase.resetTurns(false);
  }

  @Test
  public void canParseExploathingBattlefield() {
    var cleanups =
        new Cleanups(
            withPath(Path.KINGDOM_OF_EXPLOATHING),
            withProperty("hippiesDefeated", 0),
            withProperty("fratboysDefeated", 0));

    try (cleanups) {
      QuestLogRequest.registerQuests(
          true, "questlog.php?which=1", html("request/test_quest_exploathing_battlefield.html"));

      assertThat("hippiesDefeated", hasIntegerValue(is(317)));
      assertThat("fratboysDefeated", hasIntegerValue(is(4)));
    }
  }
}
