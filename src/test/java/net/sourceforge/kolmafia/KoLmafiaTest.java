package net.sourceforge.kolmafia;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withGoal;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withResponses;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class KoLmafiaTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset(true);
  }

  @Test
  public void canDetectRequirementsMet() {
    var cleanups = new Cleanups(withItem("seal-clubbing club", 20), withItem("seal tooth", 4));

    try (cleanups) {
      ArrayList<AdventureResult> requirements =
          new ArrayList<>(
              List.of(
                  AdventureResult.tallyItem("seal-clubbing club", 11, true),
                  AdventureResult.tallyItem("seal tooth", 4, true)));

      var result = KoLmafia.checkRequirements(requirements, false);
      assertTrue(result);
    }
  }

  @Test
  public void canDetectRequirementsNotMet() {
    var cleanups = new Cleanups(withItem("seal-clubbing club", 20), withItem("seal tooth", 2));

    try (cleanups) {
      ArrayList<AdventureResult> requirements =
          new ArrayList<>(
              List.of(
                  AdventureResult.tallyItem("seal-clubbing club", 11, true),
                  AdventureResult.tallyItem("seal tooth", 4, true)));

      var result = KoLmafia.checkRequirements(requirements, false);
      assertFalse(result);

      assertThat(requirements, hasSize(1));
      assertEquals(2, requirements.get(0).getCount());
    }
  }

  @Test
  public void refreshSessionSetsPassiveModifiers() {
    // This charsheet contains Stomach of Steel.
    var cleanups =
        new Cleanups(
            withSkill(SkillPool.MARIACHI_MEMORY),
            withResponses(
                Map.of(
                    "https://www.kingdomofloathing.com:443/charsheet.php",
                    new FakeHttpResponse<>(200, html("request/test_charsheet_normal.html")))));

    try (cleanups) {
      // Prime the passive skill cache.
      KoLCharacter.recalculateAdjustments();

      PreferenceListenerRegistry.deferPreferenceListeners(true);
      assertEquals(15, KoLCharacter.getStomachCapacity());
      KoLmafia.refreshSession();
      assertEquals(20, KoLCharacter.getStomachCapacity());
      PreferenceListenerRegistry.deferPreferenceListeners(false);
    }
  }

  @Nested
  class CoinMasterGoals {
    // A Star Chart item can be made by "A Star Chart", which is a coin master.
    // Coin master purchases are not a permitted crafting method unless the user has
    // set autoSatisfyWithCoinmasters, but asking for an item as an adventuring goal
    // is explicit enough that we should still buy it for them.

    private static final String ACQUIRED =
        "<html>You place the stars and lines on the chart -- the chart bursts into flames"
            + " and leaves behind a sweet star item!"
            + "<b>You acquire an item: <b>Richard's star key</b></b></html>";

    private FakeHttpClientBuilder buyStarKey() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(
          req -> {
            if (req.uri().getPath().equals("/shop.php")) {
              return new FakeHttpResponse<>(200, ACQUIRED);
            }
            return new FakeHttpResponse<>(200, "");
          });
      return builder;
    }

    @Test
    public void satisfiesCoinMasterGoalWithoutAutoSatisfy() {
      var builder = buyStarKey();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withAdventuresLeft(1),
              withGoal(ItemPool.get(ItemPool.STAR_KEY, 1)),
              withItem(ItemPool.STAR_CHART, 1),
              withItem(ItemPool.STAR, 8),
              withItem(ItemPool.LINE, 7));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctionsNow();

        // We have not opted into trading with coin masters, so there is no
        // permitted method of creating this.
        assertNull(CreateItemRequest.getInstance(ItemPool.get(ItemPool.STAR_KEY, 1)));

        KoLmafia.makeRequest(AdventureDatabase.getAdventureByName("The Hole in the Sky"), 1);

        // Nevertheless, it is an explicit goal, so we buy it.
        var requests = client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/shop.php", "whichshop=starchart&action=buyitem&whichrow=141&ajax=1");
      }
    }

    @Test
    public void doesNotBuyCoinMasterGoalIfUnaffordable() {
      var builder = buyStarKey();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withAdventuresLeft(0),
              withGoal(ItemPool.get(ItemPool.STAR_KEY, 1)),
              withItem(ItemPool.STAR_CHART, 1),
              withItem(ItemPool.STAR, 7),
              withItem(ItemPool.LINE, 7));

      try (cleanups) {
        ConcoctionDatabase.refreshConcoctionsNow();

        KoLmafia.makeRequest(AdventureDatabase.getAdventureByName("The Hole in the Sky"), 1);

        // We cannot afford it, so we just go adventuring.
        assertThat(client.getRequests(), hasSize(0));
      }
    }
  }
}
