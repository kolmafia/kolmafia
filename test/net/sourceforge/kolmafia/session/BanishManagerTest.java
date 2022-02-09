package net.sourceforge.kolmafia.session;

import static internal.helpers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.BanishManager.Banisher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class BanishManagerTest {
  @BeforeEach
  private void beforeEach() {
    KoLCharacter.reset("BanishManagerTest");
    Preferences.reset("BanishManagerTest");
    BanishManager.clearCache();
  }

  @AfterAll
  private static void cleanup() {
    BanishManager.clearCache();
  }

  private static MonsterData CRATE = MonsterDatabase.findMonster("crate");
  private static MonsterData FLUFFY_BUNNY = MonsterDatabase.findMonster("fluffy bunny");
  private static MonsterData PYGMY_WITCH_LAWYER = MonsterDatabase.findMonster("pygmy witch lawyer");
  private static MonsterData SCARY_PIRATE = MonsterDatabase.findMonster("scary pirate");
  private static MonsterData SMUT_ORC_NAILER = MonsterDatabase.findMonster("smut orc nailer");
  private static MonsterData SPOOKY_MUMMY = MonsterDatabase.findMonster("spooky mummy");
  private static MonsterData SURPRISED_MARIACHI = MonsterDatabase.findMonster("surprised mariachi");
  private static MonsterData TAN_GNAT = MonsterDatabase.findMonster("Tan Gnat");
  private static MonsterData TACO_CAT = MonsterDatabase.findMonster("Taco Cat");
  private static MonsterData MAGICAL_FRUIT_BAT = MonsterDatabase.findMonster("magical fruit bat");

  @Test
  void clearCache() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString("banishedMonsters", "fluffy bunny:Be a Mind Master:119");
    BanishManager.loadBanishedMonsters();

    assertTrue(BanishManager.isBanished("fluffy bunny"));
    BanishManager.clearCache();

    assertFalse(BanishManager.isBanished("fluffy bunny"));
  }

  @Test
  void loadBanishedMonsters() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString(
        "banishedMonsters",
        "gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128");
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

  @Test
  void recalculate() {
    assertThat("banishedMonsters", isSetTo(""));

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

  @Test
  void recalculateSortsNonMatchingPrefs() {
    KoLCharacter.setCurrentRun(420);
    BanishManager.banishMonster(SMUT_ORC_NAILER, Banisher.REFLEX_HAMMER);
    Preferences.setString("banishedMonsters", "crate:snokebomb:69");

    BanishManager.recalculate();

    assertThat("banishedMonsters", isSetTo("smut orc nailer:Reflex Hammer:420"));
  }

  @Test
  void resetRollover() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString(
        "banishedMonsters",
        "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:batter up!:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128:Taco Cat:Bowl a Curveball:124");
    BanishManager.loadBanishedMonsters();

    BanishManager.resetRollover();

    assertThat(
        "banishedMonsters",
        isSetTo(
            "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:whiny survivor:stinky cheese eye:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));
  }

  @Test
  void resetAvatar() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString(
        "banishedMonsters",
        "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128");
    BanishManager.loadBanishedMonsters();

    BanishManager.resetAvatar();

    assertThat(
        "banishedMonsters",
        isSetTo(
            "spooky vampire:ice house:20:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));
  }

  @Test
  void resetAscension() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString(
        "banishedMonsters",
        "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128");
    BanishManager.loadBanishedMonsters();

    BanishManager.resetAscension();

    assertThat("banishedMonsters", isSetTo("spooky vampire:ice house:20"));
  }

  @Test
  public void resetCosmicBowlingBall() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString(
        "banishedMonsters",
        "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:batter up!:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128:Taco Cat:Bowl a Curveball:124");
    BanishManager.loadBanishedMonsters();

    BanishManager.resetCosmicBowlingBall();

    assertThat(
        "banishedMonsters",
        isSetTo(
            "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:batter up!:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));
  }

  @Test
  void banishCurrentMonster() {
    var wimp = MonsterDatabase.findMonster("W imp");
    MonsterStatusTracker.setNextMonster(wimp);
    KoLCharacter.setCurrentRun(123);

    BanishManager.banishCurrentMonster(Banisher.SMOKE_GRENADE);

    assertTrue(BanishManager.isBanished("W imp"));
  }

  @Test
  void banishMonster() {
    KoLCharacter.setCurrentRun(123);

    BanishManager.banishMonster(SPOOKY_MUMMY, Banisher.HUMAN_MUSK);

    assertTrue(BanishManager.isBanished("spooky mummy"));
  }

  @Test
  void banishMonsterDoesNotWorkOnNonExistant() {
    KoLCharacter.setCurrentRun(123);

    BanishManager.banishMonster("nonexistent monster for testing purposes", Banisher.HUMAN_MUSK);

    assertFalse(BanishManager.isBanished("nonexistent monster for testing purposes"));
  }

  @Test
  void banishMonsterDoesNotWorkOnNoBanish() {
    KoLCharacter.setCurrentRun(123);

    BanishManager.banishMonster(SURPRISED_MARIACHI, Banisher.HUMAN_MUSK);

    assertFalse(BanishManager.isBanished("surprised mariachi"));
  }

  @Test
  void banishMonsterCorrectOnTurnCost() {
    KoLCharacter.setCurrentRun(123);

    BanishManager.banishMonster(TAN_GNAT, Banisher.PANTSGIVING);

    KoLCharacter.setCurrentRun(153);
    assertTrue(BanishManager.isBanished("Tan Gnat"));

    KoLCharacter.setCurrentRun(154);
    assertFalse(BanishManager.isBanished("Tan Gnat"));
  }

  @Test
  void banishMonsterAppliesLegacyNanorhino() {
    KoLCharacter.setCurrentRun(123);

    BanishManager.banishMonster(TACO_CAT, Banisher.NANORHINO);

    assertTrue(BanishManager.isBanished("Taco Cat"));
    assertThat("_nanorhinoBanishedMonster", isSetTo(TACO_CAT));
  }

  @ParameterizedTest
  @EnumSource(
      value = Banisher.class,
      names = {"BANISHING_SHOUT", "HOWL_OF_THE_ALPHA"})
  void banishMonsterAppliesLegacyBanishingShout(Banisher banisher) {
    KoLCharacter.setCurrentRun(123);
    Preferences.setString("banishingShoutMonsters", "pygmy bowler|pygmy janitor|pygmy headhunter");

    BanishManager.banishMonster(PYGMY_WITCH_LAWYER, banisher);

    assertTrue(BanishManager.isBanished("pygmy witch lawyer"));
    assertThat("banishingShoutMonsters", isSetTo("pygmy witch lawyer|pygmy bowler|pygmy janitor"));
  }

  @Test
  void banishMonsterAppliesLegacyStaffOfStandaloneCheese() {
    KoLCharacter.setCurrentRun(123);
    Preferences.setString("_jiggleCheesedMonsters", "pygmy bowler|pygmy janitor|pygmy headhunter");

    BanishManager.banishMonster(PYGMY_WITCH_LAWYER, Banisher.STAFF_OF_THE_STANDALONE_CHEESE);

    assertTrue(BanishManager.isBanished("pygmy witch lawyer"));
    assertThat(
        "_jiggleCheesedMonsters",
        isSetTo("pygmy witch lawyer|pygmy bowler|pygmy janitor|pygmy headhunter"));
  }

  @Test
  void respectsQueueSize() {
    KoLCharacter.setCurrentRun(14);
    Preferences.setString(
        "banishedMonsters",
        "crate:banishing shout:5:zmobie:banishing shout:10:sabre-toothed lime:banishing shout:12");
    BanishManager.loadBanishedMonsters();

    BanishManager.banishMonster(SCARY_PIRATE, Banisher.BANISHING_SHOUT);

    assertTrue(BanishManager.isBanished("scary pirate"));
    assertTrue(BanishManager.isBanished("sabre-toothed lime"));
    assertTrue(BanishManager.isBanished("zmobie"));
    assertFalse(BanishManager.isBanished("crate"));
  }

  @Test
  void removeBanishByBanisher() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString(
        "banishedMonsters",
        "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128");
    BanishManager.loadBanishedMonsters();

    BanishManager.removeBanishByBanisher(Banisher.SNOKEBOMB);

    assertThat(
        "banishedMonsters",
        isSetTo(
            "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));
  }

  @Test
  void removeBanishByMonster() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString(
        "banishedMonsters",
        "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128");
    BanishManager.loadBanishedMonsters();

    BanishManager.removeBanishByMonster("crate");

    assertThat(
        "banishedMonsters",
        isSetTo(
            "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128"));
  }

  @Test
  void isBanished() {
    BanishManager.banishMonster(SCARY_PIRATE, Banisher.BEANCANNON);

    assertTrue(BanishManager.isBanished("scary pirate"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void isBanishedDoesNotApplyIceHouseInRestricted(boolean restricted) {
    KoLCharacter.setRestricted(restricted);

    var request = new StandardRequest();
    request.responseText = "<b>Items</b><p><span class=\"i\">ice house</span><p>";
    request.processResults();

    BanishManager.banishMonster(SCARY_PIRATE, Banisher.ICE_HOUSE);

    assertEquals(!restricted, BanishManager.isBanished("scary pirate"));
  }

  @Test
  void getBanishList() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString(
        "banishedMonsters",
        "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128");
    BanishManager.loadBanishedMonsters();

    var list = BanishManager.getBanishList();

    assertEquals(
        "spooky vampire,smut orc nailer,gingerbread lawyer,unhinged survivor,grizzled survivor,cat-alien,alielf,whiny survivor,crate,fluffy bunny,paper towelgeist",
        list);
  }

  @Test
  void getIceHouseMonster() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString(
        "banishedMonsters",
        "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128");
    BanishManager.loadBanishedMonsters();

    var ice = BanishManager.getIceHouseMonster();

    assertEquals("spooky vampire", ice);
  }

  @Test
  void getIceHouseMonsterWorksWhenNoMonsterIsInIceHouse() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString(
        "banishedMonsters",
        "smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:v for vivala mask:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128");
    BanishManager.loadBanishedMonsters();

    var ice = BanishManager.getIceHouseMonster();

    assertEquals(null, ice);
  }

  @Test
  void getIceHouseDataFromMuseumWorksWhenNoCache() throws IOException {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString("banishedMonsters", "");
    BanishManager.loadBanishedMonsters();

    ChoiceManager.handlingChoice = true;
    var request = new GenericRequest("choice.php?forceoption=0");
    request.responseText = Files.readString(Path.of("request/test_museum_ice_house.html"));
    request.processResponse();

    var ice = BanishManager.getIceHouseMonster();
    assertEquals("Perceiver of Sensations", ice);

    ChoiceManager.handlingChoice = false;
    ChoiceManager.lastChoice = 0;
  }

  @Test
  void getBanishData() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setInteger("cosmicBowlingBallReturnCombats", 16);
    Preferences.setString(
        "banishedMonsters",
        "spooky vampire:ice house:20:smut orc nailer:banishing shout:115:gingerbread lawyer:snokebomb:118:unhinged survivor:Feel Hatred:119:grizzled survivor:Reflex Hammer:119:cat-alien:mafia middle finger ring:119:alielf:batter up!:119:whiny survivor:stinky cheese eye:119:crate:louder than bomb:119:fluffy bunny:Be a Mind Master:119:paper towelgeist:divine champagne popper:128:Taco Cat:Bowl a Curveball:124");
    BanishManager.loadBanishedMonsters();

    var data = BanishManager.getBanishData();

    assertThat(data, arrayWithSize(12));
    assertThat(
        data,
        arrayContaining(
            arrayContaining("spooky vampire", "ice house", "20", "Until Ice House opened"),
            arrayContaining("smut orc nailer", "banishing shout", "115", "Until Prism Break"),
            arrayContaining("gingerbread lawyer", "snokebomb", "118", "20 or Until Rollover"),
            arrayContaining("unhinged survivor", "Feel Hatred", "119", "41 or Until Rollover"),
            arrayContaining("grizzled survivor", "Reflex Hammer", "119", "21 or Until Rollover"),
            arrayContaining("cat-alien", "mafia middle finger ring", "119", "51 or Until Rollover"),
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

  @Test
  void getBanishDataWithNoBanishes() {
    KoLCharacter.setCurrentRun(128);
    Preferences.setString("banishedMonsters", "");
    BanishManager.loadBanishedMonsters();

    var data = BanishManager.getBanishData();

    assertEquals(0, data.length);
  }
}
