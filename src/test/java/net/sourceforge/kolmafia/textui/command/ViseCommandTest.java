package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ViseCommandTest extends AbstractCommandTestBase {

  public ViseCommandTest() {
    this.command = "vise";
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
  }

  static AdventureResult HASHING_VISE = ItemPool.get(ItemPool.HASHING_VISE);
  static AdventureResult CYBEER_SCHEMATIC =
      new AdventureResult("dedigitizer schematic: cybeer", 1, false);
  static AdventureResult CYBURGER_SCHEMATIC =
      new AdventureResult("dedigitizer schematic: cyburger", 1, false);
  static AdventureResult ONE = ItemPool.get(ItemPool.ONE);
  static AdventureResult ZERO = ItemPool.get(ItemPool.ZERO);

  @Test
  void failsIfNoHashingVise() {
    String output = execute("");

    assertErrorState();
    assertThat(output, containsString("You don't have an available hashing vise."));
  }

  @Test
  void providesUsageIfNoParameters() {
    var cleanups = withItem(ItemPool.HASHING_VISE);

    try (cleanups) {
      String output = execute("");
      assertThat(
          output,
          containsString(
              "Usage: vise [count] <item> [, <another>]... - use your hashing vise to smash schematics into bits."));
    }
  }

  @Test
  void failsWithBogusSchematic() {
    var cleanups = withItem(ItemPool.HASHING_VISE);

    try (cleanups) {
      String output = execute("fish");

      assertErrorState();
      assertThat(output, containsString("'fish' matches no schematics"));
    }
  }

  @Test
  void failsWithAmbiguousSchematic() {
    var cleanups = withItem(ItemPool.HASHING_VISE);

    try (cleanups) {
      String output = execute("digit");

      assertErrorState();
      assertThat(output, containsString("'digit' matches 22 schematics"));
    }
  }

  @Test
  void doesNothingWithoutSchematics() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    String visitResult = html("request/test_choice_hashing_vise.html");
    String hashResult = html("request/test_choice_hashing_vise_result.html");

    var cleanups = new Cleanups(withItem(HASHING_VISE), withHttpClientBuilder(builder));

    try (cleanups) {
      String output = execute("2 beer");

      // (hashable quantity of dedigitizer schematic: cybeer is limited to 0 by availability in
      // inventory)

      assertThat(
          output,
          containsString(
              "(hashable quantity of dedigitizer schematic: cybeer is limited to 0 by availability in inventory)"));

      var requests = client.getRequests();
      assertThat(requests, hasSize(0));
    }
  }

  @Test
  void canHashMultipleSchematics() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    String visitResult = html("request/test_choice_hashing_vise.html");
    String hashResult = html("request/test_choice_hashing_vise_result.html");

    var cleanups =
        new Cleanups(
            withItem(HASHING_VISE),
            withItem(CYBEER_SCHEMATIC.getInstance(1)),
            withItem(CYBURGER_SCHEMATIC.getInstance(2)),
            withItem(ONE.getInstance(0)),
            withItem(ZERO.getInstance(0)),
            withHttpClientBuilder(builder));

    try (cleanups) {
      // Using the item redirects to choice.php
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, visitResult);
      // Within the choice, you can hash multiple items in succession
      client.addResponse(200, hashResult);
      client.addResponse(200, hashResult);

      String output = execute("2 beer, burger");

      // (hashable quantity of dedigitizer schematic: cybeer is limited to 1 by availability in
      // inventory)
      // vise dedigitizer schematic: cybeer
      // You acquire 0 (2)
      // You acquire 1 (14)
      // vise dedigitizer schematic: cyburger
      // You acquire 0 (2)
      // You acquire 1 (14)

      assertThat(
          output,
          containsString(
              "(hashable quantity of dedigitizer schematic: cybeer is limited to 1 by availability in inventory)"));

      assertThat(InventoryManager.getCount(CYBEER_SCHEMATIC), is(0));
      assertThat(InventoryManager.getCount(CYBURGER_SCHEMATIC), is(1));
      assertThat(InventoryManager.getCount(ZERO), is(4));
      assertThat(InventoryManager.getCount(ONE), is(28));

      var requests = client.getRequests();
      assertThat(requests, hasSize(4));

      assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=" + HASHING_VISE.getItemId());
      assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      assertPostRequest(
          requests.get(2),
          "/choice.php",
          "whichchoice=1551&option=1&iid=" + CYBEER_SCHEMATIC.getItemId());
      assertPostRequest(
          requests.get(3),
          "/choice.php",
          "whichchoice=1551&option=1&iid=" + CYBURGER_SCHEMATIC.getItemId());
    }
  }
}
