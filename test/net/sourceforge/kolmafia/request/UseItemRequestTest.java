package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.addItem;
import static internal.helpers.Player.isClass;
import static internal.helpers.Player.setFamiliar;
import static internal.helpers.Player.setProperty;
import static internal.helpers.Player.setupFakeResponse;
import static internal.helpers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UseItemRequestTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset("UseItemRequestTest");
    Preferences.reset("UseItemRequestTest");
  }

  @Nested
  class Milk {
    private UseItemRequest getUseMilkRequest() {
      return UseItemRequest.getInstance(ItemPool.MILK_OF_MAGNESIUM);
    }

    @Test
    void successfulMilkUsageSetsPreferences() {
      var cleanups =
          new Cleanups(
              addItem(ItemPool.MILK_OF_MAGNESIUM),
              setProperty("_milkOfMagnesiumUsed", false),
              setProperty("milkOfMagnesiumActive", false),
              // Wiki claims that this message is indeed "You stomach ..."
              setupFakeResponse(200, "You stomach immediately begins to churn"));

      try (cleanups) {
        var req = getUseMilkRequest();
        req.run();

        assertThat("_milkOfMagnesiumUsed", isSetTo(true));
        assertThat("milkOfMagnesiumActive", isSetTo(true));
      }
    }

    @Test
    void unsuccessfulMilkUsageSetsPreference() {
      var cleanups =
          new Cleanups(
              addItem(ItemPool.MILK_OF_MAGNESIUM),
              setProperty("_milkOfMagnesiumUsed", false),
              setupFakeResponse(200, "it was pretty hard on the old gullet."));
      try (cleanups) {
        UseItemRequest req = getUseMilkRequest();
        req.run();
        assertThat("_milkOfMagnesiumUsed", isSetTo(true));
      }
    }

    @Test
    void milkPreferencePreventsWastedServerHit() {
      var cleanups =
          new Cleanups(
              addItem(ItemPool.MILK_OF_MAGNESIUM), setProperty("_milkOfMagnesiumUsed", true));
      try (cleanups) {
        Preferences.setBoolean("_milkOfMagnesiumUsed", true);

        UseItemRequest req = getUseMilkRequest();
        req.run();

        assertThat("_milkOfMagnesiumUsed", isSetTo(true));
      }
    }
  }

  @Nested
  class GreyYou {
    @Test
    void allConsumablesAreMaxUseOneInGreyYou() {
      var cleanups = new Cleanups(isClass(AscensionClass.GREY_GOO));

      try (cleanups) {
        assertThat(UseItemRequest.maximumUses(ItemPool.GRAPEFRUIT), equalTo(1));
        assertThat(UseItemRequest.maximumUses(ItemPool.COLD_WAD), equalTo(1));
        assertThat(UseItemRequest.maximumUses(ItemPool.MUSCHAT), equalTo(1));
      }
    }

    @Test
    void greyYouGivesWarningOnGcliWhenAlreadyAbsorbed() {
      // Lemon
      var cleanups = new Cleanups(isClass(AscensionClass.GREY_GOO));

      try (cleanups) {
        var req = UseItemRequest.getInstance(332);
        req.responseText = html("request/test_eat_already_absorbed.html");
        req.processResults();

        assertThat(UseItemRequest.lastUpdate, containsString("already absorbed"));
        assertThat(StaticEntity.getContinuationState(), equalTo(KoLConstants.MafiaState.ERROR));
      }

      KoLmafia.forceContinue();
    }

    @Test
    void greyYouGivesNoWarningWhenAbsorbed() {
      // Lemon
      var cleanups = new Cleanups(isClass(AscensionClass.GREY_GOO));

      try (cleanups) {
        var req = UseItemRequest.getInstance(332);
        req.responseText = html("request/test_eat_absorb_lemon.html");
        req.processResults();

        assertThat(UseItemRequest.lastUpdate, equalTo(""));
        assertThat(StaticEntity.getContinuationState(), equalTo(KoLConstants.MafiaState.CONTINUE));
      }
    }
  }

  @Nested
  class PileOfUselessRobotParts {
    @Test
    void incrementsPrefWhenPartsUsed() {
      var cleanups =
          new Cleanups(
              setProperty("homemadeRobotUpgrades", 2), setFamiliar(FamiliarPool.HOMEMADE_ROBOT));

      try (cleanups) {
        var fam = KoLCharacter.getFamiliar();
        fam.setExperience(1);

        assertThat(fam.getWeight(), equalTo(23));

        var req = UseItemRequest.getInstance(ItemPool.PILE_OF_USELESS_ROBOT_PARTS);
        req.responseText = html("request/test_use_pile_of_useless_robot_parts.html");
        req.processResults();

        assertThat("homemadeRobotUpgrades", isSetTo(3));
        assertThat(fam.getWeight(), equalTo(34));
      }
    }

    @Test
    void detectMaxedOutHomemadeRobot() {
      var cleanups =
          new Cleanups(
              setProperty("homemadeRobotUpgrades", 2), setFamiliar(FamiliarPool.HOMEMADE_ROBOT));

      try (cleanups) {
        var fam = KoLCharacter.getFamiliar();
        fam.setExperience(1);

        assertThat(fam.getWeight(), equalTo(23));

        var req = UseItemRequest.getInstance(ItemPool.PILE_OF_USELESS_ROBOT_PARTS);
        req.responseText = html("request/test_use_pile_of_useless_robot_parts_finished.html");
        req.processResults();

        assertThat("homemadeRobotUpgrades", isSetTo(9));
        assertThat(fam.getWeight(), equalTo(100));
      }
    }
  }
}
