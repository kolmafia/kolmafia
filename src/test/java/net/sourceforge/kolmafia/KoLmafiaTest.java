package net.sourceforge.kolmafia;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withConcoctionRefresh;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withGoal;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLocation;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withResponses;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class KoLmafiaTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("KoLmafiaTest");
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
  class Goals {
    private Cleanups withCanAdventure() {
      return new Cleanups(
          withAdventuresLeft(1),
          withHP(50, 50, 50),
          withEquipped(Slot.WEAPON, ItemPool.JUNE_CLEAVER));
    }

    private Cleanups canAccessHoleInTheSky() {
      return withItem(ItemPool.ROCKETSHIP, 1);
    }

    private Cleanups canAccessOilPeak() {
      return withQuestProgress(Quest.TOPPING, 1);
    }

    private FakeHttpClientBuilder buyStarKey() {
      var starKeyAcquired =
          "<html>You place the stars and lines on the chart -- the chart bursts into flames"
              + " and leaves behind a sweet star item!"
              + "<b>You acquire an item: <b>Richard's star key</b></b></html>";
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(
          req -> {
            if (req.uri().getPath().equals("/shop.php")) {
              return new FakeHttpResponse<>(200, starKeyAcquired);
            }
            return new FakeHttpResponse<>(200, "Nonempty");
          });
      return builder;
    }

    @Test
    public void satisfiesCoinMasterGoal() {
      var builder = buyStarKey();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withCanAdventure(),
              withGoal(ItemPool.get(ItemPool.STAR_KEY, 1)),
              canAccessHoleInTheSky(),
              withItem(ItemPool.STAR_CHART, 1),
              withItem(ItemPool.STAR, 8),
              withItem(ItemPool.LINE, 7),
              withConcoctionRefresh());

      try (cleanups) {
        assertNull(CreateItemRequest.getInstance(ItemPool.get(ItemPool.STAR_KEY, 1)));

        KoLmafia.makeRequest(AdventureDatabase.getAdventureByName("The Hole in the Sky"), 1);

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
              withCanAdventure(),
              withGoal(ItemPool.get(ItemPool.STAR_KEY, 1)),
              canAccessHoleInTheSky(),
              withItem(ItemPool.STAR_CHART, 1),
              withItem(ItemPool.STAR, 7),
              withItem(ItemPool.LINE, 7),
              withLocation("The Hole in the Sky"),
              withConcoctionRefresh());

      try (cleanups) {
        KoLmafia.makeRequest(AdventureDatabase.getAdventureByName("The Hole in the Sky"), 1);

        var paths = client.getRequests().stream().map(r -> r.uri().getPath()).toList();
        assertThat(paths, not(hasItem("/shop.php")));

        var adventures =
            client.getRequests().stream()
                .filter(r -> r.uri().getPath().equals("/adventure.php"))
                .toList();
        assertThat(adventures, hasSize(1));
        assertPostRequest(adventures.get(0), "/adventure.php", containsString("snarfblat=83"));
      }
    }

    private FakeHttpClientBuilder makeJarOfOil() {
      var oilAcquired =
          "<html>Driven by forces you don't understand, you make a jar out of oil and fill it with oil before making a lid out of oil and sealing the jar."
              + "<b>You acquire an item: <b>jar of oil</b></b></html>";
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(
          req -> {
            if (req.uri().getPath().equals("/multiuse.php")) {
              return new FakeHttpResponse<>(200, oilAcquired);
            }
            return new FakeHttpResponse<>(200, "Nonempty");
          });
      return builder;
    }

    @Test
    public void createsCreatableGoal() {
      var builder = makeJarOfOil();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withCanAdventure(),
              canAccessOilPeak(),
              withGoal(ItemPool.get(ItemPool.JAR_OF_OIL, 1)),
              withItem(ItemPool.BUBBLIN_CRUDE, 12),
              withConcoctionRefresh());

      try (cleanups) {
        assertThat(
            CreateItemRequest.getInstance(ItemPool.get(ItemPool.JAR_OF_OIL, 1))
                .getQuantityPossible(),
            equalTo(1));

        KoLmafia.makeRequest(AdventureDatabase.getAdventureByName("Oil Peak"), 1);

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/multiuse.php", "action=useitem&quantity=12&whichitem=5789");
      }
    }

    @Test
    public void doesNotCreateCreatableGoalWithoutEnoughIngredients() {
      var builder = makeJarOfOil();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withCanAdventure(),
              canAccessOilPeak(),
              withGoal(ItemPool.get(ItemPool.JAR_OF_OIL, 1)),
              withItem(ItemPool.BUBBLIN_CRUDE, 11),
              withLocation("Oil Peak"),
              withConcoctionRefresh());

      try (cleanups) {
        assertThat(
            CreateItemRequest.getInstance(ItemPool.get(ItemPool.JAR_OF_OIL, 1))
                .getQuantityPossible(),
            equalTo(0));

        KoLmafia.makeRequest(AdventureDatabase.getAdventureByName("Oil Peak"), 1);

        var requests = client.getRequests();
        var adventures =
            requests.stream().filter(r -> r.uri().getPath().equals("/adventure.php")).toList();
        assertThat(adventures, hasSize(1));
        assertPostRequest(adventures.get(0), "/adventure.php", containsString("snarfblat=298"));
      }
    }
  }
}
