package net.sourceforge.kolmafia;

import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLConstants.ZodiacType;
import net.sourceforge.kolmafia.KoLConstants.ZodiacZone;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KoLCharacterTest {
  @BeforeEach
  public void init() {
    KoLCharacter.reset(true);
    StandardRequest.reset();
  }

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

  @Test
  public void aboveWaterZonesDoNotCheckUnderwaterNegativeCombat() {
    inLocation("Noob Cave");
    addEffect("Colorfully Concealed");
    KoLCharacter.recalculateAdjustments();
    assertEquals(0, KoLCharacter.getCombatRateAdjustment());
  }

  @Test
  public void underwaterZonesCheckUnderwaterNegativeCombat() {
    inLocation("The Ice Hole");
    addEffect("Colorfully Concealed");
    KoLCharacter.recalculateAdjustments();
    assertEquals(-5, KoLCharacter.getCombatRateAdjustment());
  }

  @Test
  public void canFindFamiliarByRace() {
    hasFamiliar(FamiliarPool.MOSQUITO);
    hasFamiliar(FamiliarPool.BADGER);

    var fam = KoLCharacter.findFamiliar("mosquito");
    assertEquals(FamiliarPool.MOSQUITO, fam.getId());
  }

  @Test
  public void returnsNullIfFamiliarRaceDoesntExist() {
    hasFamiliar(FamiliarPool.MOSQUITO);
    hasFamiliar(FamiliarPool.BADGER);

    var fam = KoLCharacter.findFamiliar("non-existent familiar");
    assertNull(fam);
  }

  @Test
  public void canFindFamiliarById() {
    hasFamiliar(FamiliarPool.MOSQUITO);
    hasFamiliar(FamiliarPool.BADGER);

    var fam = KoLCharacter.findFamiliar(FamiliarPool.BADGER);
    assertEquals(FamiliarPool.BADGER, fam.getId());
  }

  @Test
  public void returnsNullIfFamiliarIdDoesntExist() {
    hasFamiliar(FamiliarPool.MOSQUITO);
    hasFamiliar(FamiliarPool.BADGER);

    var fam = KoLCharacter.findFamiliar(13);
    assertNull(fam);
  }

  @Test
  public void familiarsWithoutGsDoNotExistInGLover() {
    hasFamiliar(FamiliarPool.MOSQUITO);
    hasFamiliar(FamiliarPool.BADGER);

    KoLCharacter.setPath(AscensionPath.Path.GLOVER);

    var fam = KoLCharacter.findFamiliar("mosquito");
    assertNull(fam);
  }

  @Test
  public void familiarsWithGsDoExistInGLover() {
    hasFamiliar(FamiliarPool.MOSQUITO);
    hasFamiliar(FamiliarPool.BADGER);

    KoLCharacter.setPath(AscensionPath.Path.GLOVER);

    var fam = KoLCharacter.findFamiliar("astral badger");
    assertEquals(FamiliarPool.BADGER, fam.getId());
  }

  @Test
  public void familiarsWithoutBsDoExistInBeesHateYou() {
    var cleanups =
        new Cleanups(inPath(AscensionPath.Path.BEES_HATE_YOU), hasFamiliar(FamiliarPool.MU));

    try (cleanups) {
      var mu = KoLCharacter.findFamiliar(FamiliarPool.MU);
      assertThat(mu, not(nullValue()));
    }
  }

  @Test
  public void familiarsWithBsDoNotExistInBeesHateYou() {
    var cleanups =
        new Cleanups(
            inPath(AscensionPath.Path.BEES_HATE_YOU), hasFamiliar(FamiliarPool.CAT_BURGLAR));

    try (cleanups) {
      var mu = KoLCharacter.findFamiliar(FamiliarPool.CAT_BURGLAR);
      assertThat(mu, nullValue());
    }
  }

  @Test
  public void restrictedFamiliarsDoNotExistInStandard() {
    hasFamiliar(FamiliarPool.MOSQUITO);
    hasFamiliar(FamiliarPool.BADGER);
    KoLCharacter.setRestricted(true);

    var request = new StandardRequest();
    request.responseText = "<b>Familiars</b><p><span class=\"i\">Astral Badger</span><p>";
    request.processResults();

    var fam = KoLCharacter.findFamiliar("astral badger");
    assertNull(fam);
  }

  @Test
  public void unrestrictedFamiliarsDoExistInStandard() {
    hasFamiliar(FamiliarPool.MOSQUITO);
    hasFamiliar(FamiliarPool.BADGER);
    KoLCharacter.setRestricted(true);

    var request = new StandardRequest();
    request.responseText = "<b>Familiars</b><p><span class=\"i\">Astral Badger</span><p>";
    request.processResults();

    var fam = KoLCharacter.findFamiliar("mosquito");
    assertEquals(FamiliarPool.MOSQUITO, fam.getId());
  }
}
