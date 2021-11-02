package net.sourceforge.kolmafia;

import static net.sourceforge.kolmafia.extensions.ClearSharedState.deleteUserPrefsAndMoodsFiles;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.KoLConstants.ZodiacType;
import net.sourceforge.kolmafia.KoLConstants.ZodiacZone;
import net.sourceforge.kolmafia.preferences.Preferences;
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

  @Test
  public void setAdventuresLeftUpdatesState() {
    // This is a global preference, and should be settable without being logged in.
    Preferences.setBoolean("useDockIconBadge", true);

    assertEquals(0, KoLCharacter.getAdventuresLeft());
    KoLCharacter.setAdventuresLeft(10);

    // Unfortunately there's no easy way to check taskbar badge state, so we're instead relying on
    // this not bailing or raising an exception before it updates its internal state.

    assertEquals(10, KoLCharacter.getAdventuresLeft());

    Preferences.resetToDefault("useDockIconBadge");
  }

  @Test
  public void setSignAssignsValues() {
    KoLCharacter.setSign("Marmot");

    assertEquals("Marmot", KoLCharacter.getSign());
    assertEquals(6, KoLCharacter.getSignIndex());
    assertEquals(ZodiacType.MOXIE, KoLCharacter.getSignStat());
    assertEquals(ZodiacZone.CANADIA, KoLCharacter.getSignZone());

    KoLCharacter.setSign("Invalid");

    assertEquals("None", KoLCharacter.getSign());
    assertEquals(0, KoLCharacter.getSignIndex());
    assertEquals(ZodiacType.NONE, KoLCharacter.getSignStat());
    assertEquals(ZodiacZone.NONE, KoLCharacter.getSignZone());
  }

  @AfterEach
  void resetUsername() {
    deleteUserPrefsAndMoodsFiles(KoLCharacter.baseUserName());
    KoLCharacter.reset("");
  }
}
