package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class KoLCharacterTest {
  @Test
  public void rejectsUsernameWithPeriod() {
    KoLCharacter.reset("test.name");
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
}
