package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withCampgroundItem;
import static internal.helpers.Player.withChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Item.isInInventory;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BurningLeavesRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("BurningLeavesRequestTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("BurningLeavesRequestTest");
  }

  @Test
  void canParseZeroLeavesBurned() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(200, html("request/test_choice_burning_leaves_zero_leaves.html"));
    builder.client.addResponse(
        200, html("request/test_choice_burning_leaves_just_burned_one.html"));

    var cleanups =
        new Cleanups(
            withCampgroundItem(ItemPool.A_GUIDE_TO_BURNING_LEAVES),
            withProperty("_leavesBurned", 0),
            withProperty("_leavesJumped"),
            withItem(ItemPool.INFLAMMABLE_LEAF, 2),
            withHttpClientBuilder(builder));

    try (cleanups) {
      new BurningLeavesRequest(1).run();

      assertThat("_leavesBurned", isSetTo(1));
      assertThat("_leavesJumped", isSetTo(false));
      assertThat(ItemPool.INFLAMMABLE_LEAF, isInInventory(1));
    }
  }

  @Test
  void canDiscoverState() {
    var cleanups =
        new Cleanups(
            withCampgroundItem(ItemPool.A_GUIDE_TO_BURNING_LEAVES),
            withProperty("_leavesBurned", 0),
            withProperty("_leavesJumped"),
            withChoice(1510, html("request/test_choice_burning_leaves_already_jumped.html")));

    try (cleanups) {
      new GenericRequest("choice.php?whichchoice=1510").run();

      assertThat("_leavesBurned", isSetTo(3));
      assertThat("_leavesJumped", isSetTo(true));
    }
  }

  @Test
  void canParseJump() {
    var cleanups =
        new Cleanups(
            withCampgroundItem(ItemPool.A_GUIDE_TO_BURNING_LEAVES),
            withProperty("_leavesBurned", 0),
            withProperty("_leavesJumped"),
            withChoice(1510, html("request/test_choice_burning_leaves_just_jumped.html")));

    try (cleanups) {
      new GenericRequest("choice.php?whichchoice=1510&option=2").run();

      assertThat("_leavesBurned", isSetTo(0));
      assertThat("_leavesJumped", isSetTo(true));
    }
  }
}
