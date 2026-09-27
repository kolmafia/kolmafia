package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VampOutManagerTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("vamp out user");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("vamp out user");
  }

  @Test
  public void thatAllThreeDoorsAreFound() {
    String responseText = html("request/test_vamp_out_first_interview.html");

    // Goal 1 is "Mistified", behind Vlad's Boutique
    assertEquals("1", VampOutManager.autoVampOut(1, 0, responseText));
    // Goal 4 is "Muscle", behind Isabella's
    assertEquals("2", VampOutManager.autoVampOut(4, 0, responseText));
    // Goal 13 is "your own black heart", behind The Masquerade
    assertEquals("3", VampOutManager.autoVampOut(13, 0, responseText));

    assertFalse(Preferences.getBoolean("_interviewVlad"));
    assertFalse(Preferences.getBoolean("_interviewIsabella"));
    assertFalse(Preferences.getBoolean("_interviewMasquerade"));
  }

  @Test
  public void thatAVisitedDoorShiftsTheOptionIndex() {
    String responseText = html("request/test_vamp_out_second_interview.html");

    // Vlad's Boutique has been visited, so Isabella's is now the first option
    assertEquals("1", VampOutManager.autoVampOut(4, 0, responseText));
    assertEquals("2", VampOutManager.autoVampOut(13, 0, responseText));
    assertEquals("0", VampOutManager.autoVampOut(1, 0, responseText));

    assertTrue(Preferences.getBoolean("_interviewVlad"));
    assertFalse(Preferences.getBoolean("_interviewIsabella"));
    assertFalse(Preferences.getBoolean("_interviewMasquerade"));
  }

  @Test
  public void thatDoorsAreFoundBehindEscapedApostrophes() {
    String responseText =
        html("request/test_vamp_out_first_interview.html")
            .replace("Vlad's", "Vlad&#039;s")
            .replace("Isabella's", "Isabella&#039;s");

    assertEquals("1", VampOutManager.autoVampOut(1, 0, responseText));
    assertEquals("2", VampOutManager.autoVampOut(4, 0, responseText));
    assertEquals("3", VampOutManager.autoVampOut(13, 0, responseText));

    assertFalse(Preferences.getBoolean("_interviewVlad"));
    assertFalse(Preferences.getBoolean("_interviewIsabella"));
    assertFalse(Preferences.getBoolean("_interviewMasquerade"));
  }
}
