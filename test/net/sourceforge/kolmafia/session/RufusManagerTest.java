package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNoEffects;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isStarted;
import static internal.matchers.Quest.isStep;
import static internal.matchers.Quest.isUnstarted;
import static org.hamcrest.MatcherAssert.assertThat;
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
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.RufusManager.ShadowTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class RufusManagerTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("RufusManager");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("RufusManager");
  }

  private static AdventureResult SHADOW_AFFINITY = EffectPool.get(EffectPool.SHADOW_AFFINITY);

  @ParameterizedTest
  @CsvSource({
    "test_call_rufus, shadow matrix, shadow heart, shadow venom",
    "test_calling_rufus, shadow scythe, shadow snowflake, shadow brick"
  })
  public void callingRufusLearnsQuests(String file, String entity, String artifact, String item) {
    // Test that we can detect entity, artifact, and item quests
    //
    // Items are tricky, since they are presented as "3 PLURAL_NAME" and
    // we want to save rufusTestTarget as the non-plural argument

    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("rufusDesiredArtifact"),
            withProperty("rufusDesiredEntity"),
            withProperty("rufusDesiredItems"),
            withQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED),
            withProperty("rufusQuestTarget"),
            withProperty("rufusQuestType"));
    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/" + file + ".html"));
      client.addResponse(200, ""); // api.php
      client.addResponse(200, html("request/test_hang_up_on_rufus.html"));

      var useItemURL =
          "inv_use.php?which=3&whichitem=" + ItemPool.CLOSED_CIRCUIT_PAY_PHONE + "&ajax=1";
      var useRequest = new GenericRequest(useItemURL);
      useRequest.run();

      // He told us what he needs
      assertThat("rufusDesiredArtifact", isSetTo(artifact));
      assertThat("rufusDesiredEntity", isSetTo(entity));
      assertThat("rufusDesiredItems", isSetTo(item));

      // We are in a choice that you cannot walk away from.
      assertTrue(ChoiceManager.handlingChoice);
      assertEquals(1497, ChoiceManager.lastChoice);

      // Choose option 6 - Hang up
      var choiceURL = "choice.php?pwd&whichchoice=1497&option=6";
      var choiceRequest = new GenericRequest(choiceURL);
      choiceRequest.run();

      // We are no longer in a choice
      assertFalse(ChoiceManager.handlingChoice);

      // We still know what Rufus needs
      assertThat("rufusDesiredArtifact", isSetTo(artifact));
      assertThat("rufusDesiredEntity", isSetTo(entity));
      assertThat("rufusDesiredItems", isSetTo(item));

      // We did not start the quest
      assertThat(Quest.RUFUS, isUnstarted());
    }
  }

  @Test
  public void acceptingAnArtifactQuestSetsState() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("rufusDesiredArtifact"),
            withProperty("rufusDesiredEntity"),
            withProperty("rufusDesiredItems"),
            withQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED),
            withProperty("rufusQuestTarget"),
            withProperty("rufusQuestType"),
            withProperty("_shadowAffinityToday", false),
            withNoEffects());
    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_call_rufus.html"));
      client.addResponse(200, ""); // api.php
      client.addResponse(200, html("request/test_accept_rufus_quest_artifact.html"));

      var useItemURL =
          "inv_use.php?which=3&whichitem=" + ItemPool.CLOSED_CIRCUIT_PAY_PHONE + "&ajax=1";
      var useRequest = new GenericRequest(useItemURL);
      useRequest.run();

      // He told us what he needs
      assertThat("rufusDesiredArtifact", isSetTo("shadow heart"));
      assertThat("rufusDesiredEntity", isSetTo("shadow matrix"));
      assertThat("rufusDesiredItems", isSetTo("shadow venom"));

      // We are in a choice that you cannot walk away from.
      assertTrue(ChoiceManager.handlingChoice);
      assertEquals(1497, ChoiceManager.lastChoice);

      // Choose option 2 - I'll find the artifact
      var choiceURL = "choice.php?pwd&whichchoice=1497&option=2";
      var choiceRequest = new GenericRequest(choiceURL);
      choiceRequest.run();

      // We are no longer in a choice
      assertFalse(ChoiceManager.handlingChoice);

      // We no longer record what the last options were
      assertThat("rufusDesiredArtifact", isSetTo(""));
      assertThat("rufusDesiredEntity", isSetTo(""));
      assertThat("rufusDesiredItems", isSetTo(""));

      // Instead, quest properties are set
      assertThat(Quest.RUFUS, isStarted());
      assertThat("rufusQuestType", isSetTo("artifact"));
      assertThat("rufusQuestTarget", isSetTo("shadow heart"));

      // Since this was the first quest of the day, we have Shadow Affinity
      assertEquals(11, SHADOW_AFFINITY.getCount(KoLConstants.activeEffects));
      assertThat("_shadowAffinityToday", isSetTo(true));
    }
  }

  @Test
  public void returningAnArtifactSetsState() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withQuestProgress(Quest.RUFUS, "step1"),
            withProperty("rufusQuestTarget", "shadow heart"),
            withProperty("rufusQuestType", "artifact"),
            withItem(ItemPool.SHADOW_HEART),
            withItem(ItemPool.RUFUS_SHADOW_LODESTONE, 0),
            withNoEffects());
    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_call_rufus_back_artifact.html"));
      client.addResponse(200, html("request/test_return_artifact_to_rufus.html"));
      client.addResponse(200, ""); // api.php

      var useItemURL =
          "inv_use.php?which=3&whichitem=" + ItemPool.CLOSED_CIRCUIT_PAY_PHONE + "&ajax=1";
      var useRequest = new GenericRequest(useItemURL);
      useRequest.run();

      // We are in a choice that you cannot walk away from.
      assertTrue(ChoiceManager.handlingChoice);
      assertEquals(1498, ChoiceManager.lastChoice);

      // He wants to know if we succeeded
      // Choose option 1 - Yeah, I got it
      var choiceURL = "choice.php?pwd&whichchoice=1498&option=1";
      var choiceRequest = new GenericRequest(choiceURL);
      choiceRequest.run();

      // We are no longer in a choice
      assertFalse(ChoiceManager.handlingChoice);

      // The quest properties are reset
      assertThat(Quest.RUFUS, isUnstarted());
      assertThat("rufusQuestType", isSetTo(""));
      assertThat("rufusQuestTarget", isSetTo(""));

      // We no longer have the artifact
      assertFalse(InventoryManager.hasItem(ItemPool.SHADOW_HEART));

      // We have Rufus's shadow lodestone
      assertTrue(InventoryManager.hasItem(ItemPool.RUFUS_SHADOW_LODESTONE));
    }
  }

  @Test
  public void returningItemsSetsState() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withQuestProgress(Quest.RUFUS, "step1"),
            withProperty("rufusQuestTarget", "shadow skin"),
            withProperty("rufusQuestType", "items"),
            withItem(ItemPool.SHADOW_SKIN, 3),
            withItem(ItemPool.RUFUS_SHADOW_LODESTONE, 0),
            withNoEffects());
    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_call_rufus_back_items.html"));
      client.addResponse(200, html("request/test_return_items_to_rufus.html"));
      client.addResponse(200, ""); // api.php

      var useItemURL =
          "inv_use.php?which=3&whichitem=" + ItemPool.CLOSED_CIRCUIT_PAY_PHONE + "&ajax=1";
      var useRequest = new GenericRequest(useItemURL);
      useRequest.run();

      // We are in a choice that you cannot walk away from.
      assertTrue(ChoiceManager.handlingChoice);
      assertEquals(1498, ChoiceManager.lastChoice);

      // He wants to know if we succeeded
      // Choose option 1 - Yes, I have them right here.
      var choiceURL = "choice.php?pwd&whichchoice=1498&option=1";
      var choiceRequest = new GenericRequest(choiceURL);
      choiceRequest.run();

      // We are no longer in a choice
      assertFalse(ChoiceManager.handlingChoice);

      // The quest properties are reset
      assertThat(Quest.RUFUS, isUnstarted());
      assertThat("rufusQuestType", isSetTo(""));
      assertThat("rufusQuestTarget", isSetTo(""));

      // We have three fewer items
      assertEquals(0, InventoryManager.getCount(ItemPool.SHADOW_SKIN));

      // We have Rufus's shadow lodestone
      assertTrue(InventoryManager.hasItem(ItemPool.RUFUS_SHADOW_LODESTONE));
    }
  }

  @Test
  public void lootingShadowForestSetsProperty() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("_shadowForestLooted", false),
            withItem(ItemPool.SHADOW_FLAME, 0),
            withItem(ItemPool.SHADOW_NECTAR, 0),
            withItem(ItemPool.SHADOW_STICK, 0),
            withItem(ItemPool.RUFUS_SHADOW_LODESTONE, 1));
    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("adventure.php?snarfblat=567")), "");
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_follow_rufus_lodestone.html"));
      client.addResponse(200, ""); // api.php
      client.addResponse(200, html("request/test_loot_shadow_forest.html"));
      client.addResponse(200, ""); // api.php

      var riftURL = "place.php?whichplace=woods&action=woods_shadowrift";
      var riftRequest = new GenericRequest(riftURL);
      riftRequest.run();

      // We are in a choice that you cannot walk away from.
      assertTrue(ChoiceManager.handlingChoice);
      assertEquals(1500, ChoiceManager.lastChoice);

      // Choose option 3 - The forest
      var choiceURL = "choice.php?pwd&whichchoice=1500&option=3";
      var choiceRequest = new GenericRequest(choiceURL);
      choiceRequest.run();

      // We are no longer in a choice
      assertFalse(ChoiceManager.handlingChoice);

      // We no longer have Rufus's shadow lodestone
      assertFalse(InventoryManager.hasItem(ItemPool.RUFUS_SHADOW_LODESTONE));

      // We have looted the forest today
      assertThat("_shadowForestLooted", isSetTo(true));
      assertEquals(2, InventoryManager.getCount(ItemPool.SHADOW_FLAME));
      assertEquals(3, InventoryManager.getCount(ItemPool.SHADOW_NECTAR));
      assertEquals(3, InventoryManager.getCount(ItemPool.SHADOW_STICK));
    }
  }

  @Nested
  class ItemsQuest {
    @Test
    void acceptItemsQuestWithoutItemsIsStarted() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("rufusDesiredItems", "shadow sinew"),
              withItem(ItemPool.SHADOW_SINEW, 0),
              withQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED),
              withProperty("rufusQuestTarget", ""),
              withProperty("rufusQuestType", ""),
              withProperty("_shadowAffinityToday", false),
              withNoEffects());
      try (cleanups) {
        client.addResponse(200, html("request/test_accept_rufus_quest_items.html"));
        var request = new GenericRequest("choice.php?pwd&whichchoice=1497&option=3");
        request.run();

        assertThat(Quest.RUFUS, isStarted());
        assertThat("rufusQuestType", isSetTo("items"));
        assertThat("rufusQuestTarget", isSetTo("shadow sinew"));

        // Since this was the first quest of the day, we have Shadow Affinity
        assertEquals(11, SHADOW_AFFINITY.getCount(KoLConstants.activeEffects));
        assertThat("_shadowAffinityToday", isSetTo(true));
      }
    }

    @Test
    void acceptItemsQuestWithEnoughItemsIsStep1() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("rufusDesiredItems", "shadow sinew"),
              withItem(ItemPool.SHADOW_SINEW, 3),
              withQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED),
              withProperty("rufusQuestTarget", ""),
              withProperty("rufusQuestType", ""),
              withProperty("_shadowAffinityToday", false),
              withNoEffects());
      try (cleanups) {
        client.addResponse(200, html("request/test_accept_rufus_quest_items.html"));
        var request = new GenericRequest("choice.php?pwd&whichchoice=1497&option=3");
        request.run();

        assertThat(Quest.RUFUS, isStep("step1"));
        assertThat("rufusQuestType", isSetTo("items"));
        assertThat("rufusQuestTarget", isSetTo("shadow sinew"));

        // Since this was the first quest of the day, we have Shadow Affinity
        assertEquals(11, SHADOW_AFFINITY.getCount(KoLConstants.activeEffects));
        assertThat("_shadowAffinityToday", isSetTo(true));
      }
    }

    @Test
    void gainingItemMightLeaveQuestStarted() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("rufusDesiredItems", "shadow sinew"),
              withItem(ItemPool.SHADOW_SINEW, 1),
              withQuestProgress(Quest.RUFUS, QuestDatabase.STARTED),
              withProperty("rufusQuestType", "items"),
              withProperty("rufusQuestTarget", "shadow sinew"));
      try (cleanups) {
        client.addResponse(200, html("request/test_closet_pull_shadow_item.html"));
        var request =
            new GenericRequest("inventory.php?action=closetpull&ajax=1&whichitem=11143&qty=1&pwd");
        request.run();

        assertEquals(2, InventoryManager.getCount(ItemPool.SHADOW_SINEW));
        assertThat(Quest.RUFUS, isStep("started"));
      }
    }

    @Test
    void gainingItemMightAdvanceQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("rufusDesiredItems", "shadow sinew"),
              withItem(ItemPool.SHADOW_SINEW, 2),
              withQuestProgress(Quest.RUFUS, QuestDatabase.STARTED),
              withProperty("rufusQuestType", "items"),
              withProperty("rufusQuestTarget", "shadow sinew"));
      try (cleanups) {
        client.addResponse(200, html("request/test_closet_pull_shadow_item.html"));
        var request =
            new GenericRequest("inventory.php?action=closetpull&ajax=1&whichitem=11143&qty=1&pwd");
        request.run();

        assertEquals(3, InventoryManager.getCount(ItemPool.SHADOW_SINEW));
        assertThat(Quest.RUFUS, isStep("step1"));
      }
    }

    @Test
    void losingItemMightLeaveQuestAdvanced() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("rufusDesiredItems", "shadow sinew"),
              withItem(ItemPool.SHADOW_SINEW, 4),
              withQuestProgress(Quest.RUFUS, "step1"),
              withProperty("rufusQuestType", "items"),
              withProperty("rufusQuestTarget", "shadow sinew"));
      try (cleanups) {
        client.addResponse(200, html("request/test_closet_push_shadow_item.html"));
        var request =
            new GenericRequest("inventory.php?action=closetpush&ajax=1&whichitem=11143&qty=1&pwd");
        request.run();

        assertEquals(3, InventoryManager.getCount(ItemPool.SHADOW_SINEW));
        assertThat(Quest.RUFUS, isStep("step1"));
      }
    }

    @Test
    void losingItemMightUnadvanceQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("rufusDesiredItems", "shadow sinew"),
              withItem(ItemPool.SHADOW_SINEW, 3),
              withQuestProgress(Quest.RUFUS, "step1"),
              withProperty("rufusQuestType", "items"),
              withProperty("rufusQuestTarget", "shadow sinew"));
      try (cleanups) {
        client.addResponse(200, html("request/test_closet_push_shadow_item.html"));
        var request =
            new GenericRequest("inventory.php?action=closetpush&ajax=1&whichitem=11143&qty=1&pwd");
        request.run();

        assertEquals(2, InventoryManager.getCount(ItemPool.SHADOW_SINEW));
        assertThat(Quest.RUFUS, isStep("started"));
      }
    }
  }

  @Nested
  class QuestLog {
    @Test
    void canParseEntityStartedInQuestLog() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED),
              withProperty("rufusQuestTarget", ""),
              withProperty("rufusQuestType", ""));
      try (cleanups) {
        client.addResponse(200, html("request/test_rufus_questlog_entity.html"));
        var request = new GenericRequest("questlog.php");
        request.run();

        assertThat(Quest.RUFUS, isStarted());
        assertThat("rufusQuestType", isSetTo("entity"));
        assertThat("rufusQuestTarget", isSetTo("shadow scythe"));
      }
    }

    @Test
    void canParseEntityDoneInQuestLog() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED),
              withProperty("rufusQuestTarget", ""),
              withProperty("rufusQuestType", ""));
      try (cleanups) {
        client.addResponse(200, html("request/test_rufus_questlog_entity_done.html"));
        var request = new GenericRequest("questlog.php");
        request.run();

        assertThat(Quest.RUFUS, isStep("step1"));
      }
    }

    @Test
    void canParseArtifactStartedInQuestLog() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED),
              withProperty("rufusQuestTarget", ""),
              withProperty("rufusQuestType", ""));
      try (cleanups) {
        client.addResponse(200, html("request/test_rufus_questlog_artifact.html"));
        var request = new GenericRequest("questlog.php");
        request.run();

        assertThat(Quest.RUFUS, isStarted());
        assertThat("rufusQuestType", isSetTo("artifact"));
        assertThat("rufusQuestTarget", isSetTo("shadow bucket"));
      }
    }

    @Test
    void canParseArtifactDoneInQuestLog() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED),
              withProperty("rufusQuestTarget", ""),
              withProperty("rufusQuestType", ""));
      try (cleanups) {
        client.addResponse(200, html("request/test_rufus_questlog_artifact_done.html"));
        var request = new GenericRequest("questlog.php");
        request.run();

        assertThat(Quest.RUFUS, isStep("step1"));
      }
    }

    @Test
    void canParseItemsStartedInQuestLog() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED),
              withProperty("rufusQuestTarget", ""),
              withProperty("rufusQuestType", ""));
      try (cleanups) {
        client.addResponse(200, html("request/test_rufus_questlog_items.html"));
        var request = new GenericRequest("questlog.php");
        request.run();

        assertThat(Quest.RUFUS, isStarted());
        assertThat("rufusQuestType", isSetTo("items"));
        assertThat("rufusQuestTarget", isSetTo("shadow flame"));
      }
    }

    @Test
    void canParseItemsDoneInQuestLog() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.RUFUS, QuestDatabase.UNSTARTED),
              withProperty("rufusQuestTarget", ""),
              withProperty("rufusQuestType", ""));
      try (cleanups) {
        client.addResponse(200, html("request/test_rufus_questlog_items_done.html"));
        var request = new GenericRequest("questlog.php");
        request.run();

        assertThat(Quest.RUFUS, isStep("step1"));
      }
    }
  }

  @Nested
  class ShadowLabyrinth {
    @Test
    void canDetectHotAdjective() {
      var text = "Opt for the white-hot cleft";
      ShadowTheme theme = RufusManager.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = RufusManager.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.FIRE);
      assertEquals("90-100 Muscle substats", spoiler.toString());
    }

    @Test
    void canDetectColdAdjective() {
      var text = "Opt for the iced-over passage";
      ShadowTheme theme = RufusManager.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = RufusManager.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.COLD);
      assertEquals("30 Shadow's Chill: Maximum MP +300%", spoiler.toString());
    }

    void canDetectWaterAdjective() {
      var text = "Leap into the sodden hole";
      ShadowTheme theme = RufusManager.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = RufusManager.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.WATER);
      assertEquals("90-100 Moxie substats", spoiler.toString());
    }

    @Test
    void canDetectMathAdjective() {
      var text = "Try to reach the irrational portal";
      ShadowTheme theme = RufusManager.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = RufusManager.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.MATH);
      assertEquals("90-100 Mysticality substats", spoiler.toString());
    }

    @Test
    void canDetectTimeAdjective() {
      var text = "Try to reach the old opening";
      ShadowTheme theme = RufusManager.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = RufusManager.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.TIME);
      assertEquals("+3 turns to 3 random effects", spoiler.toString());
    }

    @Test
    void canDetectBloodAdjective() {
      var text = "Walk to the vein-shot lane";
      ShadowTheme theme = RufusManager.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = RufusManager.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.BLOOD);
      assertEquals("30 Shadow's Heart: Maximum HP +300%", spoiler.toString());
    }

    @Test
    void canDetectGhostAdjective() {
      var text = "Try to reach the nearly invisible hole";
      ShadowTheme theme = RufusManager.shadowLabyrinthTheme(text);
      ChoiceOption spoiler = RufusManager.shadowLabyrinthSpoiler(text);
      assertEquals(theme, ShadowTheme.GHOST);
      assertEquals(
          "30 Shadow's Thickness: Superhuman (+5) Spooky, Hot, Sleaze resistance",
          spoiler.toString());
    }

    @ParameterizedTest
    @CsvSource({
      "Opt for the white-hot cleft, shadow lighter",
      "Opt for the iced-over passage, shadow snowflake",
      "Leap into the sodden hole, shadow bucket",
      "Try to reach the irrational portal, shadow heptahedron",
      "Walk to the vein-shot lane, shadow heart",
      "Try to reach the nearly invisible hole, shadow wave"
    })
    public void checkShadowArtifacts(String text, String artifact) {
      var cleanups =
          new Cleanups(
              withProperty("questRufus", "started"),
              withProperty("rufusQuestType", "artifact"),
              withProperty("rufusQuestTarget", artifact));
      try (cleanups) {
        ChoiceOption spoiler = RufusManager.shadowLabyrinthSpoiler(text);
        assertEquals(artifact, spoiler.toString());
      }
    }

    @Test
    void canParseLabyrinthOfShadows() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups = new Cleanups(withHttpClientBuilder(builder));
      try (cleanups) {
        String html = html("request/test_visit_labyrinth_of_shadows.html");
        client.addResponse(200, html);
        var request = new GenericRequest("choice.php?forceoption=0");
        request.run();

        var spoilers = ChoiceAdventures.choiceSpoilers(1499, new StringBuffer(html));
        var options = spoilers.getOptions();
        assertEquals("Randomize themes", options[0].toString());
        assertEquals("90-100 Muscle substats", options[1].toString());
        assertEquals("+3 turns to 3 random effects", options[2].toString());
        assertEquals("30 Shadow's Heart: Maximum HP +300%", options[3].toString());
        assertEquals("Randomize themes", options[4].toString());
        assertEquals("Leave with nothing", options[5].toString());
      }
    }

    private void checkLabyrinthOfShadowsAutomation(
        int configured, String responseText, boolean quest, String artifact, int expected) {
      var cleanups =
          new Cleanups(
              withProperty("choiceAdventure1499", configured),
              withQuestProgress(
                  Quest.RUFUS, quest ? QuestDatabase.STARTED : QuestDatabase.UNSTARTED),
              withProperty("rufusQuestType", quest ? "artifact" : ""),
              withProperty("rufusQuestTarget", quest ? artifact : ""));
      try (cleanups) {
        // ChoiceManager.getDecision ultimately calls RufusManager.specialChoiceDecision
        // Choice 1499 is Labyrinth of Shadows
        int result = ChoiceManager.getDecision(1499, responseText);
        assertEquals(expected, result);
      }
    }

    @Test
    void canAutomateLabyrinthOfShadows() {
      String html = html("request/test_visit_labyrinth_of_shadows.html");

      // Options provided by responseText:
      //
      // 1 -> "Move blindly ahead"
      // 2 -> "Opt for the white-hot cleft" -> FIRE
      // 3 -> "Try to reach the old opening" -> TIME
      // 4 -> "Select the blood-soaked void" -> BLOOD
      // 5 -> "Turn in a circle"
      // 6 -> "Walk away from all of this"
      //
      // User configured choice options:
      //
      // 0 -> show in browser (already eliminated)
      // 1 -> FIRE
      // 2 -> MATH
      // 3 -> WATER
      // 4 -> TIME
      // 5 -> BLOOD
      // 6 -> COLD
      // 7 -> GHOST
      //
      // Artifact themes:
      //
      // FIRE -> shadow lighter
      // MATH -> shadow heptahedron
      // WATER -> shadow bucket
      // TIME
      // BLOOD -> shadow hart
      // COLD -> shadow snowflake
      // GHOST -> shadow wave

      // With no artifact quest: FIRE -> 2, TIME -> 3, BLOOD -> 4
      checkLabyrinthOfShadowsAutomation(1, html, false, "", 2);
      checkLabyrinthOfShadowsAutomation(2, html, false, "", 1);
      checkLabyrinthOfShadowsAutomation(3, html, false, "", 1);
      checkLabyrinthOfShadowsAutomation(4, html, false, "", 3);
      checkLabyrinthOfShadowsAutomation(5, html, false, "", 4);
      checkLabyrinthOfShadowsAutomation(6, html, false, "", 1);
      checkLabyrinthOfShadowsAutomation(7, html, false, "", 1);

      // With artifact quest: shadow lighter -> 2, shadow heart -> 4
      checkLabyrinthOfShadowsAutomation(1, html, true, "shadow lighter", 2);
      checkLabyrinthOfShadowsAutomation(1, html, true, "shadow heptahedron", 1);
      checkLabyrinthOfShadowsAutomation(1, html, true, "shadow bucket", 1);
      checkLabyrinthOfShadowsAutomation(1, html, true, "shadow heart", 4);
      checkLabyrinthOfShadowsAutomation(1, html, true, "shadow snowflake", 1);
      checkLabyrinthOfShadowsAutomation(1, html, true, "shadow wave", 1);

      // With artifact quest: user's configured choice is ignored
      checkLabyrinthOfShadowsAutomation(1, html, true, "shadow heart", 4);
      checkLabyrinthOfShadowsAutomation(2, html, true, "shadow heart", 4);
      checkLabyrinthOfShadowsAutomation(3, html, true, "shadow heart", 4);
      checkLabyrinthOfShadowsAutomation(4, html, true, "shadow heart", 4);
      checkLabyrinthOfShadowsAutomation(5, html, true, "shadow heart", 4);
      checkLabyrinthOfShadowsAutomation(6, html, true, "shadow heart", 4);
      checkLabyrinthOfShadowsAutomation(7, html, true, "shadow heart", 4);
    }
  }
}
