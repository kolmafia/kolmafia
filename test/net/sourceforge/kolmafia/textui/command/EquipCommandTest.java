package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withIntrinsicEffect;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
            withEquipped(Slot.FOLDER1, ItemPool.FOLDER_19),
            withEquipped(Slot.FOLDER2, ItemPool.FOLDER_22),
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

  @Nested
  class professor {
    @ParameterizedTest
    @ValueSource(strings = {"mafia thumb ring", "Treads of Loathing"})
    public void itShouldEquipAccessoryAsRequested(String item) {
      HttpClientWrapper.setupFakeClient();
      AdventureResult itemAR = ItemPool.get(item);
      var cleanups =
          new Cleanups(
              withPath(AscensionPath.Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withItem(itemAR));
      try (cleanups) {
        assertTrue(EquipmentManager.canEquip(itemAR));
        assertTrue(InventoryManager.hasItem(itemAR));
        assertTrue(KoLCharacter.isMildManneredProfessor());
        execute(itemAR.getName());
        assertContinueState();
        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&ajax=1&slot=1&action=equip&whichitem=" + itemAR.getItemId());
      }
    }
  }
}
