package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class KoLCharacterTest {
  @Test
  public void rejectsUsernameWithTwoPeriods() {
    KoLCharacter.reset("test..name");
    // Unset value.
    assertEquals("", KoLCharacter.getUserName());
  }

  @Test
  public void rejectsUsernameWithForwardSlash() {
    KoLCharacter.reset("test/name");
    assertEquals("", KoLCharacter.getUserName());
  }

  @Test
  public void rejectsUsernameWithBackslash() {
    KoLCharacter.reset("test\\name");
    assertEquals("", KoLCharacter.getUserName());
  }

  @Test
  public void acceptsUsernameWithOnlyLetters() {
    KoLCharacter.reset("testname");
    assertEquals("testname", KoLCharacter.getUserName());
  }

  @AfterEach
  void resetUsername() {
    KoLCharacter.reset("");
  }
}
