package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withHandlingChoice;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UnequipCommandTest extends AbstractCommandTestBase {
  public UnequipCommandTest() {
    this.command = "unequip";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("UnequipCommandTest");
  }

  @Test
  public void unequipNoneDoesNothing() {
    HttpClientWrapper.setupFakeClient();
    this.execute("none");

    assertContinueState();
    var requests = getRequests();
    assertThat(requests, empty());
  }

  @Test
  public void unequipOffhand() {
    HttpClientWrapper.setupFakeClient();
    var cleanups = withEquipped(EquipmentManager.OFFHAND, ItemPool.HOT_PLATE);

    try (cleanups) {
      execute("offhand");
      assertContinueState();

      var requests = getRequests();
      assertThat(requests, hasSize(1));
      assertPostRequest(
          requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=offhand");
    }
  }

  @Test
  public void unequipFolder() {
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.FOLDER1, ItemPool.FOLDER_19),
            withEquipped(EquipmentManager.FOLDER2, ItemPool.FOLDER_22),
            withHandlingChoice(false) // escape the choice
            );

    try (cleanups) {
      execute("folder2");
      assertContinueState();

      var requests = getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(requests.get(0), "/inventory.php", "action=useholder");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=774&slot=1&option=2");
    }
  }

  @Test
  public void unequipByName() {
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.ACCESSORY1, ItemPool.SHINY_RING),
            withEquipped(EquipmentManager.ACCESSORY2, ItemPool.SHINY_RING),
            withEquipped(EquipmentManager.ACCESSORY3, ItemPool.SHINY_RING));

    try (cleanups) {
      execute("shiny ring");
      assertContinueState();

      var requests = getRequests();
      assertThat(requests, hasSize(3));
      assertPostRequest(
          requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=acc1");
      assertPostRequest(
          requests.get(1), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=acc2");
      assertPostRequest(
          requests.get(2), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=acc3");
    }
  }
}
