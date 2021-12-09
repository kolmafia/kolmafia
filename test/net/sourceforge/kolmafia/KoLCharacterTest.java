package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.KoLConstants.ZodiacType;
import net.sourceforge.kolmafia.KoLConstants.ZodiacZone;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.Test;

public class KoLCharacterTest {

  @Test
  public void rejectsUsernameWithTwoPeriods() {
    KoLCharacter.reset("");
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
    KoLCharacter.reset("test..name");
    // Unset value.
    assertEquals("", KoLCharacter.getUserName());
  }

  @Test
  public void rejectsUsernameWithForwardSlash() {
    KoLCharacter.reset("");
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
    KoLCharacter.reset("test/name");
    assertEquals("", KoLCharacter.getUserName());
  }

  @Test
  public void rejectsUsernameWithBackslash() {
    KoLCharacter.reset("");
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
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

  @Test
  public void getSongs() {
    KoLConstants.activeEffects.add(EffectPool.get(EffectPool.ODE));
    KoLConstants.activeEffects.add(EffectPool.get(2375)); // Paul's Passionate Pop Song
    KoLConstants.activeEffects.add(EffectPool.get(1495)); // Rolando's Rondo of Resisto
    KoLConstants.activeEffects.add(EffectPool.get(3)); // Confused (i.e. not a song)

    assertEquals(3, KoLCharacter.getSongs());
  }

  private void equip(int slot, String item) {
    EquipmentManager.setEquipment(slot, AdventureResult.parseResult(item));
  }

  @Test
  public void getMaxSongs() {
    KoLCharacter.setAscensionClass(AscensionClass.ACCORDION_THIEF);
    equip(EquipmentManager.HAT, "brimstone beret"); // Four Songs (mutex)
    equip(EquipmentManager.ACCESSORY1, "plexiglass pendant"); // Four Songs (mutex)
    equip(EquipmentManager.WEAPON, "zombie accordion"); // Additional Song
    KoLCharacter.addAvailableSkill(SkillPool.MARIACHI_MEMORY); // Additional Song

    KoLCharacter.recalculateAdjustments();

    assertEquals(6, KoLCharacter.getMaxSongs());
  }
}
