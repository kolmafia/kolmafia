package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class LedCandleCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("LedCandleCommandTest");
    Preferences.reset("LedCandleCommandTest");
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
    ChoiceManager.handlingChoice = false;
  }

  public LedCandleCommandTest() {
    this.command = "ledcandle";
  }

  @Test
  void mustHaveCandle() {
    String output = execute("meat");

    assertErrorState();
    assertThat(output, containsString("You need a LED candle"));
  }

  @Test
  void mustSpecifyUpgrade() {
    var cleanups = withItem(ItemPool.LED_CANDLE);

    try (cleanups) {
      String output = execute("");

      assertErrorState();
      assertThat(output, containsString("Which tweak"));
    }
  }

  @Test
  void mustSpecifyValidUpgrade() {
    var cleanups = withItem(ItemPool.LED_CANDLE);

    try (cleanups) {
      String output = execute("dog");

      assertErrorState();
      assertThat(output, containsString("I don't understand what tweak"));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "disco, 1",
    "item, 1",
    "ultra, 2",
    "meat, 2",
    "reading, 3",
    "stats, 3",
    "red light, 4",
    "attack, 4"
  })
  void canChooseUpgrades(String upgrade, int num) {
    var cleanups = withItem(ItemPool.LED_CANDLE);

    try (cleanups) {
      String output = execute(upgrade);

      assertContinueState();
      assertThat(output, containsString("Tweaking LED Candle"));

      var requests = getRequests();

      assertThat(requests, hasSize(greaterThanOrEqualTo(2)));
      assertGetRequest(requests.get(0), equalTo("/inventory.php"), startsWith("action=tweakjill"));
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1509&option=" + num);
    }
  }

  @Test
  void worksWithEquippedCandle() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.JILL_OF_ALL_TRADES),
            withEquipped(Slot.FAMILIAR, ItemPool.LED_CANDLE));

    try (cleanups) {
      String output = execute("meat");

      assertContinueState();
      assertThat(output, containsString("Tweaking LED Candle"));
    }
  }
}
