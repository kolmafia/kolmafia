package net.sourceforge.kolmafia.request;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
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
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
      try (var _ =
          new Cleanups(
              withItem(ItemPool.MINIATURE_CRYSTAL_BALL),
              withFamiliarInTerrarium(FamiliarPool.BOWLET),
              withPath(Path.BEES_HATE_YOU),
              withNextResponse(200, html("request/test_equip_terrarium_familiar.html")))) {
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
      try (var _ =
          new Cleanups(
              withItem(ItemPool.MINIATURE_CRYSTAL_BALL),
              withFamiliarInTerrarium(FamiliarPool.BOWLET),
              withPath(Path.BEES_HATE_YOU),
              withNextResponse(200, "Item unequipped."))) {
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
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.GROUPIE),
              withEquipped(Slot.FAMILIAR, ItemPool.STILLSUIT),
              withFamiliarInTerrarium(FamiliarPool.BOWLET),
              withFamiliarInTerrarium(FamiliarPool.GREY_GOOSE),
              withProperty("stillsuitFamiliar", "Bowlet"))) {
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
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.GROUPIE),
              withEquipped(Slot.FAMILIAR, ItemPool.STILLSUIT),
              withFamiliarInTerrarium(FamiliarPool.BOWLET),
              withProperty("stillsuitFamiliar", "Bowlet"))) {
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
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.BOWLET),
              withEquipped(Slot.FAMILIAR, ItemPool.STILLSUIT),
              withFamiliarInTerrarium(FamiliarPool.GROUPIE),
              withProperty("stillsuitFamiliar", "Bowlet"))) {
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
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.GROUPIE),
              withEquipped(Slot.FAMILIAR, ItemPool.STILLSUIT),
              withFamiliarInTerrarium(FamiliarPool.BOWLET),
              withProperty("stillsuitFamiliar", "Ian, the familiar of great reknown"))) {
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
      try (var _ = new Cleanups(withNextResponse(200, page))) {
        RequestThread.postRequest(new FamiliarRequest());

        var familiar = KoLCharacter.usableFamiliar(familiarId);
        assertThat(familiar, notNullValue());
        assertThat(familiar.getSoupWeight(), equalTo(soupWeight));
        var attrs = soupAttributes.split(",");
        assertThat(familiar.getSoupAttributes(), hasItems(attrs));
      }
    }
  }
}
