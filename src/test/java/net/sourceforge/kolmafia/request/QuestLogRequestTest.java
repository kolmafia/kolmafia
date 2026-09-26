package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.hasIntegerValue;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isFinished;
import static internal.matchers.Quest.isStarted;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class QuestLogRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("QuestLogRequestTest");
    Preferences.reset("QuestLogRequestTest");
    KoLConstants.inventory.clear();
    AdventureSpentDatabase.resetTurns(false);
  }

  private static Cleanups withQuestPlayerName() {
    var currentUser = KoLCharacter.getUserName();
    KoLCharacter.reset("Ryo_Sangnoir");
    QuestDatabase.reset();
    return new Cleanups(
        () -> {
          KoLCharacter.reset(currentUser);
          QuestDatabase.reset();
        });
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

  @Nested
  class CurrentQuests {
    @Test
    public void parsesQuestProgress() {
      var cleanups = new Cleanups(withQuestPlayerName());

      try (cleanups) {
        QuestLogRequest.registerQuests(
            true, "questlog.php?which=1", html("request/test_questlog_current.html"));

        assertThat(Quest.RAT, isStep(1));
        assertThat(Quest.BAT, isStep(3));
        assertThat(Quest.GOBLIN, isStep(1));
        assertThat(Quest.FRIAR, isStep(1));
        assertThat(Quest.CYRPT, isStarted());
        assertThat(Quest.TRAPPER, isStep(4));
        assertThat(Quest.MACGUFFIN, isStep(2));
        assertThat(Quest.MANOR, isStarted());
        assertThat(Quest.RON, isStep(1));
        assertThat(Quest.DESERT, isStarted());
        assertThat(Quest.ISLAND_WAR, isStep(1));

        // Topping should be step 1 but that would involve changing all the later steps as it's
        // not in the data file at the moment
        // assertThat(Quest.TOPPING, isStep(1));

        // Quest progress inferred from other quests on the page
        assertThat(Quest.BLACK, isFinished());
        assertThat(Quest.TOOT, isFinished());

        assertThat("warProgress", isSetTo("started"));
      }
    }

    @Test
    public void clearsPreferencesForAbsentQuests() {
      var cleanups =
          new Cleanups(
              withQuestPlayerName(),
              withProperty("ghostLocation", "The Spooky Forest"),
              withProperty("_newYouQuestMonster", "fluffy bunny"),
              withProperty("doctorBagQuestItem", "cast"));

      try (cleanups) {
        QuestLogRequest.registerQuests(
            true, "questlog.php?which=1", html("request/test_questlog_current.html"));

        assertThat("ghostLocation", isSetTo(""));
        assertThat("_newYouQuestMonster", isSetTo(""));
        assertThat("doctorBagQuestItem", isSetTo(""));
      }
    }
  }

  @Nested
  class CompletedQuests {
    @Test
    public void parsesCompletedCouncilQuests() {
      var cleanups = new Cleanups(withQuestPlayerName());

      try (cleanups) {
        QuestLogRequest.registerQuests(
            true, "questlog.php?which=2", html("request/test_questlog_completed.html"));

        assertThat(Quest.LARVA, isFinished());
        assertThat(Quest.GARBAGE, isFinished());
        assertThat(Quest.WORSHIP, isFinished());
        assertThat(Quest.SHEN, isFinished());

        // Sub-quests of the worship quest are inferred from its completion
        assertThat(Quest.CURSES, isFinished());
        assertThat(Quest.DOCTOR, isFinished());
        assertThat(Quest.BUSINESS, isFinished());
        assertThat(Quest.SPARE, isFinished());
      }
    }

    @Test
    public void parsesCompletedOtherQuests() {
      var cleanups = new Cleanups(withQuestPlayerName());

      try (cleanups) {
        QuestLogRequest.registerQuests(
            true, "questlog.php?which=2", html("request/test_questlog_completed.html"));

        assertThat(Quest.TEMPLE, isFinished());
        assertThat(Quest.JIMMY_MUSHROOM, isFinished());
        assertThat(Quest.JIMMY_CHEESEBURGER, isFinished());
        assertThat(Quest.TACO_DAN_AUDIT, isFinished());
        assertThat(Quest.TACO_DAN_COCKTAIL, isFinished());
        assertThat(Quest.TACO_DAN_FISH, isFinished());
        assertThat(Quest.BRODEN_BACTERIA, isFinished());
        assertThat(Quest.BRODEN_SPRINKLES, isFinished());
      }
    }
  }
}
