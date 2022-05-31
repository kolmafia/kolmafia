package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import internal.helpers.RequestLoggerOutput;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PrefTraceCommandTest extends AbstractCommandTestBase {

  public PrefTraceCommandTest() {
    this.command = "ptrace";
  }

  @BeforeEach
  public void setup() {
    KoLCharacter.reset("ptrace");
    Preferences.reset("ptrace");
  }

  @AfterEach
  public void tearDown() {
    execute("");
  }

  @Test
  public void tracesPreferencesChanged() {
    execute("_VYKEACompanionType");

    RequestLoggerOutput.startStream();
    Cleanups cleanups = Player.setProperty("_VYKEACompanionType", "couch");
    try (cleanups) {
      var text = RequestLoggerOutput.stopStream();
      assertThat(text, containsString("ptrace: _VYKEACompanionType = couch"));
    }
  }

  @Test
  public void clearsPreviousItems() {
    execute("_VYKEACompanionType");
    String output = execute("");

    assertThat(output, containsString("Previously watched prefs have been cleared"));

    RequestLoggerOutput.startStream();
    Cleanups cleanups = Player.setProperty("_VYKEACompanionType", "couch");
    try (cleanups) {
      var text = RequestLoggerOutput.stopStream();
      assertThat(text, not(containsString("ptrace")));
    }
  }
}
