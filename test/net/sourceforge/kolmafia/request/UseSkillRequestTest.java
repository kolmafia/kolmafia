package net.sourceforge.kolmafia.request;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withLevel;
import static internal.helpers.Player.withMP;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UseSkillRequestTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("UseSkillRequestTest");
  }

  @BeforeEach
  void beforeEach() {
    Preferences.reset("UseSkillRequestTest");
  }

  private static int EXPERIENCE_SAFARI = SkillDatabase.getSkillId("Experience Safari");

  @Test
  void errorDoesNotIncrementSkillUses() {
    ContactManager.registerPlayerId("targetPlayer", "123");
    KoLCharacter.setMP(1000, 1000, 1000);
    KoLCharacter.addAvailableSkill(EXPERIENCE_SAFARI);
    int startingCasts = SkillDatabase.getCasts(EXPERIENCE_SAFARI);

    UseSkillRequest req = UseSkillRequest.getInstance(EXPERIENCE_SAFARI, "targetPlayer", 1);

    var cleanups = withNextResponse(200, "You don't have enough mana to cast that skill.");

    try (cleanups) {
      req.run();
    }

    assertEquals("Not enough mana to cast Experience Safari.", UseSkillRequest.lastUpdate);
    assertEquals(startingCasts, SkillDatabase.getCasts(EXPERIENCE_SAFARI));

    KoLmafia.forceContinue();
  }

  @Test
  void successIncrementsSkillUses() {
    ContactManager.registerPlayerId("targetPlayer", "123");
    KoLCharacter.setMP(1000, 1000, 1000);
    KoLCharacter.addAvailableSkill(EXPERIENCE_SAFARI);
    int startingCasts = SkillDatabase.getCasts(EXPERIENCE_SAFARI);

    UseSkillRequest req = UseSkillRequest.getInstance(EXPERIENCE_SAFARI, "targetPlayer", 1);

    var cleanups =
        withNextResponse(
            200,
            "You bless your friend, targetPlayer, with the ability to experience a safari adventure.");

    try (cleanups) {
      req.run();
    }

    assertEquals("", UseSkillRequest.lastUpdate);
    assertEquals(startingCasts + 1, SkillDatabase.getCasts(EXPERIENCE_SAFARI));
  }

  @Test
  void correctErrorMessageForTomeWhenInRun() {
    var cleanups =
        new Cleanups(
            withMP(1000, 1000, 1000),
            withSkill(SkillPool.STICKER),
            withProperty("tomeSummons", 0),
            withProperty("_stickerSummons", 0),
            withInteractivity(false),
            withNextResponse(200, "You may only use three Tome summonings each day"));

    try (cleanups) {
      UseSkillRequest req = UseSkillRequest.getInstance(SkillPool.STICKER);
      req.run();

      assertThat(
          UseSkillRequest.lastUpdate, equalTo("You may only use three Tome summonings each day"));
      assertThat("tomeSummons", isSetTo(3));
      assertThat("_stickerSummons", isSetTo(0));
    }
  }

  @Test
  void correctErrorMessageForTomeWhenOutOfRun() {
    var cleanups =
        new Cleanups(
            withMP(1000, 1000, 1000),
            withSkill(SkillPool.STICKER),
            withProperty("tomeSummons", 0),
            withProperty("_stickerSummons", 0),
            withInteractivity(true),
            withNextResponse(200, "You may only use three Tome summonings each day"));

    try (cleanups) {
      UseSkillRequest req = UseSkillRequest.getInstance(SkillPool.STICKER);
      req.run();

      assertThat(
          UseSkillRequest.lastUpdate, equalTo("You can only cast Summon Stickers 3 times per day"));
      assertThat("tomeSummons", isSetTo(3));
      assertThat("_stickerSummons", isSetTo(3));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Accordion Thief, 15, true",
    "Accordion Thief, 13, false",
    "Sauceror, 15, false",
    "Turtle Tamer, 13, false",
  })
  void canOnlyCastBenettonsInRightState(String className, int level, boolean canCast) {
    var ascensionClass = AscensionClass.find(className);

    var cleanups = new Cleanups(withClass(ascensionClass), withLevel(level));

    try (cleanups) {
      var skill = UseSkillRequest.getInstance(SkillPool.BENETTONS);
      assertThat(skill.getMaximumCast() > 0, equalTo(canCast));
    }
  }

  @Test
  void incrementsUsageForLimitedSkills() {
    var cleanups =
        new Cleanups(
            withMP(1000, 1000, 1000),
            withSkill(SkillPool.DONHOS),
            withProperty("_donhosCasts", 1),
            withClass(AscensionClass.ACCORDION_THIEF),
            withLevel(15),
            withNextResponse(200, html("request/test_cast_donhos_bubbly_ballad.html")));
    try (cleanups) {
      UseSkillRequest req = UseSkillRequest.getInstance(SkillPool.DONHOS, "me", 5);
      req.run();
      assertThat("_donhosCasts", isSetTo(6));
    }
  }

  @Nested
  class DesignerSweatpants {
    @BeforeEach
    public void initializeState() {
      HttpClientWrapper.setupFakeClient();
      KoLCharacter.reset("DesignerSweatpants");
      Preferences.reset("DesignerSweatpants");
    }

    @AfterAll
    public static void afterAll() {
      UseSkillRequest.lastSkillUsed = -1;
      UseSkillRequest.lastSkillCount = 0;
    }

    @Test
    void tooManyCasts() {
      UseSkillRequest.lastSkillUsed = SkillPool.SWEAT_OUT_BOOZE;
      var req = UseSkillRequest.getInstance(SkillPool.SWEAT_OUT_BOOZE);
      req.responseText = html("request/test_runskillz_cant_use_again.html");
      req.processResults();
      assertThat(UseSkillRequest.lastUpdate, containsString("Summon limit exceeded"));
    }

    @Test
    void wearDesignerSweatpantsForCastingSweatSkills() {
      var cleanups = new Cleanups(withEquippableItem("designer sweatpants"));
      InventoryManager.checkDesignerSweatpants();

      try (cleanups) {
        var req = UseSkillRequest.getInstance(SkillPool.DRENCH_YOURSELF_IN_SWEAT, 1);
        req.run();

        var requests = getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=equip&whichitem=10929");
        assertGetRequest(
            requests.get(1), "/runskillz.php", "action=Skillz&whichskill=7419&ajax=1&quantity=1");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void dontWearDesignerSweatpantsForSweatingOutBooze() {
      var cleanups = new Cleanups(withEquippableItem("designer sweatpants"));
      InventoryManager.checkDesignerSweatpants();

      try (cleanups) {
        var req = UseSkillRequest.getInstance(SkillPool.SWEAT_OUT_BOOZE, 1);
        req.run();

        var requests = getRequests();
        assertThat(requests, hasSize(2));
        assertGetRequest(
            requests.get(0), "/runskillz.php", "action=Skillz&whichskill=7414&ajax=1&quantity=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void doNotEquipDesignerSweatpantsForSkillIfAlreadyWearing() {
      var cleanups = new Cleanups(withEquipped(EquipmentManager.PANTS, "designer sweatpants"));
      InventoryManager.checkDesignerSweatpants();

      try (cleanups) {
        var req = UseSkillRequest.getInstance(SkillPool.DRENCH_YOURSELF_IN_SWEAT, 1);
        req.run();

        var requests = getRequests();
        assertThat(requests, hasSize(2));
        assertGetRequest(
            requests.get(0), "/runskillz.php", "action=Skillz&whichskill=7419&ajax=1&quantity=1");
      }
    }

    @Test
    void decreaseSweatWhenCastingSweatBooze() {
      Preferences.setInteger("sweat", 31);
      UseSkillRequest.lastSkillUsed = SkillPool.SWEAT_OUT_BOOZE;
      UseSkillRequest.lastSkillCount = 1;
      UseSkillRequest.parseResponse(
          "runskillz.php?action=Skillz&whichskill=7414&ajax=1&quantity=1",
          html("request/test_cast_sweat_booze.html"));
      // 31 - 25 = 6
      assertEquals(Preferences.getInteger("sweat"), 6);
    }

    @Test
    void decreaseSweatWhenCastingOtherSweatSkills() {
      Preferences.setInteger("sweat", 69);
      UseSkillRequest.lastSkillUsed = SkillPool.DRENCH_YOURSELF_IN_SWEAT;
      UseSkillRequest.lastSkillCount = 1;
      UseSkillRequest.parseResponse(
          "runskillz.php?action=Skillz&whichskill=7419&ajax=1&quantity=1",
          html("request/test_cast_drench_sweat.html"));
      // 69 - 15 = 54
      assertEquals(Preferences.getInteger("sweat"), 54);
    }
  }

  @Nested
  class Numberology {
    @Test
    void calculatingUniverseRequiresAvailableTurns() {
      var cleanups =
          new Cleanups(
              withProperty("skillLevel144", 1),
              withProperty("_universeCalculated", 0),
              withInteractivity(true),
              withAdventuresLeft(0));
      try (cleanups) {
        var skill = UseSkillRequest.getInstance(SkillPool.CALCULATE_THE_UNIVERSE);
        assertEquals(0, skill.getMaximumCast());
      }
    }

    @ParameterizedTest
    @CsvSource({"5, 0", "5, 1", "5, 2", "5, 3", "5, 4", "5, 5"})
    void calculatingUniverseHasDailyLimit(int skillLevel, int casts) {
      var cleanups =
          new Cleanups(
              withProperty("skillLevel144", skillLevel),
              withProperty("_universeCalculated", casts),
              withInteractivity(true),
              withAdventuresLeft(1));
      try (cleanups) {
        var skill = UseSkillRequest.getInstance(SkillPool.CALCULATE_THE_UNIVERSE);
        assertEquals(skillLevel - casts, skill.getMaximumCast());
      }
    }

    @Test
    void calculatingUniverseLimitedInHardcoreOrRonin() {
      var cleanups =
          new Cleanups(
              withProperty("skillLevel144", 5),
              withProperty("_universeCalculated", 0),
              withInteractivity(false),
              withAdventuresLeft(1));
      try (cleanups) {
        var skill = UseSkillRequest.getInstance(SkillPool.CALCULATE_THE_UNIVERSE);
        assertEquals(3, skill.getMaximumCast());
      }
    }
  }
}
