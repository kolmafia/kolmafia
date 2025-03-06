package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.panel.AdventureSelectPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GoalManagerTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("GoalManagerTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("GoalManagerTest");
  }

  // GoalManager.addGoal() and GoalManager.setGoal()
  //
  // ConditionsCommand command parses goals from a string and calls
  // GoalManager to add, set, or remove goals.
  //
  // AdventureSelectPanel calls ConditionsCommand with user input.  Near
  // as I can tell, that is the only place which can validate a list of
  // goals against an adventuring location.

  @Nested
  class Leprecondo {
    // For now, only the AdventureSelectPanel validates goals.
    //
    // We want to associate furniture with a specific location, so put
    // validation there.

    static AdventureSelectPanel panel = new AdventureSelectPanel(true);

    @Test
    void canSeekTwoFurnitures() {
      var location = AdventureDatabase.getAdventure("Frat House");
      var cleanups = new Cleanups(withProperty("leprecondoDiscovered", ""));
      try (cleanups) {
        GoalManager.clearGoals();
        boolean ok = panel.handleConditions("leprecondo", location);
        assertThat(ok, is(true));
        assertThat(GoalManager.getGoalString(), is("+2 Leprecondo Furniture"));
      }
    }

    @Test
    void canSeekOneFurnitures() {
      var location = AdventureDatabase.getAdventure("Frat House");
      var cleanups = new Cleanups(withProperty("leprecondoDiscovered", "10"));
      GoalManager.clearGoals();
      boolean ok = panel.handleConditions("leprecondo", location);
      assertThat(ok, is(true));
      assertThat(GoalManager.getGoalString(), is("+1 Leprecondo Furniture"));
    }

    @Test
    void canSeekZeroFurnitures() {
      var location = AdventureDatabase.getAdventure("Frat House");
      var cleanups = new Cleanups(withProperty("leprecondoDiscovered", "10,18"));
      GoalManager.clearGoals();
      boolean ok = panel.handleConditions("leprecondo", location);
      assertThat(ok, is(false));
      assertThat(GoalManager.getGoalString(), is(""));
    }
  }
}
