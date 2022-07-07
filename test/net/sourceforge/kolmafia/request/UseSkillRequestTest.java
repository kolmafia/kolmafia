package net.sourceforge.kolmafia.request;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.canUse;
import static internal.helpers.Player.equip;
import static internal.helpers.Player.setupFakeResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UseSkillRequestTest {

  private static int EXPERIENCE_SAFARI = SkillDatabase.getSkillId("Experience Safari");

  @Test
  void errorDoesNotIncrementSkillUses() {
    ContactManager.registerPlayerId("targetPlayer", "123");
    KoLCharacter.setMP(1000, 1000, 1000);
    KoLCharacter.addAvailableSkill(EXPERIENCE_SAFARI);
    int startingCasts = SkillDatabase.getCasts(EXPERIENCE_SAFARI);

    UseSkillRequest req = UseSkillRequest.getInstance(EXPERIENCE_SAFARI, "targetPlayer", 1);

    var cleanups = setupFakeResponse(200, "You don't have enough mana to cast that skill.");

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
        setupFakeResponse(
            200,
            "You bless your friend, targetPlayer, with the ability to experience a safari adventure.");

    try (cleanups) {
      req.run();
    }

    assertEquals("", UseSkillRequest.lastUpdate);
    assertEquals(startingCasts + 1, SkillDatabase.getCasts(EXPERIENCE_SAFARI));
  }

  @Nested
  class DesignerSweatpants {
    @BeforeEach
    public void initializeState() {
      HttpClientWrapper.setupFakeClient();
      KoLCharacter.reset("DesignerSweatpants");
      Preferences.reset("DesignerSweatpants");
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
      var cleanups = new Cleanups(canUse("designer sweatpants"));
      InventoryManager.checkDesignerSweatpants();

      try (cleanups) {
        var req = UseSkillRequest.getInstance("Drench Yourself in Sweat", 1);
        req.run();

        var requests = getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=equip&whichitem=10929");
        assertGetRequest(
            requests.get(1), "/runskillz.php", "action=Skillz&whichskill=7419&ajax=1&quantity=1");
      }
    }

    @Test
    void doNotEquipDesignerSweatpantsForSkillIfAlreadyWearing() {
      var cleanups = new Cleanups(equip(EquipmentManager.PANTS, "designer sweatpants"));
      InventoryManager.checkDesignerSweatpants();

      try (cleanups) {
        var req = UseSkillRequest.getInstance("Drench Yourself in Sweat", 1);
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
}
