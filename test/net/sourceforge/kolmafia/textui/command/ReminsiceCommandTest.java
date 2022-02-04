package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.addItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.LocketManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReminsiceCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("ReminisceCommandTest");
    Preferences.reset("ReminisceCommandTest");
    // new ReminsiceCommandFakeRequest().register("ReminisceCommandTest");

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  @AfterEach
  public void afterEach() {
    KoLCharacter.reset("");
  }

  public ReminsiceCommandTest() {
    this.command = "reminisce";
  }

  @Test
  void mustHaveLocket() {
    String output = execute("");

    assertThat(output, containsString("You do not have"));
    assertErrorState();
  }

  @Test
  void cannotFightMoreThanThree() {
    Preferences.setString("_locketMonstersFought", "1,3,5");
    LocketManager.parseFoughtMonsters();
    var cleanups = addItem("combat lover's locket");
    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("You can only"));
      assertErrorState();
    }
  }

  @Test
  void mustReminisceSomething() {
    var cleanups = addItem("combat lover's locket");
    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("No monster"));
      assertErrorState();
    }
  }

  @Test
  void mustReminisceAValidMonster() {
    LocketManager.parseFoughtMonsters();
    var cleanups = addItem("combat lover's locket");
    try (cleanups) {
      String output = execute("monster that does not exist purple monkey dishwasher");

      assertThat(output, containsString("does not match a monster"));
      assertErrorState();
    }
  }

  @Test
  void cannotFightSameMonsterTwice() {
    Preferences.setString("_locketMonstersFought", "1");
    LocketManager.parseFoughtMonsters();
    var cleanups = addItem("combat lover's locket");
    try (cleanups) {
      String output = execute("1");

      assertThat(output, containsString("You've already"));
      assertErrorState();
    }
  }

  @Test
  void cannotFightMonsterNotInLocket() {
    assertThat(LocketManager.getMonsters(), not(hasItem(1)));
    var cleanups = addItem("combat lover's locket");
    try (cleanups) {
      String output = execute("1");

      assertThat(output, containsString("You do not have"));
      assertErrorState();
    }
  }

  @Test
  void parsesMonsterById() {
    LocketManager.rememberMonster(1);
    var cleanups = addItem("combat lover's locket");
    try (cleanups) {
      String output = execute("1", true);

      assertThat(output, containsString("spooky vampire"));
      assertContinueState();
    }
  }

  @Test
  void parsesMonsterByName() {
    LocketManager.rememberMonster(1);
    var cleanups = addItem("combat lover's locket");
    try (cleanups) {
      String output = execute("spooky vampire", true);

      assertThat(output, containsString("spooky vampire"));
      assertContinueState();
    }
  }
}
