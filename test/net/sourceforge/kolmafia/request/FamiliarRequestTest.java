package net.sourceforge.kolmafia.request;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FamiliarRequestTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("FamiliarRequestTest");
    Preferences.reset("FamiliarRequestTest");
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
              withEquipped(EquipmentManager.FAMILIAR, ItemPool.STILLSUIT),
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
              withEquipped(EquipmentManager.FAMILIAR, ItemPool.STILLSUIT),
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
              withEquipped(EquipmentManager.FAMILIAR, ItemPool.STILLSUIT),
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
              withEquipped(EquipmentManager.FAMILIAR, ItemPool.STILLSUIT),
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
}
