package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResponseTextParserTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("ResponseTextParserTest");
    Preferences.reset("ResponseTextParserTest");
  }

  @ParameterizedTest
  @ValueSource(ints = {SkillPool.SLIMY_SHOULDERS, SkillPool.SLIMY_SYNAPSES, SkillPool.SLIMY_SINEWS})
  void canLearnSlimeSkills(int skillId) {
    var levelPref = "skillLevel" + skillId;
    var cleanups = new Cleanups(withProperty(levelPref, 1));
    try (cleanups) {
      ResponseTextParser.learnSkill(skillId);
      assertThat(levelPref, isSetTo(2));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {SkillPool.SLIMY_SHOULDERS, SkillPool.SLIMY_SYNAPSES, SkillPool.SLIMY_SINEWS})
  void cannotOverlearnSlimeSkills(int skillId) {
    var levelPref = "skillLevel" + skillId;
    var cleanups = new Cleanups(withProperty(levelPref, 10));
    try (cleanups) {
      ResponseTextParser.learnSkill(skillId);
      assertThat(levelPref, isSetTo(10));
    }
  }

  @Test
  public void canLearnRecipeFromItem() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withPasswordHash("recipe"),
            // If you have a password hash, KoL looks at your vinyl boots
            withGender(KoLCharacter.FEMALE),
            withProperty("unknownRecipe10974", true),
            withProperty("_concoctionDatabaseRefreshes", 0));
    try (cleanups) {
      client.addResponse(200, html("request/test_learn_recipe.html"));
      client.addResponse(200, ""); // api.php

      String URL = "inv_use.php?which=3&whichitem=10983&pwd&ajax=1";
      var request = new GenericRequest(URL);
      request.run();

      // Learned recipe: Pete's wiley whey bar (10974)
      assertThat("unknownRecipe10974", isSetTo(false));
      assertThat("_concoctionDatabaseRefreshes", isSetTo(1));

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0), "/inv_use.php", "which=3&whichitem=10983&ajax=1&pwd=recipe");
      assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
    }
  }

  @Nested
  class ConcoctionRefresh {
    // Testing heuristics to minimize calls to ConcoctionDatabase.refreshConcoctions.
    //
    // We will adventure in Menagerie Level 1

    public Cleanups withConcoctionsRefreshed() {
      // Refresh concoctions to ensure they are up to date.
      ConcoctionDatabase.refreshConcoctions(true);
      return withProperty("_concoctionDatabaseRefreshes", 0);
    }

    @Nested
    class Golem {

      // The Fruit Golem drops 3 pieces of fruit and some Meat
      //    Each of those is both usable and an ingredient

      private static final AdventureResult ORANGE = ItemPool.get("orange", 0);
      private static final AdventureResult CHERRY = ItemPool.get("cherry", 0);
      private static final AdventureResult LIME = ItemPool.get("lime", 0);

      private static void addGolemResponses(FakeHttpClientBuilder builder) {
        var client = builder.client;
        // adventure.php?snarfblat=51
        builder.client.addResponse(
            302, Map.of("location", List.of("fight.php?ireallymeanit=1667680471")), "");
        client.addResponse(200, html("request/test_menagerie_golem_fight_0.html"));
        client.addResponse(200, ""); // api.php
        // fight.php?action=attack
        client.addResponse(200, html("request/test_menagerie_golem_fight_1.html"));
        client.addResponse(200, ""); // api.php
      }

      private static void checkGolemRequests(FakeHttpClientBuilder builder) {
        var client = builder.client;
        var requests = client.getRequests();
        assertThat(requests, hasSize(5));

        assertPostRequest(
            requests.get(0), "/adventure.php", "snarfblat=" + AdventurePool.MENAGERIE_LEVEL_1);
        assertGetRequest(requests.get(1), "/fight.php", "ireallymeanit=1667680471");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/fight.php", "action=attack");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }

      @Test
      public void willRefreshAfterGainingItems() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withItem(ORANGE),
                withItem(CHERRY),
                withItem(LIME),
                withHP(500, 500, 500),
                withConcoctionsRefreshed());
        try (cleanups) {
          addGolemResponses(builder);
          String URL = "adventure.php?snarfblat=" + AdventurePool.MENAGERIE_LEVEL_1;
          var request = new GenericRequest(URL);
          request.run();

          // Starting a fight does not require a refresh
          assertThat("_concoctionDatabaseRefreshes", isSetTo(0));

          URL = "fight.php?action=attack";
          request = new GenericRequest(URL);
          request.run();

          // We got three items that would require a refresh
          assertEquals(1, InventoryManager.getCount(ORANGE));
          assertEquals(1, InventoryManager.getCount(CHERRY));
          assertEquals(1, InventoryManager.getCount(LIME));

          // We refreshed only once even though we got three items
          assertThat("_concoctionDatabaseRefreshes", isSetTo(1));

          checkGolemRequests(builder);
        }
      }
    }

    @Nested
    class Elemental {
      // The BASIC Elemental drops a GOTO
      //    That is neither usable nor an ingredient

      private static final AdventureResult GOTO = ItemPool.get("GOTO", 0);

      private static void addElementalResponses(FakeHttpClientBuilder builder) {
        var client = builder.client;
        // adventure.php?snarfblat=51
        builder.client.addResponse(
            302, Map.of("location", List.of("fight.php?ireallymeanit=1667675621")), "");
        client.addResponse(200, html("request/test_menagerie_elemental_fight_0.html"));
        client.addResponse(200, ""); // api.php
        // fight.php?action=attack
        client.addResponse(200, html("request/test_menagerie_elemental_fight_1.html"));
        client.addResponse(200, ""); // api.php
      }

      private static void checkElementalRequests(FakeHttpClientBuilder builder) {
        var client = builder.client;
        var requests = client.getRequests();
        assertThat(requests, hasSize(5));

        assertPostRequest(
            requests.get(0), "/adventure.php", "snarfblat=" + AdventurePool.MENAGERIE_LEVEL_1);
        assertGetRequest(requests.get(1), "/fight.php", "ireallymeanit=1667675621");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/fight.php", "action=attack");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }

      @Test
      public void willNotRefreshAfterGainingUnusableItems() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withItem(GOTO),
                withHP(500, 500, 500),
                withConcoctionsRefreshed());
        try (cleanups) {
          addElementalResponses(builder);

          String URL = "adventure.php?snarfblat=" + AdventurePool.MENAGERIE_LEVEL_1;
          var request = new GenericRequest(URL);
          request.run();

          // Starting a fight does not force a refresh
          assertThat("_concoctionDatabaseRefreshes", isSetTo(0));

          URL = "fight.php?action=attack";
          request = new GenericRequest(URL);
          request.run();

          // We did not refresh again because GOTO is not usable.
          // However, we do refresh because we spent a turn.
          assertEquals(1, InventoryManager.getCount(GOTO));
          assertThat("_concoctionDatabaseRefreshes", isSetTo(1));

          checkElementalRequests(builder);
        }
      }
    }
  }
}
