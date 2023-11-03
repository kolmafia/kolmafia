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
import net.sourceforge.kolmafia.RequestThread;
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
            withProperty("_leavesJumped", false),
            withItem(ItemPool.INFLAMMABLE_LEAF, 2),
            withHttpClientBuilder(builder));

    try (cleanups) {
      BurningLeavesRequest.visit();
      RequestThread.postRequest(new BurningLeavesRequest(1));

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
            withProperty("_leavesJumped", false),
            withChoice(1510, html("request/test_choice_burning_leaves_already_jumped.html")));

    try (cleanups) {
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
            withProperty("_leavesJumped", false),
            withChoice(1510, 2, html("request/test_choice_burning_leaves_just_jumped.html")));

    try (cleanups) {
      assertThat("_leavesBurned", isSetTo(0));
      assertThat("_leavesJumped", isSetTo(true));
    }
  }

  @Test
  void canDetectMaxLassosMade() {
    var cleanups =
        new Cleanups(
            withCampgroundItem(ItemPool.A_GUIDE_TO_BURNING_LEAVES),
            withProperty("_leavesBurned", 0),
            withProperty("_leafLassosCrafted", 0),
            withChoice(
                1510, 1, "leaves=69", html("request/test_choice_burning_leaves_max_summon.html")));

    try (cleanups) {
      assertThat("_leavesBurned", isSetTo(0));
      assertThat("_leafLassosCrafted", isSetTo(3));
    }
  }

  @Test
  void canDetectMaxMonstersFought() {
    var cleanups =
        new Cleanups(
            withCampgroundItem(ItemPool.A_GUIDE_TO_BURNING_LEAVES),
            withProperty("_leavesBurned", 0),
            withProperty("_leafMonstersFought", 0),
            withChoice(
                1510, 1, "leaves=666", html("request/test_choice_burning_leaves_max_summon.html")));

    try (cleanups) {
      assertThat("_leavesBurned", isSetTo(0));
      assertThat("_leafMonstersFought", isSetTo(5));
    }
  }
}
