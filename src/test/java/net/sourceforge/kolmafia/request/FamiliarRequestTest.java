package net.sourceforge.kolmafia.request;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withRonin;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class FamiliarRequestTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("FamiliarRequestTest");
    Preferences.reset("FamiliarRequestTest");
  }

  @Nested
  class UnusableFamiliars {
    @Test
    void canEquipUnusableFamiliar() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.MINIATURE_CRYSTAL_BALL),
              withFamiliarInTerrarium(FamiliarPool.BOWLET),
              withPath(Path.BEES_HATE_YOU),
              withNextResponse(200, html("request/test_equip_terrarium_familiar.html")));

      try (cleanups) {
        var bowlet =
            KoLCharacter.ownedFamiliars().stream()
                .filter(x -> x.getId() == FamiliarPool.BOWLET)
                .findFirst()
                .orElseThrow();
        var crystalBall = ItemPool.get(ItemPool.MINIATURE_CRYSTAL_BALL, 1);
        var famRequest = new FamiliarRequest(bowlet, crystalBall);
        famRequest.run();

        var equipped = bowlet.getItem();
        assertThat(equipped, notNullValue());
        assertThat(equipped.getItemId(), equalTo(ItemPool.MINIATURE_CRYSTAL_BALL));
      }
    }

    @Test
    void canUnequipUnusableFamiliar() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.MINIATURE_CRYSTAL_BALL),
              withFamiliarInTerrarium(FamiliarPool.BOWLET),
              withPath(Path.BEES_HATE_YOU),
              withNextResponse(200, "Item unequipped."));

      try (cleanups) {
        var bowlet =
            KoLCharacter.ownedFamiliars().stream()
                .filter(x -> x.getId() == FamiliarPool.BOWLET)
                .findFirst()
                .orElseThrow();
        var crystalBall = ItemPool.get(ItemPool.MINIATURE_CRYSTAL_BALL, 1);
        bowlet.setItem(crystalBall);
        var famRequest = new FamiliarRequest(bowlet, EquipmentRequest.UNEQUIP);
        famRequest.run();

        var equipped = bowlet.getItem();
        assertThat(equipped, notNullValue());
        assertThat(equipped.getItemId(), equalTo(-1));
      }
    }
  }

  @Nested
  class StillsuitFamiliar {
    /**
     * Changing from familiar A (with stillsuit) to familiar B when familiar C is our desired
     * stillsuit familiar
     */
    @Test
    void respectsStillSuitFamiliar() {
      setupFakeClient();
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.GROUPIE),
              withEquipped(Slot.FAMILIAR, ItemPool.STILLSUIT),
              withFamiliarInTerrarium(FamiliarPool.BOWLET),
              withFamiliarInTerrarium(FamiliarPool.GREY_GOOSE),
              withProperty("stillsuitFamiliar", "Bowlet"));

      try (cleanups) {
        var newFamiliar = FamiliarData.registerFamiliar(FamiliarPool.GREY_GOOSE, 0);
        var req = new FamiliarRequest(newFamiliar);
        req.responseText = "You take";
        req.processResults();
        var requests = getRequests();

        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/familiar.php",
            "famid=" + FamiliarPool.GROUPIE + "&action=unequip&ajax=1");
        assertPostRequest(
            requests.get(1),
            "/familiar.php",
            "action=equip&whichfam=" + FamiliarPool.BOWLET + "&whichitem=10932&ajax=1");
      }
    }

    @Test
    void doNotMoveStillsuitIfTakingOutStillsuitFamiliar() {
      setupFakeClient();
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.GROUPIE),
              withEquipped(Slot.FAMILIAR, ItemPool.STILLSUIT),
              withFamiliarInTerrarium(FamiliarPool.BOWLET),
              withProperty("stillsuitFamiliar", "Bowlet"));

      try (cleanups) {
        var newFamiliar = FamiliarData.registerFamiliar(FamiliarPool.BOWLET, 0);
        var req = new FamiliarRequest(newFamiliar);
        req.responseText = "You take";
        req.processResults();
        var requests = getRequests();

        assertThat(requests, hasSize(0));
      }
    }

    @Test
    void doNotMoveStillsuitIfPuttingAwayStillsuitFamiliar() {
      setupFakeClient();
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BOWLET),
              withEquipped(Slot.FAMILIAR, ItemPool.STILLSUIT),
              withFamiliarInTerrarium(FamiliarPool.GROUPIE),
              withProperty("stillsuitFamiliar", "Bowlet"));

      try (cleanups) {
        var newFamiliar = FamiliarData.registerFamiliar(FamiliarPool.GROUPIE, 0);
        var req = new FamiliarRequest(newFamiliar);
        req.responseText = "You take";
        req.processResults();
        var requests = getRequests();

        assertThat(requests, hasSize(0));
      }
    }

    @Test
    void ignoreBogusStillsuitFamiliar() {
      setupFakeClient();
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.GROUPIE),
              withEquipped(Slot.FAMILIAR, ItemPool.STILLSUIT),
              withFamiliarInTerrarium(FamiliarPool.BOWLET),
              withProperty("stillsuitFamiliar", "Ian, the familiar of great reknown"));

      try (cleanups) {
        var newFamiliar = FamiliarData.registerFamiliar(FamiliarPool.BOWLET, 0);
        var req = new FamiliarRequest(newFamiliar);
        req.responseText = "You take";
        req.processResults();
        var requests = getRequests();

        assertThat(requests, hasSize(0));
      }
    }
  }

  @Nested
  class TimeTwitchingTowerSoup {
    @ParameterizedTest
    @CsvSource({
      "288, 1, 'hp'",
      "209, 2, 'hp,act'",
      "154, 3, 'stats,dmg'",
    })
    void canParseSoupedUpFamiliars(int familiarId, int soupWeight, String soupAttributes) {
      var page = html("request/test_familiar_soup.html");
      var cleanups = new Cleanups(withNextResponse(200, page));

      try (cleanups) {
        RequestThread.postRequest(new FamiliarRequest());

        var familiar = KoLCharacter.usableFamiliar(familiarId);
        assertThat(familiar, notNullValue());
        assertThat(familiar.getSoupWeight(), equalTo(soupWeight));
        var attrs = soupAttributes.split(",");
        assertThat(familiar.getSoupAttributes(), hasItems(attrs));
      }
    }
  }

  @Nested
  class ShortOrderCookExperience {
    @ParameterizedTest
    @CsvSource({"'', 1", "11, 11;1", "1;11, 1;11"})
    void tracksBonusMessage(String previous, String expected) {
      // An observed bonus is authoritative even if we do not know about the cook yet.
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withProperty("shortOrderCookFamiliars", previous));

      try (cleanups) {
        FamiliarRequest.parseResponse(
            "familiar.php?action=newfam&newfam=1&ajax=1",
            html("request/test_familiar_short_order_cook_experience.html"));

        assertThat("shortOrderCookFamiliars", isSetTo(expected));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "1, 1",
      "38, ''", // Inflatable Dodecapede
      "120, ''", // Stocking Mimic
      "280, 280", // Each Crimbo Ghost can receive its own bonus
      "281, 281",
      "282, 282"
    })
    void infersPreviousBonusOnlyForEligibleFamiliars(int familiarId, String expected) {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(familiarId),
              withFamiliarInTerrarium(FamiliarPool.SHORT_ORDER_COOK),
              withProperty("shortOrderCookFamiliars", ""));

      try (cleanups) {
        for (int i = 0; i < 2; i++) {
          FamiliarRequest.parseResponse(
              "familiar.php?action=newfam&newfam=" + familiarId,
              "You take your familiar with you.");
        }

        assertThat("shortOrderCookFamiliars", isSetTo(expected));
      }
    }

    @Test
    void doesNotInferBonusWithoutCook() {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withProperty("shortOrderCookFamiliars", ""));

      try (cleanups) {
        FamiliarRequest.parseResponse(
            "familiar.php?action=newfam&newfam=1", "You take your familiar with you.");

        assertThat("shortOrderCookFamiliars", isSetTo(""));
      }
    }

    @Test
    void doesNotInferBonusInStandardRonin() {
      var cleanups =
          new Cleanups(
              withPath(Path.STANDARD),
              withHardcore(false),
              withRonin(true),
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withFamiliarInTerrarium(FamiliarPool.SHORT_ORDER_COOK),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.FAMILIARS, "Shorter-Order Cook"),
              withProperty("shortOrderCookFamiliars", ""));

      try (cleanups) {
        assertThat(
            FamiliarRequest.parseResponse(
                "familiar.php?action=newfam&newfam=1", "You take your familiar with you."),
            equalTo(true));

        assertThat("shortOrderCookFamiliars", isSetTo(""));
      }
    }

    @Test
    void doesNotInferBonusInZombieSlayerRonin() {
      var cleanups =
          new Cleanups(
              withPath(Path.ZOMBIE_SLAYER),
              withHardcore(false),
              withRonin(true),
              withFamiliarInTerrarium(FamiliarPool.SPOOKY_PIRATE_SKELETON),
              withFamiliarInTerrarium(FamiliarPool.SHORT_ORDER_COOK),
              withProperty("shortOrderCookFamiliars", ""));

      try (cleanups) {
        assertThat(
            FamiliarRequest.parseResponse(
                "familiar.php?action=newfam&newfam=" + FamiliarPool.SPOOKY_PIRATE_SKELETON,
                "You take your familiar with you."),
            equalTo(true));

        assertThat("shortOrderCookFamiliars", isSetTo(""));
      }
    }

    @Test
    void doesNotInferBonusWithGraftedCook() {
      var cleanups =
          new Cleanups(
              withPath(Path.Z_IS_FOR_ZOOTOMIST),
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withFamiliarInTerrarium(FamiliarPool.SHORT_ORDER_COOK),
              withProperty("zootGraftedHeadFamiliar", FamiliarPool.SHORT_ORDER_COOK),
              withProperty("shortOrderCookFamiliars", ""));

      try (cleanups) {
        FamiliarRequest.parseResponse(
            "familiar.php?action=newfam&newfam=1", "You take your familiar with you.");

        assertThat("shortOrderCookFamiliars", isSetTo(""));
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "You don't have a familiar of that type in your Terrarium."})
    void doesNotTrackFailedSwitch(String responseText) {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withFamiliarInTerrarium(FamiliarPool.SHORT_ORDER_COOK),
              withProperty("shortOrderCookFamiliars", ""));

      try (cleanups) {
        assertThat(
            FamiliarRequest.parseResponse("familiar.php?action=newfam&newfam=1", responseText),
            equalTo(false));

        assertThat("shortOrderCookFamiliars", isSetTo(""));
      }
    }

    @Test
    void resetsOnAscensionButNotRollover() {
      var cleanups = withProperty("shortOrderCookFamiliars", "1;11");

      try (cleanups) {
        Preferences.resetDailies();
        Preferences.resetPerRollover();
        assertThat("shortOrderCookFamiliars", isSetTo("1;11"));

        Preferences.resetPerAscension();
        assertThat("shortOrderCookFamiliars", isSetTo(""));
      }
    }
  }
}
