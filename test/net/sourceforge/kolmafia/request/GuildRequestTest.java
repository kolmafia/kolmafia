package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withoutItem;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class GuildRequestTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("GuildRequestTest");
  }

  @BeforeEach
  protected void beforeEach() {
    Preferences.reset("GuildRequestTest");
  }

  @AfterEach
  protected void afterEach() {
    KoLConstants.inventory.clear();
  }

  @Test
  public void canTrackWizardOfEgoQuest() {
    // First talk with "ocg"
    GuildRequest request = new GuildRequest("ocg");
    String responseText = html("request/test_guild_quest_ego_intro.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), QuestDatabase.UNSTARTED);

    // Second talk with "ocg"
    request = new GuildRequest("ocg");
    responseText = html("request/test_guild_quest_ego_started.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), QuestDatabase.STARTED);

    // Talk after turning in key and getting next step
    AdventureResult key = ItemPool.get(ItemPool.FERNSWARTHYS_KEY);
    request = new GuildRequest("ocg");
    responseText = html("request/test_guild_quest_ego_step2.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), "step2");
    assertTrue(KoLConstants.inventory.contains(key));

    // Talk after finding dusty old book
    AdventureResult dusty = ItemPool.get(ItemPool.DUSTY_BOOK);
    AdventureResult manual = ItemPool.get(ItemPool.MOX_MANUAL);
    request = new GuildRequest("ocg");
    responseText = html("request/test_guild_quest_ego_finished.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), QuestDatabase.FINISHED);
    assertFalse(KoLConstants.inventory.contains(key));
    assertFalse(KoLConstants.inventory.contains(dusty));
    assertTrue(KoLConstants.inventory.contains(manual));
  }

  @Nested
  class WhiteCitadel {
    @Test
    public void canDetectWhiteCitadelQuestStarted() {
      // You have completed the Meatcar and visited "paco",
      // You visit "paco" again and they give the White Citadel Quest.
      //
      // We want to parse his response and start the quest
      //
      // Requesting: guild.php?place=paco
      // Field: location = [choice.php?forceoption=0]
      // Requesting: choice.php?forceoption=0
      // Requesting: choice.php?pwd&whichchoice=930&option=1

      var cleanups = withProperty("questG02Whitecastle", "unstarted");

      try (cleanups) {
        // talk with "ocg"
        GenericRequest request = new GenericRequest("choice.php?forceoption=0");
        String responseText = html("request/test_guild_quest_citadel_started_0.html");
        request.responseText = responseText;
        request.setHasResult(true);
        ChoiceManager.preChoice(request);
        request.processResponse();
        assertEquals(QuestDatabase.UNSTARTED, QuestDatabase.getQuest(Quest.CITADEL));

        request = new GenericRequest("choice.php?pwd&whichchoice=930&option=1");
        responseText = html("request/test_guild_quest_citadel_started_1.html");
        request.responseText = responseText;
        request.setHasResult(true);
        ChoiceManager.preChoice(request);
        request.processResponse();
        assertEquals(QuestDatabase.STARTED, QuestDatabase.getQuest(Quest.CITADEL));
      }
    }

    @Test
    public void canDetectWhiteCitadelQuestFinished() {
      // You have visited White Citadel and been given a White Citadel Satisfaction Satchel
      // Quest.CITADEL is at "step10"
      // You return to the guild and speak to "paco"
      // They take your satchel and give you a lucky rabbit's foot.
      //
      // White Citadel Satisfaction Satchel -> removed from inventory
      // Quest.CITADEL is at QuestDatabase.FINISHED
      //
      // Requesting: guild.php?place=paco
      // Field: location = [choice.php?forceoption=0]
      // Requesting: choice.php?forceoption=0

      var cleanups = withItem(ItemPool.CITADEL_SATCHEL);

      try (cleanups) {
        // talk with "ocg"
        GenericRequest request = new GenericRequest("choice.php?forceoption=0");
        String responseText = html("request/test_guild_quest_citadel_finished.html");
        request.responseText = responseText;
        request.setHasResult(true);
        ChoiceManager.preChoice(request);
        request.processResponse();
        assertEquals(1, InventoryManager.getCount(ItemPool.LUCKY_RABBIT_FOOT));
        assertEquals(0, InventoryManager.getCount(ItemPool.CITADEL_SATCHEL));
        assertEquals(QuestDatabase.FINISHED, QuestDatabase.getQuest(Quest.CITADEL));
      }
    }
  }

  @Nested
  class Nemesis {
    // Quest.NEMESIS	questG04Nemesis		new Nemesis Quest
    //
    // started        "scg" offers quest; Misspelled Cemetary is open
    // step1          Tomb of the Unknown Your Class Here (1049 - meet ghost)
    // step2          Fight The Unknown <class>
    // step3          Tomb of the Unknown Your Class Here (1049 - defeated ghost)
    // step4          Tomb of the Unknown Your Class Here (1049 - claimed Epic Weapon)
    // step5          "scg" sends to Beelzebozo; The "Fun" House is open
    // step6          Fight The Clownlord Beelzebozo
    // step7          "scg" directs you to craft legendary Epic Weapon
    // step8          Crafted Legendary Epic Weapon
    // step9          "scg" sees Legendary Epic Weapon
    // step10         "scg" sends to cave; Dark and Dank and Sinister Cave is open
    // step11         The Dark and Dank and Sinister Cave Entrance (1087 - see puzzle)
    // step12         The Dark and Dank and Sinister Cave Entrance (1087 - passed)
    // step13         Rubble, Rubble, Toil and Trouble (1088 - see pile of rubble)
    // step14         Acquired 6 or more fizzing spore pods
    // step15         Rubble, Rubble, Toil and Trouble (1088 - blew up rubble)
    // step16         Defeated Nemesis (Inner Sanctum) and acquire Epic Hat
    // step16.5       "scg" sees Epic Hat
    // step17         Second visit to scg" unlocks nemesis assassins
    // step18         Lose to menacing thug
    // step19         Defeat menacing thug
    // step20         Lose to Mob Penguin hitman
    // step21         Defeat Mob Penguin hitman
    // step22         Lose to third Nemesis assassin
    // step23         Defeat third Nemesis assassin
    // step24         Lose to fourth Nemesis assassin
    // step25         Defeat 4th Nemesis assassin (gain volcano map)
    // step26         (Obsolete; unlocks volcano lair via navigator on the Poop Deck)
    // step27         Defeat Nemesis (The Nemesis; Lair) and obtain Legendary Pants
    // step28         Start fight with Nemesis (Volcanic Cave)
    // step29         Defeat Nemesis (Volcanic Cave)
    // finished       Defeat Demonic Nemesis and obtain final quest rewards

    @Test
    public void defeatingCaveBossIsStep16() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.NEMESIS, "step15"),
              withoutItem(ItemPool.EL_SOMBRERO_DE_LOPEZ));

      try (cleanups) {
        client.addResponse(
            302, Map.of("location", List.of("fight.php?ireallymeanit=1742736449")), "");
        client.addResponse(200, html("request/test_nemesis_caveboss_1.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_nemesis_caveboss_2.html"));
        client.addResponse(200, ""); // api.php

        assertThat(InventoryManager.getCount(ItemPool.EL_SOMBRERO_DE_LOPEZ), is(0));
        assertThat(Quest.NEMESIS, isStep("step15"));

        GenericRequest visit =
            new GenericRequest("place.php?whichplace=nemesiscave&action=nmcave_boss");
        visit.run();
        GenericRequest fight = new GenericRequest("fight.php?action=skill&whichskill=7513");
        fight.run();

        assertThat(InventoryManager.getCount(ItemPool.EL_SOMBRERO_DE_LOPEZ), is(1));
        assertThat(Quest.NEMESIS, isStep("step16"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(5));

        assertPostRequest(
            requests.get(0), "/place.php", "whichplace=nemesiscave&action=nmcave_boss");
        assertGetRequest(requests.get(1), "/fight.php", "ireallymeanit=1742736449");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/fight.php", "action=skill&whichskill=7513");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void talkToGuildOnceIsStep16A() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withQuestProgress(Quest.NEMESIS, "step16"));

      try (cleanups) {
        client.addResponse(200, html("request/test_nemesis_caveboss_guild_1.html"));
        client.addResponse(200, ""); // api.php

        GenericRequest visit = new GenericRequest("guild.php?place=scg");
        visit.run();

        assertThat(Quest.NEMESIS, isStep("step16.5"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/guild.php", "place=scg");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void talkToGuildAgainIsStep17() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder), withQuestProgress(Quest.NEMESIS, "step16.5"));

      try (cleanups) {
        client.addResponse(200, html("request/test_nemesis_caveboss_guild_2.html"));
        client.addResponse(200, ""); // api.php

        GenericRequest visit = new GenericRequest("guild.php?place=scg");
        visit.run();

        assertThat(Quest.NEMESIS, isStep("step17"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));

        assertPostRequest(requests.get(0), "/guild.php", "place=scg");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }
}
