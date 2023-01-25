package net.sourceforge.kolmafia;

import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLConstants.ZodiacType;
import net.sourceforge.kolmafia.KoLConstants.ZodiacZone;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class KoLCharacterTest {
  @BeforeEach
  public void init() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("KoLCharacterTest");
    Preferences.reset("KoLCharacterTest");
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

    assertEquals(ZodiacSign.MARMOT, KoLCharacter.getSign());
    assertEquals(6, KoLCharacter.getSignIndex());
    assertEquals(ZodiacType.MOXIE, KoLCharacter.getSignStat());
    assertEquals(ZodiacZone.CANADIA, KoLCharacter.getSignZone());

    KoLCharacter.setSign("Invalid");

    assertEquals(ZodiacSign.NONE, KoLCharacter.getSign());
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
    var cleanups =
        new Cleanups(
            withClass(AscensionClass.ACCORDION_THIEF),
            withEquipped(EquipmentManager.HAT, "brimstone beret"), // Four Songs (mutex)
            withEquipped(EquipmentManager.ACCESSORY1, "plexiglass pendant"), // Four Songs (mutex)
            withEquipped(EquipmentManager.WEAPON, "zombie accordion"), // Additional Song
            withSkill(SkillPool.MARIACHI_MEMORY) // Additional Song
            );

    try (cleanups) {
      KoLCharacter.recalculateAdjustments();
      assertEquals(6, KoLCharacter.getMaxSongs());
    }
  }

  @Test
  public void aboveWaterZonesDoNotCheckUnderwaterNegativeCombat() {
    var cleanups = new Cleanups(withLocation("Noob Cave"), withEffect("Colorfully Concealed"));

    try (cleanups) {
      KoLCharacter.recalculateAdjustments();
      assertEquals(0, KoLCharacter.getCombatRateAdjustment());
    }
  }

  @Test
  public void underwaterZonesCheckUnderwaterNegativeCombat() {
    var cleanups = new Cleanups(withLocation("The Ice Hole"), withEffect("Colorfully Concealed"));

    try (cleanups) {
      KoLCharacter.recalculateAdjustments();
      assertEquals(-5, KoLCharacter.getCombatRateAdjustment());
    }
  }

  @Test
  public void canFindFamiliarByRace() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
            withFamiliarInTerrarium(FamiliarPool.BADGER));

    try (cleanups) {
      var fam = KoLCharacter.usableFamiliar("mosquito");
      assertEquals(FamiliarPool.MOSQUITO, fam.getId());
    }
  }

  @Test
  public void returnsNullIfFamiliarRaceDoesntExist() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
            withFamiliarInTerrarium(FamiliarPool.BADGER));

    try (cleanups) {
      var fam = KoLCharacter.usableFamiliar("non-existent familiar");
      assertNull(fam);
    }
  }

  @Test
  public void canFindFamiliarById() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
            withFamiliarInTerrarium(FamiliarPool.BADGER));

    try (cleanups) {
      var fam = KoLCharacter.usableFamiliar(FamiliarPool.BADGER);
      assertEquals(FamiliarPool.BADGER, fam.getId());
    }
  }

  @Test
  public void returnsNullIfFamiliarIdDoesntExist() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
            withFamiliarInTerrarium(FamiliarPool.BADGER));

    try (cleanups) {
      var fam = KoLCharacter.usableFamiliar(13);
      assertNull(fam);
    }
  }

  @Test
  public void familiarsWithoutGsDoNotExistInGLover() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
            withFamiliarInTerrarium(FamiliarPool.BADGER),
            withPath(AscensionPath.Path.GLOVER));

    try (cleanups) {
      var fam = KoLCharacter.usableFamiliar("mosquito");
      assertNull(fam);
    }
  }

  @Test
  public void familiarsWithGsDoExistInGLover() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
            withFamiliarInTerrarium(FamiliarPool.BADGER),
            withPath(AscensionPath.Path.GLOVER));

    try (cleanups) {
      var fam = KoLCharacter.usableFamiliar("astral badger");
      assertEquals(FamiliarPool.BADGER, fam.getId());
    }
  }

  @Test
  public void familiarsWithoutBsDoExistInBeesHateYou() {
    var cleanups =
        new Cleanups(
            withPath(AscensionPath.Path.BEES_HATE_YOU), withFamiliarInTerrarium(FamiliarPool.MU));

    try (cleanups) {
      var mu = KoLCharacter.usableFamiliar(FamiliarPool.MU);
      assertThat(mu, not(nullValue()));
    }
  }

  @Test
  public void familiarsWithBsDoNotExistInBeesHateYou() {
    var cleanups =
        new Cleanups(
            withPath(AscensionPath.Path.BEES_HATE_YOU),
            withFamiliarInTerrarium(FamiliarPool.CAT_BURGLAR));

    try (cleanups) {
      var mu = KoLCharacter.usableFamiliar(FamiliarPool.CAT_BURGLAR);
      assertThat(mu, nullValue());
    }
  }

  @Test
  public void restrictedFamiliarsDoNotExistInStandard() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
            withFamiliarInTerrarium(FamiliarPool.BADGER),
            withRestricted(true));

    try (cleanups) {
      var request = new StandardRequest();
      request.responseText = "<b>Familiars</b><p><span class=\"i\">Astral Badger</span><p>";
      request.processResults();

      var fam = KoLCharacter.usableFamiliar("astral badger");
      assertNull(fam);
    }
  }

  @Test
  public void unrestrictedFamiliarsDoExistInStandard() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
            withFamiliarInTerrarium(FamiliarPool.BADGER),
            withRestricted(true));

    try (cleanups) {
      var request = new StandardRequest();
      request.responseText = "<b>Familiars</b><p><span class=\"i\">Astral Badger</span><p>";
      request.processResults();

      var fam = KoLCharacter.usableFamiliar("mosquito");
      assertEquals(FamiliarPool.MOSQUITO, fam.getId());
    }
  }

  @Test
  public void familiarsWithoutGsAreStillOwnedInGLover() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.MOSQUITO), withPath(AscensionPath.Path.GLOVER));

    try (cleanups) {
      var fam = KoLCharacter.ownedFamiliar("mosquito");
      assertTrue(fam.isPresent());
    }
  }

  @Test
  public void greyGooHasNoStomach() {
    var cleanups = new Cleanups(withClass(AscensionClass.GREY_GOO));

    try (cleanups) {
      assertThat(KoLCharacter.getFullnessLimit(), equalTo(0));
    }
  }

  @Test
  public void greyGooHasNoLiver() {
    var cleanups = new Cleanups(withClass(AscensionClass.GREY_GOO));

    try (cleanups) {
      assertThat(KoLCharacter.getInebrietyLimit(), equalTo(0));
    }
  }

  @Test
  public void greyGooHasNoSpleen() {
    var cleanups = new Cleanups(withClass(AscensionClass.GREY_GOO));

    try (cleanups) {
      assertThat(KoLCharacter.getSpleenLimit(), equalTo(0));
    }
  }

  @Nested
  class Autumnaton {
    @Test
    public void adventuringWithAutumnatonGivesExperience() {
      var cleanups =
          new Cleanups(
              withTurnsPlayed(1),
              withLocation("The Spooky Forest"),
              withProperty("autumnatonQuestTurn", 5),
              withProperty("autumnatonQuestLocation", "The Spooky Forest"));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        assertThat(KoLCharacter.currentNumericModifier(DoubleModifier.EXPERIENCE), is(1.0));
      }
    }

    @Test
    public void oldQuestDoesNotGiveExperience() {
      var cleanups =
          new Cleanups(
              withTurnsPlayed(6),
              withLocation("The Spooky Forest"),
              withProperty("autumnatonQuestTurn", 2),
              withProperty("autumnatonQuestLocation", "The Spooky Forest"));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        assertThat(KoLCharacter.currentNumericModifier(DoubleModifier.EXPERIENCE), is(0.0));
      }
    }
  }
}
