package net.sourceforge.kolmafia;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withLocation;
import static internal.helpers.Player.withNoItems;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResponseTextParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CryptManagerTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("RequestEditorKitTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("RequestEditorKitTest");
  }

  @Nested
  class VisitCyrpt {
    @Test
    void parsesFourCornerCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 1000));
      try (cleanups) {
        var request = new GenericRequest("crypt.php", true);
        request.responseText = html("request/test_cyrpt_four_corners.html");
        ResponseTextParser.externalUpdate(request);

        assertThat("cyrptNookEvilness", isSetTo(50));
        assertThat("cyrptNicheEvilness", isSetTo(50));
        assertThat("cyrptCrannyEvilness", isSetTo(50));
        assertThat("cyrptAlcoveEvilness", isSetTo(50));
        assertThat("cyrptTotalEvilness", isSetTo(200));
      }
    }

    @Test
    void parsesThreeCornerCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 1000));
      try (cleanups) {
        var request = new GenericRequest("crypt.php", true);
        request.responseText = html("request/test_cyrpt_three_corners.html");
        ResponseTextParser.externalUpdate(request);

        assertThat("cyrptNookEvilness", isSetTo(0));
        assertThat("cyrptNicheEvilness", isSetTo(50));
        assertThat("cyrptCrannyEvilness", isSetTo(50));
        assertThat("cyrptAlcoveEvilness", isSetTo(50));
        assertThat("cyrptTotalEvilness", isSetTo(150));
      }
    }

    @Test
    void parsesTwoCornerCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 1000));
      try (cleanups) {
        var request = new GenericRequest("crypt.php", true);
        request.responseText = html("request/test_cyrpt_two_corners.html");
        ResponseTextParser.externalUpdate(request);

        assertThat("cyrptNookEvilness", isSetTo(0));
        assertThat("cyrptNicheEvilness", isSetTo(0));
        assertThat("cyrptCrannyEvilness", isSetTo(50));
        assertThat("cyrptAlcoveEvilness", isSetTo(50));
        assertThat("cyrptTotalEvilness", isSetTo(100));
      }
    }

    @Test
    void parsesOneCornerCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 1000));
      try (cleanups) {
        var request = new GenericRequest("crypt.php", true);
        request.responseText = html("request/test_cyrpt_one_corner.html");
        ResponseTextParser.externalUpdate(request);

        assertThat("cyrptNookEvilness", isSetTo(0));
        assertThat("cyrptNicheEvilness", isSetTo(0));
        assertThat("cyrptCrannyEvilness", isSetTo(0));
        assertThat("cyrptAlcoveEvilness", isSetTo(50));
        assertThat("cyrptTotalEvilness", isSetTo(50));
      }
    }

    @Test
    void parsesHaertCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 1000));
      try (cleanups) {
        var request = new GenericRequest("crypt.php", true);
        request.responseText = html("request/test_cyrpt_haert.html");
        ResponseTextParser.externalUpdate(request);

        assertThat("cyrptNookEvilness", isSetTo(0));
        assertThat("cyrptNicheEvilness", isSetTo(0));
        assertThat("cyrptCrannyEvilness", isSetTo(0));
        assertThat("cyrptAlcoveEvilness", isSetTo(0));
        assertThat("cyrptTotalEvilness", isSetTo(999));
      }
    }

    @Test
    void parsesEmptyCyrpt() {
      var cleanups =
          new Cleanups(
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 1000));
      try (cleanups) {
        var request = new GenericRequest("crypt.php", true);
        request.responseText = html("request/test_cyrpt_empty.html");
        ResponseTextParser.externalUpdate(request);

        assertThat("cyrptNookEvilness", isSetTo(0));
        assertThat("cyrptNicheEvilness", isSetTo(0));
        assertThat("cyrptCrannyEvilness", isSetTo(0));
        assertThat("cyrptAlcoveEvilness", isSetTo(0));
        assertThat("cyrptTotalEvilness", isSetTo(0));
      }
    }
  }

  @Nested
  class BossFight {
    @Test
    void adjustsEvilnessWhenFightAndDefeatBoss() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withLocation("The Defiled Cranny"),
              withProperty("cyrptNookEvilness", 0),
              withProperty("cyrptNicheEvilness", 0),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptTotalEvilness", 100));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("fight.php")), "");
        client.addResponse(200, html("request/test_cyrpt_boss_encounter.html"));
        client.addResponse(200, "");
        client.addResponse(200, html("request/test_cyrpt_boss_defeat.html"));
        client.addResponse(200, "");

        var request = new GenericRequest("adventure.php?snarfblat=262", true);
        request.run();

        assertThat("cyrptCrannyEvilness", isSetTo(13));
        assertThat("cyrptTotalEvilness", isSetTo(63));

        var fight = new GenericRequest("fight.php?action=attack", true);
        fight.run();

        assertThat("cyrptCrannyEvilness", isSetTo(0));
        assertThat("cyrptTotalEvilness", isSetTo(50));

        var requests = client.getRequests();
        assertThat(requests, hasSize(5));

        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=262");
        assertGetRequest(requests.get(1), "/fight.php", null);
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/fight.php", "action=attack");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void adjustsEvilnessWhenDefeatLastBoss() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withLocation("The Defiled Cranny"),
              withProperty("cyrptNookEvilness", 0),
              withProperty("cyrptNicheEvilness", 0),
              withProperty("cyrptCrannyEvilness", 13),
              withProperty("cyrptAlcoveEvilness", 0),
              withProperty("cyrptTotalEvilness", 13));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("fight.php")), "");
        client.addResponse(200, html("request/test_cyrpt_boss_encounter.html"));
        client.addResponse(200, "");
        client.addResponse(200, html("request/test_cyrpt_boss_defeat.html"));
        client.addResponse(200, "");

        var request = new GenericRequest("adventure.php?snarfblat=262", true);
        request.run();

        var fight = new GenericRequest("fight.php?action=attack", true);
        fight.run();

        assertThat("cyrptCrannyEvilness", isSetTo(0));
        assertThat("cyrptTotalEvilness", isSetTo(999));

        var requests = client.getRequests();
        assertThat(requests, hasSize(5));

        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=262");
        assertGetRequest(requests.get(1), "/fight.php", null);
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/fight.php", "action=attack");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void adjustsEvilnessWhenDefeatBonerdagon() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withNoItems(),
              withLocation("Haert of the Cyrpt"),
              withProperty("cyrptTotalEvilness", 999),
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED));
      try (cleanups) {
        // crypt.php?action=heart -> choice.php -> fight.php
        // We'll skip simulating the choice
        client.addResponse(302, Map.of("location", List.of("fight.php")), "");
        client.addResponse(200, html("request/test_cyrpt_bonerdagon_encounter.html"));
        client.addResponse(200, "");
        client.addResponse(200, html("request/test_cyrpt_bonerdagon_defeat.html"));
        client.addResponse(200, "");

        var request = new GenericRequest("crypt.php?action=heart", true);
        request.run();

        var fight = new GenericRequest("fight.php?action=attack", true);
        fight.run();

        assertThat(Quest.CYRPT, isStep(1));
        assertThat("cyrptTotalEvilness", isSetTo(0));
        assertTrue(InventoryManager.hasItem(ItemPool.BONERDAGON_SKULL));
        assertTrue(InventoryManager.hasItem(ItemPool.BONERDAGON_CHEST));

        var requests = client.getRequests();
        assertThat(requests, hasSize(5));

        assertPostRequest(requests.get(0), "/crypt.php", "action=heart");
        assertGetRequest(requests.get(1), "/fight.php", null);
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/fight.php", "action=attack");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }
}
