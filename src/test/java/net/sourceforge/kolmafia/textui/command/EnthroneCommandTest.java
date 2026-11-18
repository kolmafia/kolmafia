package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.fakeClientBuilder;
import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withBjorned;
import static internal.helpers.Player.withEnthroned;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EnthroneCommandTest extends AbstractCommandTestBase {
  EnthroneCommandTest() {
    this.command = "enthrone";
  }

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("EnthroneCommandTest");
  }

  @BeforeEach
  void initializeState() {
    setupFakeClient();
    KoLCharacter.setFamiliar(FamiliarData.NO_FAMILIAR);
    KoLCharacter.setBjorned(FamiliarData.NO_FAMILIAR);
    KoLCharacter.setEnthroned(FamiliarData.NO_FAMILIAR);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
    ChoiceManager.handlingChoice = false;
    FightRequest.currentRound = 0;
  }

  private void addResponse(String response) {
    fakeClientBuilder.client.addResponse(200, response);
  }

  @Nested
  class ListingAndValidation {
    @Test
    void listsUsableFamiliarsWhenNoParametersGiven() {
      var cleanups = withFamiliarInTerrarium(FamiliarPool.MOSQUITO);

      try (cleanups) {
        String output = execute("");

        assertThat(output, containsString("Mosquito"));
        assertContinueState();
        assertThat(getRequests(), empty());
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "unequip"})
    void doesNothingWhenThroneIsAlreadyEmpty(String parameter) {
      execute(parameter);

      assertContinueState();
      assertThat(getRequests(), empty());
    }

    @Test
    void ignoresNoChangeParameter() {
      var cleanups = withFamiliarInTerrarium(FamiliarPool.MOSQUITO);

      try (cleanups) {
        execute("Mosquito (no change)");

        assertContinueState();
        assertThat(getRequests(), empty());
      }
    }

    @Test
    void errorsOnUnknownFamiliar() {
      String output = execute("nonexistent");

      assertErrorState();
      assertThat(output, containsString("You don't have a nonexistent for a familiar."));
      assertThat(getRequests(), empty());
    }

    @Test
    void errorsOnAmbiguousFamiliar() {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withFamiliarInTerrarium(FamiliarPool.BOWLET));

      try (cleanups) {
        String output = execute("o");

        assertErrorState();
        assertThat(output, containsString("[o] has too many matches."));
        assertThat(getRequests(), empty());
      }
    }

    @Test
    void errorsWhenFamiliarCannotBeEnthroned() {
      var cleanups = withFamiliarInTerrarium(FamiliarPool.BOWLET);

      try (cleanups) {
        String output = execute("Bowlet");

        assertErrorState();
        assertThat(output, containsString("You can't enthrone a Bowlet!"));
        assertThat(getRequests(), empty());
      }
    }

    @Test
    void checkOnlyPrintsFamiliarWithoutRequests() {
      var cleanups = withFamiliarInTerrarium(FamiliarPool.MOSQUITO);

      try (cleanups) {
        String output = execute("mosquito", true);

        assertContinueState();
        assertThat(output, containsString("Mosquito"));
        assertThat(getRequests(), empty());
        assertThat(KoLCharacter.getEnthroned(), equalTo(FamiliarData.NO_FAMILIAR));
      }
    }
  }

  @Nested
  class Enthroning {
    @Test
    void equipsCrownAndEnthronesFamiliar() {
      var cleanups =
          new Cleanups(withItem(ItemPool.HATSEAT), withFamiliarInTerrarium(FamiliarPool.MOSQUITO));

      try (cleanups) {
        addResponse("You equip an item.");
        addResponse("Your Mosquito is now carried in the Crown of Thrones.");

        execute("mosquito");

        assertContinueState();
        var requests = getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&ajax=1&action=equip&whichitem=" + ItemPool.HATSEAT);
        assertPostRequest(
            requests.get(1),
            "/familiar.php",
            "action=hatseat&famid=" + FamiliarPool.MOSQUITO + "&ajax=1");
        assertThat(KoLCharacter.getEnthroned().getId(), equalTo(FamiliarPool.MOSQUITO));
      }
    }

    @Test
    void doesNotEquipCrownWhenItIsAlreadyWorn() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.HAT, ItemPool.HATSEAT),
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO));

      try (cleanups) {
        addResponse("Your Mosquito is now carried in the Crown of Thrones.");

        execute("mosquito");

        assertContinueState();
        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/familiar.php",
            "action=hatseat&famid=" + FamiliarPool.MOSQUITO + "&ajax=1");
        assertThat(KoLCharacter.getEnthroned().getId(), equalTo(FamiliarPool.MOSQUITO));
      }
    }

    @Test
    void putsAwayCurrentFamiliarBeforeEnthroning() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.MOSQUITO),
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withEquipped(Slot.HAT, ItemPool.HATSEAT));

      try (cleanups) {
        addResponse("Your Mosquito goes back in the Terrarium.");
        addResponse("Your Mosquito is now carried in the Crown of Thrones.");

        execute("mosquito");

        assertContinueState();
        var requests = getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/familiar.php", "action=putback&ajax=1");
        assertPostRequest(
            requests.get(1),
            "/familiar.php",
            "action=hatseat&famid=" + FamiliarPool.MOSQUITO + "&ajax=1");
        assertThat(KoLCharacter.getFamiliar(), equalTo(FamiliarData.NO_FAMILIAR));
        assertThat(KoLCharacter.getEnthroned().getId(), equalTo(FamiliarPool.MOSQUITO));
      }
    }

    @Test
    void removesBjornedFamiliarBeforeEnthroning() {
      var cleanups =
          new Cleanups(
              withBjorned(FamiliarPool.MOSQUITO),
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withEquipped(Slot.HAT, ItemPool.HATSEAT),
              withEquipped(Slot.CONTAINER, ItemPool.BUDDY_BJORN));

      try (cleanups) {
        addResponse("Buddy Bjorn vacated.");
        addResponse("Your Mosquito is now carried in the Crown of Thrones.");

        execute("mosquito");

        assertContinueState();
        var requests = getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/familiar.php", "action=backpack&famid=0&ajax=1");
        assertPostRequest(
            requests.get(1),
            "/familiar.php",
            "action=hatseat&famid=" + FamiliarPool.MOSQUITO + "&ajax=1");
        assertThat(KoLCharacter.getBjorned(), equalTo(FamiliarData.NO_FAMILIAR));
        assertThat(KoLCharacter.getEnthroned().getId(), equalTo(FamiliarPool.MOSQUITO));
      }
    }

    @Test
    void doesNothingWhenFamiliarIsAlreadyEnthroned() {
      var cleanups =
          new Cleanups(
              withEnthroned(FamiliarPool.MOSQUITO),
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withEquipped(Slot.HAT, ItemPool.HATSEAT));

      try (cleanups) {
        execute("mosquito");

        assertContinueState();
        assertThat(getRequests(), empty());
      }
    }
  }

  @Nested
  class Clearing {
    @ParameterizedTest
    @ValueSource(strings = {"none", "unequip"})
    void vacatesThrone(String parameter) {
      var cleanups =
          new Cleanups(
              withEnthroned(FamiliarPool.MOSQUITO), withEquipped(Slot.HAT, ItemPool.HATSEAT));

      try (cleanups) {
        addResponse("Crown of Thrones vacated.");

        execute(parameter);

        assertContinueState();
        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/familiar.php", "action=hatseat&famid=0&ajax=1");
        assertThat(KoLCharacter.getEnthroned(), equalTo(FamiliarData.NO_FAMILIAR));
      }
    }
  }
}
