package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withCurrentRun;
import static internal.helpers.Player.withNextMonster;
import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.BanishManager.Banisher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class BanishManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("BanishManagerTest");
    Preferences.reset("BanishManagerTest");
    BanishManager.clearCache();
  }

  @AfterAll
  public static void cleanup() {
    BanishManager.clearCache();
  }

  private static final MonsterData CRATE = MonsterDatabase.findMonster("crate");
  private static final MonsterData PYGMY_WITCH_LAWYER =
      MonsterDatabase.findMonster("pygmy witch lawyer");
  private static final MonsterData SCARY_PIRATE = MonsterDatabase.findMonster("scary pirate");
  private static final MonsterData SMUT_ORC_NAILER = MonsterDatabase.findMonster("smut orc nailer");
  private static final MonsterData SPOOKY_MUMMY = MonsterDatabase.findMonster("spooky mummy");
  private static final MonsterData SURPRISED_MARIACHI =
      MonsterDatabase.findMonster("surprised mariachi");
  private static final MonsterData TAN_GNAT = MonsterDatabase.findMonster("Tan Gnat");
  private static final MonsterData TACO_CAT = MonsterDatabase.findMonster("Taco Cat");
  private static final MonsterData MAGICAL_FRUIT_BAT =
      MonsterDatabase.findMonster("magical fruit bat");

  @Nested
  class ClearCache {
    @Test
    void clearCache() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withProperty("banishedMonsters", "fluffy bunny:Be a Mind Master:119"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        assertTrue(BanishManager.isBanished("fluffy bunny"));
        BanishManager.clearCache();

        assertFalse(BanishManager.isBanished("fluffy bunny"));
      }
    }

    @Test
    void clearCachePhyla() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128), withProperty("banishedPhyla", "beast:Patriotic Screech:119"));

      try (cleanups) {
        BanishManager.loadBanishedPhyla();

        assertTrue(BanishManager.isBanished("fluffy bunny"));
        BanishManager.clearCache();

        assertFalse(BanishManager.isBanished("fluffy bunny"));
      }
    }
  }

  @Nested
  class LoadBanished {

    @Test
    void loadBanishedMonsters() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withProperty(
                  "banishedMonsters",
                  "gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        assertTrue(BanishManager.isBanished("gingerbread lawyer"));
        assertTrue(BanishManager.isBanished("unhinged survivor"));
        assertTrue(BanishManager.isBanished("grizzled survivor"));
        assertTrue(BanishManager.isBanished("cat-alien"));
        assertTrue(BanishManager.isBanished("alielf"));
        assertTrue(BanishManager.isBanished("whiny survivor"));
        assertTrue(BanishManager.isBanished("crate"));
        assertTrue(BanishManager.isBanished("fluffy bunny"));
        assertTrue(BanishManager.isBanished("paper towelgeist"));

        assertFalse(BanishManager.isBanished("zmobie"));
      }
    }

    @Test
    void loadBanishedMonstersSkipsInvalidBanisher() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withProperty(
                  "banishedMonsters",
                  "gingerbread lawyer:made up banisher:118:unhinged survivor:Feel Hatred:119"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        assertFalse(BanishManager.isBanished("gingerbread lawyer"));
        assertTrue(BanishManager.isBanished("unhinged survivor"));
      }
    }
  }

  @Nested
  class Recalculate {

    @Test
    void recalculate() {
      var cleanups = new Cleanups(withCurrentRun(), withProperty("banishedMonsters", ""));

      try (cleanups) {
        // This will be removed because it's run out.
        KoLCharacter.setCurrentRun(69);
        BanishManager.banishMonster(CRATE, Banisher.SNOKEBOMB);

        KoLCharacter.setCurrentRun(419);
        BanishManager.banishMonster(SMUT_ORC_NAILER, Banisher.REFLEX_HAMMER);

        KoLCharacter.setCurrentRun(420);
        BanishManager.banishMonster(MAGICAL_FRUIT_BAT, Banisher.FEEL_HATRED);

        BanishManager.recalculate();

        assertThat(
            "banishedMonsters",
            isSetTo("smut orc nailer:Reflex Hammer:419:magical fruit bat:Feel Hatred:420"));
      }
    }

    @Test
    void recalculateSortsNonMatchingPrefs() {
      var cleanups =
          new Cleanups(withCurrentRun(420), withProperty("banishedMonsters", "crate:snokebomb:69"));

      try (cleanups) {
        BanishManager.banishMonster(SMUT_ORC_NAILER, Banisher.REFLEX_HAMMER);
        BanishManager.recalculate();

        assertThat("banishedMonsters", isSetTo("smut orc nailer:Reflex Hammer:420"));
      }
    }
  }

  @Nested
  class Reset {

    @Test
    void resetRollover() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withProperty(
                  "banishedMonsters",
                  "spooky vampire:ice house:0:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:batter up!:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128:Taco Cat:Bowl a Curveball:124"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        BanishManager.resetRollover();

        assertThat(
            "banishedMonsters",
            isSetTo(
                "spooky vampire:ice house:0:smut orc nailer:banishing shout:115:whiny survivor:stinky cheese eye:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));
      }
    }

    @Test
    void resetAvatar() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withProperty(
                  "banishedMonsters",
                  "spooky vampire:ice house:0:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        BanishManager.resetAvatar();

        assertThat(
            "banishedMonsters",
            isSetTo(
                "spooky vampire:ice house:0:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));
      }
    }

    @Test
    void resetAscension() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withProperty(
                  "banishedMonsters",
                  "spooky vampire:ice house:0:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        BanishManager.resetAscension();

        assertThat("banishedMonsters", isSetTo("spooky vampire:ice house:0"));
      }
    }

    @Test
    public void resetCosmicBowlingBall() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withProperty(
                  "banishedMonsters",
                  "spooky vampire:ice house:0:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:batter up!:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128:Taco Cat:Bowl a Curveball:124"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        BanishManager.resetCosmicBowlingBall();

        assertThat(
            "banishedMonsters",
            isSetTo(
                "spooky vampire:ice house:0:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:batter up!:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));
      }
    }
  }

  @Nested
  class BanishMonster {
    @Test
    void banishCurrentMonster() {
      var cleanups =
          new Cleanups(
              withCurrentRun(123), withProperty("banishedMonsters"), withNextMonster("W imp"));

      try (cleanups) {
        BanishManager.banishCurrentMonster(Banisher.SMOKE_GRENADE);
        assertTrue(BanishManager.isBanished("W imp"));
      }
    }

    @Test
    void banishCurrentMonsterWithNoCurrentMonster() {
      var cleanups =
          new Cleanups(
              withCurrentRun(123),
              withProperty("banishedMonsters", "spooky vampire:ice house:0"),
              withNextMonster((MonsterData) null));

      try (cleanups) {
        BanishManager.banishCurrentMonster(Banisher.SMOKE_GRENADE);

        // Still well-formed
        assertThat("banishedMonsters", isSetTo("spooky vampire:ice house:0"));
      }
    }

    @Test
    void banishMonster() {
      var cleanups = new Cleanups(withCurrentRun(123), withProperty("banishedMonsters"));

      try (cleanups) {
        BanishManager.banishMonster(SPOOKY_MUMMY, Banisher.HUMAN_MUSK);

        assertTrue(BanishManager.isBanished("spooky mummy"));
      }
    }

    @Test
    void banishMonsterWorksOnRebanish() {
      var cleanups = new Cleanups(withProperty("banishedMonsters"));

      try (cleanups) {
        BanishManager.banishMonster(SPOOKY_MUMMY, Banisher.ICE_HOUSE);
        assertThat(BanishManager.getFirstBanished(Banisher.ICE_HOUSE), equalTo("spooky mummy"));

        BanishManager.banishMonster(SPOOKY_MUMMY, Banisher.ICE_HOUSE);

        assertThat(BanishManager.getBanished(Banisher.ICE_HOUSE), hasSize(1));
        assertThat(BanishManager.getFirstBanished(Banisher.ICE_HOUSE), equalTo("spooky mummy"));
      }
    }

    @Test
    void oneExpiringBanishLeavesTheOther() {
      var cleanups = new Cleanups(withCurrentRun(), withProperty("banishedMonsters"));

      try (cleanups) {
        KoLCharacter.setCurrentRun(100);
        BanishManager.banishMonster(SPOOKY_MUMMY, Banisher.SPRING_LOADED_FRONT_BUMPER);
        KoLCharacter.setCurrentRun(105);
        BanishManager.banishMonster(SPOOKY_MUMMY, Banisher.STINKY_CHEESE_EYE);
        KoLCharacter.setCurrentRun(120);
        var data = BanishManager.getBanishedMonsterData();
        assertThat(data, arrayWithSize(1));
        assertThat(
            data,
            arrayContaining(
                arrayContaining(
                    "spooky mummy", "Spring-Loaded Front Bumper", "100", "10 or Until Rollover")));
      }
    }

    @Test
    void oneOverwritingBanishLeavesTheOther() {
      var cleanups = new Cleanups(withCurrentRun(), withProperty("banishedMonsters"));

      try (cleanups) {
        KoLCharacter.setCurrentRun(100);
        BanishManager.banishMonster(SPOOKY_MUMMY, Banisher.SPRING_LOADED_FRONT_BUMPER);
        KoLCharacter.setCurrentRun(105);
        BanishManager.banishMonster(SPOOKY_MUMMY, Banisher.STINKY_CHEESE_EYE);
        KoLCharacter.setCurrentRun(106);
        BanishManager.banishMonster(SPOOKY_MUMMY, Banisher.STINKY_CHEESE_EYE);
        var data = BanishManager.getBanishedMonsterData();
        assertThat(data, arrayWithSize(2));
        assertThat(
            data,
            arrayContaining(
                arrayContaining(
                    "spooky mummy", "Spring-Loaded Front Bumper", "100", "24 or Until Rollover"),
                arrayContaining("spooky mummy", "stinky cheese eye", "106", "10")));
      }
    }

    @Test
    void oneOverwritingUnrelatedBanishLeavesTheOther() {
      var cleanups = new Cleanups(withCurrentRun(), withProperty("banishedMonsters"));

      try (cleanups) {
        KoLCharacter.setCurrentRun(100);
        BanishManager.banishMonster(SPOOKY_MUMMY, Banisher.SPRING_LOADED_FRONT_BUMPER);
        KoLCharacter.setCurrentRun(105);
        BanishManager.banishMonster(SPOOKY_MUMMY, Banisher.STINKY_CHEESE_EYE);
        KoLCharacter.setCurrentRun(106);
        BanishManager.banishMonster(SCARY_PIRATE, Banisher.STINKY_CHEESE_EYE);
        var data = BanishManager.getBanishedMonsterData();
        assertThat(data, arrayWithSize(2));
        assertThat(
            data,
            arrayContaining(
                arrayContaining(
                    "spooky mummy", "Spring-Loaded Front Bumper", "100", "24 or Until Rollover"),
                arrayContaining("scary pirate", "stinky cheese eye", "106", "10")));
      }
    }

    @Test
    void regressionTestForSpringLoadedFrontBumperBeingWiped() {
      var cleanups =
          new Cleanups(
              withCurrentRun(176),
              withProperty(
                  "banishedMonsters",
                  "biker:ice house:0:pygmy janitor:snokebomb:161:pygmy headhunter:Bowl a Curveball:161:pygmy witch accountant:Throw Latte on Opponent:165:pygmy witch accountant:Spring-Loaded Front Bumper:172:coaltergeist:KGB tranquilizer dart:176"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();
        BanishManager.banishMonster("steam elemental", Banisher.THROW_LATTE_ON_OPPONENT, false);
        assertThat(
            Preferences.getString("banishedMonsters"),
            equalTo(
                "biker:ice house:0:pygmy janitor:snokebomb:161:pygmy witch accountant:Spring-Loaded Front Bumper:172:coaltergeist:KGB tranquilizer dart:176:steam elemental:Throw Latte on Opponent:176"));
      }
    }

    @Test
    void banishMonsterDoesNotWorkOnNonExistant() {
      var cleanups = new Cleanups(withCurrentRun(123), withProperty("banishedMonsters"));

      try (cleanups) {
        BanishManager.banishMonster(
            "nonexistent monster for testing purposes", Banisher.HUMAN_MUSK, true);

        assertFalse(BanishManager.isBanished("nonexistent monster for testing purposes"));
      }
    }

    @Test
    void banishMonsterDoesNotWorkOnNoBanish() {
      var cleanups = new Cleanups(withCurrentRun(123), withProperty("banishedMonsters"));

      try (cleanups) {
        BanishManager.banishMonster(SURPRISED_MARIACHI, Banisher.HUMAN_MUSK);

        assertFalse(BanishManager.isBanished("surprised mariachi"));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "153, true",
      "154, false",
    })
    void banishMonsterCorrectOnTurnCost(final int turns, final boolean banished) {
      var cleanups = new Cleanups(withCurrentRun(123), withProperty("banishedMonsters"));

      try (cleanups) {
        BanishManager.banishMonster(TAN_GNAT, Banisher.PANTSGIVING);

        KoLCharacter.setCurrentRun(turns);
        assertThat(BanishManager.isBanished("Tan Gnat"), equalTo(banished));
      }
    }

    @Test
    void banishMonsterAppliesLegacyNanorhino() {
      var cleanups =
          new Cleanups(
              withCurrentRun(123),
              withProperty("banishedMonsters"),
              withProperty("_nanorhinoBanishedMonster"));

      try (cleanups) {
        BanishManager.banishMonster(TACO_CAT, Banisher.NANORHINO);

        assertTrue(BanishManager.isBanished("Taco Cat"));
        assertThat("_nanorhinoBanishedMonster", isSetTo(TACO_CAT));
      }
    }

    @ParameterizedTest
    @EnumSource(
        value = Banisher.class,
        names = {"BANISHING_SHOUT", "HOWL_OF_THE_ALPHA"})
    void banishMonsterAppliesLegacyBanishingShout(Banisher banisher) {
      var cleanups =
          new Cleanups(
              withCurrentRun(123),
              withProperty("banishedMonsters"),
              withProperty(
                  "banishingShoutMonsters", "pygmy bowler|pygmy janitor|pygmy headhunter"));

      try (cleanups) {
        BanishManager.banishMonster(PYGMY_WITCH_LAWYER, banisher);

        assertTrue(BanishManager.isBanished("pygmy witch lawyer"));
        assertThat(
            "banishingShoutMonsters", isSetTo("pygmy witch lawyer|pygmy bowler|pygmy janitor"));
      }
    }

    @Test
    void banishMonsterAppliesLegacyStaffOfStandaloneCheese() {
      var cleanups =
          new Cleanups(
              withCurrentRun(123),
              withProperty("banishedMonsters"),
              withProperty(
                  "_jiggleCheesedMonsters", "pygmy bowler|pygmy janitor|pygmy headhunter"));

      try (cleanups) {
        BanishManager.banishMonster(PYGMY_WITCH_LAWYER, Banisher.STAFF_OF_THE_STANDALONE_CHEESE);

        assertTrue(BanishManager.isBanished("pygmy witch lawyer"));
        assertThat(
            "_jiggleCheesedMonsters",
            isSetTo("pygmy witch lawyer|pygmy bowler|pygmy janitor|pygmy headhunter"));
      }
    }

    @Test
    void respectsQueueSize() {
      var cleanups =
          new Cleanups(
              withCurrentRun(14),
              withProperty(
                  "banishedMonsters",
                  "crate:banishing shout:5:zmobie:banishing shout:10:sabre-toothed lime:banishing shout:12"),
              withProperty("banishingShoutMonsters"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        BanishManager.banishMonster(SCARY_PIRATE, Banisher.BANISHING_SHOUT);

        assertTrue(BanishManager.isBanished("scary pirate"));
        assertTrue(BanishManager.isBanished("sabre-toothed lime"));
        assertTrue(BanishManager.isBanished("zmobie"));
        assertFalse(BanishManager.isBanished("crate"));
      }
    }

    @Test
    void poppingFromQueueDoesNotResetUnrelatedBanish() {
      var cleanups =
          new Cleanups(
              withCurrentRun(14),
              withProperty(
                  "banishedMonsters",
                  "crate:banishing shout:5:zmobie:banishing shout:10:sabre-toothed lime:banishing shout:12:crate:snokebomb:9"),
              withProperty("banishingShoutMonsters"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        BanishManager.banishMonster(SCARY_PIRATE, Banisher.BANISHING_SHOUT);

        assertTrue(BanishManager.isBanished("scary pirate"));
        assertTrue(BanishManager.isBanished("sabre-toothed lime"));
        assertTrue(BanishManager.isBanished("zmobie"));
        assertTrue(BanishManager.isBanished("crate"));
      }
    }

    @Test
    void canBanishPhylaFromNoBanishZone() {
      var cleanups = new Cleanups(withCurrentRun(1), withProperty("banishedPhyla"));

      try (cleanups) {
        BanishManager.loadBanishedPhyla();

        BanishManager.banishMonster("angry tourist", Banisher.PATRIOTIC_SCREECH, true);

        assertTrue(BanishManager.isBanished("Creepy Ginger Twin"));
      }
    }
  }

  @Test
  void removeBanishByBanisher() {
    var cleanups =
        new Cleanups(
            withCurrentRun(128),
            withProperty(
                "banishedMonsters",
                "spooky vampire:ice house:0:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));

    try (cleanups) {
      BanishManager.loadBanishedMonsters();

      BanishManager.removeBanishByBanisher(Banisher.SNOKEBOMB);

      assertThat(
          "banishedMonsters",
          isSetTo(
              "spooky vampire:ice house:0:smut orc nailer:banishing shout:115:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));
    }
  }

  @Nested
  class IsBanished {
    @Test
    void isBanished() {
      var cleanups = new Cleanups(withProperty("banishedMonsters"));

      try (cleanups) {
        BanishManager.banishMonster(SCARY_PIRATE, Banisher.BEANCANNON);

        assertTrue(BanishManager.isBanished("scary pirate"));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isBanishedDoesNotApplyIceHouseInRestricted(boolean restricted) {
      var cleanups =
          new Cleanups(
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "ice house"),
              withRestricted(restricted));

      try (cleanups) {
        BanishManager.banishMonster(SCARY_PIRATE, Banisher.ICE_HOUSE);
        assertEquals(!restricted, BanishManager.isBanished("scary pirate"));
      }
    }

    @Test
    void phylaBanishedMeansIsBanished() {
      var cleanups = new Cleanups(withProperty("banishedPhyla"));

      try (cleanups) {
        BanishManager.banishMonster(MAGICAL_FRUIT_BAT, Banisher.PATRIOTIC_SCREECH);

        assertTrue(BanishManager.isBanished("taco cat"));
      }
    }

    @Test
    void phylaBanishedDoesNotBanishInNoBanishZones() {
      var cleanups = new Cleanups(withProperty("banishedPhyla"));

      try (cleanups) {
        BanishManager.banishMonster("pygmy bowler", Banisher.PATRIOTIC_SCREECH, true);

        assertFalse(BanishManager.isBanished("angry tourist"));
      }
    }
  }

  @Nested
  class GetBanished {
    @Test
    void getBanishList() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withProperty(
                  "banishedMonsters",
                  "spooky vampire:ice house:0:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        var list = BanishManager.getBanishedMonsters();

        assertThat(
            list,
            containsInAnyOrder(
                equalTo("spooky vampire"),
                equalTo("smut orc nailer"),
                equalTo("gingerbread lawyer"),
                equalTo("unhinged survivor"),
                equalTo("grizzled survivor"),
                equalTo("cat-alien"),
                equalTo("alielf"),
                equalTo("whiny survivor"),
                equalTo("crate"),
                equalTo("fluffy bunny"),
                equalTo("paper towelgeist")));
      }
    }

    @Test
    void getBanishedPhyla() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128), withProperty("banishedPhyla", "fish:Patriotic Screech:30"));
      try (cleanups) {
        BanishManager.loadBanishedPhyla();

        var list = BanishManager.getBanishedPhyla();

        assertThat(list, containsInAnyOrder(equalTo("fish")));
      }
    }
  }

  @Nested
  class GetFirstBanished {
    @Test
    void getIceHouseMonster() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withProperty(
                  "banishedMonsters",
                  "spooky vampire:ice house:0:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();
        var ice = BanishManager.getFirstBanished(Banisher.ICE_HOUSE);

        assertEquals("spooky vampire", ice);
      }
    }

    @Test
    void getIceHouseMonsterWorksWhenNoMonsterIsInIceHouse() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withProperty(
                  "banishedMonsters",
                  "smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();
        var ice = BanishManager.getFirstBanished(Banisher.ICE_HOUSE);

        assertNull(ice);
      }
    }

    @Test
    void canDiscoverIceHouseMonsterFromNoncombat() {
      var cleanups = new Cleanups(withCurrentRun(128), withProperty("banishedMonsters", ""));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        var request = new GenericRequest("choice.php?forceoption=0");
        request.responseText = html("request/test_museum_ice_house.html");
        ChoiceManager.visitChoice(request);

        var ice = BanishManager.getFirstBanished(Banisher.ICE_HOUSE);
        assertEquals("Perceiver of Sensations", ice);
      }
    }
  }

  @Nested
  class Banishes {
    @Test
    void getBanishData() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withProperty(
                  "banishedMonsters",
                  "spooky vampire:ice house:0:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:batter up!:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128:Taco Cat:Bowl a Curveball:124"),
              withProperty("cosmicBowlingBallReturnCombats", 16));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        var data = BanishManager.getBanishedMonsterData();

        assertThat(data, arrayWithSize(12));
        assertThat(
            data,
            arrayContaining(
                arrayContaining("spooky vampire", "ice house", "0", "Until Ice House opened"),
                arrayContaining("smut orc nailer", "banishing shout", "115", "Until Prism Break"),
                arrayContaining("gingerbread lawyer", "snokebomb", "118", "20 or Until Rollover"),
                arrayContaining("unhinged survivor", "Feel Hatred", "119", "41 or Until Rollover"),
                arrayContaining(
                    "grizzled survivor", "Reflex Hammer", "119", "21 or Until Rollover"),
                arrayContaining(
                    "cat-alien", "mafia middle finger ring", "119", "51 or Until Rollover"),
                arrayContaining("alielf", "batter up!", "119", "Until Rollover"),
                arrayContaining("whiny survivor", "stinky cheese eye", "119", "1"),
                arrayContaining("crate", "louder than bomb", "119", "11 or Until Rollover"),
                arrayContaining("fluffy bunny", "Be a Mind Master", "119", "71"),
                arrayContaining("paper towelgeist", "divine champagne popper", "128", "5"),
                arrayContaining(
                    "Taco Cat",
                    "Bowl a Curveball",
                    "124",
                    "Until Ball returns (16 combats) or Until Rollover")));
      }
    }

    @Test
    void getBanishedPhyla() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128), withProperty("banishedPhyla", "undead:Patriotic Screech:119"));

      try (cleanups) {
        BanishManager.loadBanishedPhyla();

        var data = BanishManager.getBanishedPhylaData();

        assertThat(data, arrayWithSize(1));
        assertThat(
            data, arrayContaining(arrayContaining("undead", "Patriotic Screech", "119", "90")));
      }
    }

    @Test
    void getBanishDataWithNoBanishes() {
      var cleanups = new Cleanups(withCurrentRun(128), withProperty("banishedMonsters", ""));

      try (cleanups) {
        BanishManager.loadBanishedMonsters();

        var data = BanishManager.getBanishedMonsterData();

        assertEquals(0, data.length);
      }
    }

    @Test
    void getBanishPhylaDataWithNoBanishes() {
      var cleanups = new Cleanups(withCurrentRun(128), withProperty("banishedPhyla", ""));

      try (cleanups) {
        BanishManager.loadBanishedPhyla();

        var data = BanishManager.getBanishedPhylaData();

        assertEquals(0, data.length);
      }
    }
  }
}
