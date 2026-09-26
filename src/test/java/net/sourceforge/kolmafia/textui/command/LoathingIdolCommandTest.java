package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class LoathingIdolCommandTest extends AbstractCommandTestBase {
  public LoathingIdolCommandTest() {
    this.command = "loathingidol";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("LoathingIdolCommandTest");
  }

  @Test
  public void warnIfNoMicrophone() {
    String output = execute("pop");
    assertThat(output, containsString("You need a Loathing Idol Microphone first"));
  }

  @Test
  public void warnAgainstUnknownInput() {
    var cleanups = new Cleanups(withItem(ItemPool.LOATHING_IDOL_MICROPHONE_25));

    try (cleanups) {
      String output = execute("metal");

      assertErrorState();
      assertThat(output, containsString("metal is not a valid option"));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {ItemPool.LOATHING_IDOL_MICROPHONE, ItemPool.LOATHING_IDOL_MICROPHONE_50})
  public void usesFoundMicrophone(int mikeId) {
    HttpClientWrapper.setupFakeClient();
    var cleanups = new Cleanups(withItem(mikeId));

    try (cleanups) {
      execute("country");
      assertContinueState();

      var requests = getRequests();
      assertThat(requests, hasSize(greaterThanOrEqualTo(1)));
      assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=" + mikeId + "&ajax=1");
    }
  }
}
