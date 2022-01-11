package net.sourceforge.kolmafia.textui.command;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MoodCommandTest extends AbstractCommandTestBase {

  public MoodCommandTest() {
    this.command = "mood";
  }

  @BeforeEach
  public void initializeState() throws IOException {
    KoLCharacter.reset("moody");
    KoLCharacter.reset(true);
    Preferences.saveSettingsToFile = false;

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public BufferedReader basicMockedReader() throws IOException {
    BufferedReader bufferedReader = Mockito.mock(BufferedReader.class);
    Mockito.when(bufferedReader.readLine())
        .thenReturn(
            "[ apathetic ]",
            "",
            "[ default ]",
            "gain_effect beaten up => uneffect Beaten Up",
            "lose_effect empathy => cast 1 empathy of the newt",
            "lose_effect leash of linguini => cast 1 leash of linguini",
            "lose_effect singer's faithful ocelot => cast 1 singer's faithful ocelot",
            "",
            null);
    return bufferedReader;
  }

  @Test
  public void itShouldClearAllTriggersForCurrentMood() throws IOException {
    Preferences.setString("currentMood", "default");
    MoodManager.loadSettings(basicMockedReader());
    assertEquals(4, MoodManager.getTriggers().size(), "Moods not initialized as expected");
    String output = execute("clear");
    assertEquals(0, MoodManager.getTriggers().size(), "Moods not cleared");
  }
}
