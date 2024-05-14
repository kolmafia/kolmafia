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
import java.time.Month;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLConstants.ZodiacType;
import net.sourceforge.kolmafia.KoLConstants.ZodiacZone;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

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
            withEquipped(Slot.HAT, "brimstone beret"), // Four Songs (mutex)
            withEquipped(Slot.ACCESSORY1, "plexiglass pendant"), // Four Songs (mutex)
            withEquipped(Slot.WEAPON, "zombie accordion"), // Additional Song
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
            withPath(Path.GLOVER));

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
            withPath(Path.GLOVER));

    try (cleanups) {
      var fam = KoLCharacter.usableFamiliar("astral badger");
      assertEquals(FamiliarPool.BADGER, fam.getId());
    }
  }

  @Test
  public void familiarsWithoutBsDoExistInBeesHateYou() {
    var cleanups =
        new Cleanups(withPath(Path.BEES_HATE_YOU), withFamiliarInTerrarium(FamiliarPool.MU));

    try (cleanups) {
      var mu = KoLCharacter.usableFamiliar(FamiliarPool.MU);
      assertThat(mu, not(nullValue()));
    }
  }

  @Test
  public void familiarsWithBsDoNotExistInBeesHateYou() {
    var cleanups =
        new Cleanups(
            withPath(Path.BEES_HATE_YOU), withFamiliarInTerrarium(FamiliarPool.CAT_BURGLAR));

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
        new Cleanups(withFamiliarInTerrarium(FamiliarPool.MOSQUITO), withPath(Path.GLOVER));

    try (cleanups) {
      var fam = KoLCharacter.ownedFamiliar("mosquito");
      assertTrue(fam.isPresent());
    }
  }

  @Nested
  class StomachCapacity {
    @Test
    void robotsHaveNoStomachCapacity() {
      var cleanups =
          new Cleanups(
              withProperty("_sweetToothUsed", true),
              withClass(AscensionClass.ACCORDION_THIEF),
              withPath(Path.YOU_ROBOT));
      try (cleanups) {
        assertThat(KoLCharacter.getStomachCapacity(), is(0));
      }
    }

    @Test
    void greyGooHasNoStomachCapacity() {
      var cleanups =
          new Cleanups(
              withSkill(SkillPool.STEEL_STOMACH),
              withClass(AscensionClass.GREY_GOO),
              withPath(Path.GREY_YOU));

      try (cleanups) {
        assertThat(KoLCharacter.getStomachCapacity(), is(0));
      }
    }

    @Test
    void vampyresCannotExpandStomach() {
      var cleanups =
          new Cleanups(withProperty("_pantsgivingFullness", 2), withClass(AscensionClass.VAMPYRE));

      try (cleanups) {
        assertThat(KoLCharacter.getStomachCapacity(), is(5));
      }
    }

    @Test
    void borisHasABigAppetite() {
      var cleanups = new Cleanups(withClass(AscensionClass.AVATAR_OF_BORIS));

      try (cleanups) {
        assertThat(KoLCharacter.getStomachCapacity(), is(20));
      }
    }

    @ParameterizedTest
    @EnumSource(
        value = AscensionClass.class,
        names = {
          "SEAL_CLUBBER",
          "TURTLE_TAMER",
          "SAUCEROR",
          "PASTAMANCER",
          "DISCO_BANDIT",
          "ACCORDION_THIEF"
        })
    void standardClassesHave15Stomach(final AscensionClass ascensionClass) {
      var cleanups = new Cleanups(withPath(Path.NONE), withClass(ascensionClass));

      try (cleanups) {
        assertThat(KoLCharacter.getStomachCapacity(), is(15));
      }
    }

    @Test
    void awolClassesInAftercoreCanExpandStomachButDontFeastWithBoris() {
      var cleanups =
          new Cleanups(
              withDay(2023, Month.APRIL, 19),
              withProperty("_voraciTeaUsed", true),
              withClass(AscensionClass.COW_PUNCHER));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        assertThat(KoLCharacter.getStomachCapacity(), is(11));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void edCanExpandStomachIfHeHasOne(final boolean hasStomach) {
      var cleanups =
          new Cleanups(
              withProperty("_distentionPillUsed", true),
              withClass(AscensionClass.ED),
              withPath(Path.ACTUALLY_ED_THE_UNDYING));

      if (hasStomach) cleanups.add(withSkill(SkillPool.REPLACEMENT_STOMACH));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        assertThat(KoLCharacter.getStomachCapacity(), is(hasStomach ? 6 : 0));
      }
    }

    @ParameterizedTest
    @CsvSource({"0, 5", "1, 8", "2, 11", "3, 14"})
    void wereProfessorStomach(final int wereStomach, final int stomachCapacity) {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.UNKNOWN),
              withPath(Path.WEREPROFESSOR),
              withAdjustmentsRecalculated());

      cleanups.add(withProperty("wereProfessorStomach", wereStomach));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        assertThat(KoLCharacter.getStomachCapacity(), is(stomachCapacity));
      }
    }
  }

  @Nested
  class LiverCapacity {
    @Test
    public void greyGooHasNoLiver() {
      var cleanups =
          new Cleanups(
              withSkill(SkillPool.STEEL_LIVER),
              withClass(AscensionClass.GREY_GOO),
              withPath(Path.GREY_YOU));

      try (cleanups) {
        assertThat(KoLCharacter.getLiverCapacity(), equalTo(0));
      }
    }

    @Test
    void vampyresCannotExpandLiver() {
      // "If you somehow got liver or stomach of steel, those would similarly not work."
      var cleanups =
          new Cleanups(withSkill(SkillPool.STEEL_LIVER), withClass(AscensionClass.VAMPYRE));

      try (cleanups) {
        assertThat(KoLCharacter.getLiverCapacity(), is(4));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void edNeedsLiverToDrink(final boolean hasLiver) {
      var cleanups =
          new Cleanups(withClass(AscensionClass.ED), withPath(Path.ACTUALLY_ED_THE_UNDYING));

      if (hasLiver) cleanups.add(withSkill(SkillPool.REPLACEMENT_LIVER));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        assertThat(KoLCharacter.getLiverCapacity(), is(hasLiver ? 4 : 0));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "false, false, 1, 3",
      "true, false, 4, 7",
      "true, true, 11, 16",
      "false, true, 13, 15"
    })
    void ltaLiverIsComplicated(
        final boolean beltImplantedStill,
        final boolean sobernessInjectionPen,
        final int level,
        final int liverCapacity) {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.SEAL_CLUBBER),
              withPath(Path.LICENSE_TO_ADVENTURE),
              withLevel(level),
              withAdjustmentsRecalculated());

      if (beltImplantedStill) cleanups.add(withProperty("bondDrunk1", true));
      if (sobernessInjectionPen) cleanups.add(withProperty("bondDrunk2", true));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        assertThat(KoLCharacter.getLiverCapacity(), is(liverCapacity));
      }
    }

    @ParameterizedTest
    @CsvSource({"0, 4", "1, 7", "2, 10", "3, 13"})
    void wereProfessorLiver(final int wereLiver, final int liverCapacity) {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.UNKNOWN),
              withPath(Path.WEREPROFESSOR),
              withAdjustmentsRecalculated());

      cleanups.add(withProperty("wereProfessorLiver", wereLiver));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        assertThat(KoLCharacter.getLiverCapacity(), is(liverCapacity));
      }
    }
  }

  @Nested
  class SpleenCapacity {
    @Test
    void greyGooHasNoSpleen() {
      var cleanups = new Cleanups(withClass(AscensionClass.GREY_GOO));

      try (cleanups) {
        assertThat(KoLCharacter.getSpleenLimit(), equalTo(0));
      }
    }

    @Test
    void awolClassesCanSkillTo15Spleen() {
      var cleanups =
          new Cleanups(
              withSkill(SkillPool.TOLERANT_CONSTITUTION), withClass(AscensionClass.BEANSLINGER));

      try (cleanups) {
        assertThat(KoLCharacter.getSpleenLimit(), equalTo(15));
      }
    }

    @Test
    void spleenOfSteel() {
      var cleanups =
          new Cleanups(withSkill(SkillPool.STEEL_SPLEEN), withClass(AscensionClass.TURTLE_TAMER));

      try (cleanups) {
        assertThat(KoLCharacter.getSpleenLimit(), equalTo(20));
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {68, 69})
    void stillBeatingSpleen(final int sbsAscension) {
      var cleanups =
          new Cleanups(
              withProperty("lastStillBeatingSpleen", sbsAscension),
              withAscensions(69),
              withAdjustmentsRecalculated());

      try (cleanups) {
        assertThat(KoLCharacter.getSpleenLimit(), equalTo(sbsAscension == 69 ? 16 : 15));
      }
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

  @Nested
  class Liberation {
    @Test
    void resizesOrganContentsAfterSmallPath() {
      try (var cleanups =
          new Cleanups(
              withProperty("kingLiberated"),
              withPath(Path.SMALL),
              withInebriety(1),
              withFullness(2))) {
        KoLCharacter.liberateKing();
        assertThat(KoLCharacter.getFullness(), is(20));
        assertThat(KoLCharacter.getInebriety(), is(10));
      }
    }

    @Test
    void liberatingKingEnablesStandardRestrictedSkills() {
      try (var cleanups =
          new Cleanups(
              withProperty("kingLiberated"),
              withPath(Path.STANDARD),
              withSkill(SkillPool.DRINKING_TO_DRINK))) {
        assertThat(KoLCharacter.getLiverCapacity(), is(14));
        KoLCharacter.liberateKing();
        assertThat(KoLCharacter.getLiverCapacity(), is(15));
      }
    }
  }

  @Nested
  class RoninBreak {
    @Test
    void breakingRoninEnablesStandardRestrictedSkills() {
      try (var cleanups = new Cleanups(withRonin(true), withSkill(SkillPool.DRINKING_TO_DRINK))) {
        assertThat(KoLCharacter.getLiverCapacity(), is(14));
        KoLCharacter.setRonin(false);
        assertThat(KoLCharacter.getLiverCapacity(), is(15));
      }
    }
  }
}
