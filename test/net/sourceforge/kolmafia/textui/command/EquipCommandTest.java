package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EquipCommandTest extends AbstractCommandTestBase {
  public EquipCommandTest() {
    this.command = "equip";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("EquipCommandTest");
  }

  @Test
  public void equipOffhand() {
    HttpClientWrapper.setupFakeClient();
    var cleanups = withEquippableItem(ItemPool.HOT_PLATE);

    try (cleanups) {
      execute("hot plate");
      assertContinueState();

      var requests = getRequests();
      assertThat(requests, hasSize(1));
      assertPostRequest(
          requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=equip&whichitem=4665");
    }
  }

  @Test
  public void equipFolder() {
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(
            withItem(ItemPool.FOLDER_01),
            withEquipped(EquipmentManager.FOLDER1, ItemPool.FOLDER_19),
            withEquipped(EquipmentManager.FOLDER2, ItemPool.FOLDER_22),
            withHandlingChoice(false) // escape the choice
            );

    try (cleanups) {
      execute("folder3 folder (red)");
      assertContinueState();

      var requests = getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(requests.get(0), "/inventory.php", "action=useholder");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=774&option=1&folder=1");
    }
  }
}
