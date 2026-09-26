package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.LocketManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReminisceCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void beforeAll() {
    Preferences.reset("ReminisceCommandTest");

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  @BeforeEach
  public void beforeEach() {
    LocketManager.clear();
  }

  public ReminisceCommandTest() {
    this.command = "reminisce";
  }

  @Test
  void mustHaveLocket() {
    String output = execute("Black Crayon Penguin");

    assertThat(output, containsString("You do not own"));
    assertErrorState();
  }

  @Test
  void cannotFightMoreThanThree() {
    LocketManager.rememberMonster(1204);

    var cleanups =
        new Cleanups(
            withItem("combat lover's locket"), withProperty("_locketMonstersFought", "1,3,5"));
    try (cleanups) {
      String output = execute("Black Crayon Penguin");
      assertThat(output, containsString("You can only"));
      assertErrorState();
    }
  }

  @Test
  void mustReminisceSomething() {
    var cleanups = withItem("combat lover's locket");
    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("No monster"));
      assertErrorState();
    }
  }

  @Test
  void mustReminisceAValidMonster() {
    var cleanups = withItem("combat lover's locket");
    try (cleanups) {
      String output = execute("monster that does not exist purple monkey dishwasher");

      assertThat(output, containsString("does not match a monster"));
      assertErrorState();
    }
  }

  @Test
  void cannotFightSameMonsterTwice() {
    LocketManager.rememberMonster(1);

    var cleanups =
        new Cleanups(withItem("combat lover's locket"), withProperty("_locketMonstersFought", "1"));
    try (cleanups) {
      String output = execute("1");

      assertThat(output, containsString("You've already"));
      assertErrorState();
    }
  }

  @Test
  void cannotFightMonsterNotInLocket() {
    assertThat(LocketManager.getMonsters(), not(hasItem(1)));
    var cleanups = withItem("combat lover's locket");
    try (cleanups) {
      String output = execute("1");

      assertThat(output, containsString("You do not have"));
      assertErrorState();
    }
  }

  @Test
  void parsesMonsterById() {
    LocketManager.rememberMonster(1);
    var cleanups = withItem("combat lover's locket");
    try (cleanups) {
      String output = execute("1", true);

      assertThat(output, containsString("spooky vampire"));
      assertContinueState();
    }
  }

  @Test
  void parsesMonsterByName() {
    LocketManager.rememberMonster(1);
    var cleanups = withItem("combat lover's locket");
    try (cleanups) {
      String output = execute("spooky vampire", true);

      assertThat(output, containsString("spooky vampire"));
      assertContinueState();
    }
  }
}
