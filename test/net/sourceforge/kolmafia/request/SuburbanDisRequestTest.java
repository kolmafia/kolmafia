package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withItem;
import static internal.matchers.Item.isInInventory;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SuburbanDisRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("SuburbanDisRequest");
    KoLCharacter.reset(true);
    Preferences.saveSettingsToFile = false;
  }

  @Test
  void getAdventuresUsedOnlyCostsForFighting() {
    var fight = new SuburbanDisRequest("dothis");
    assertEquals(fight.getAdventuresUsed(), 1);
  }

  @Test
  void getAdventuresUsedHasNoCostForAltar() {
    var fight = new SuburbanDisRequest("altar");
    assertEquals(fight.getAdventuresUsed(), 0);
  }

  @Test
  void processResultsDealsWithStonesFromDifferentZones() {
    var cleanups =
        new Cleanups(withItem(ItemPool.VANITY_STONE), withItem(ItemPool.LECHEROUS_STONE));

    try (cleanups) {
      QuestDatabase.setQuest(Quest.CLUMSINESS, QuestDatabase.FINISHED);
      QuestDatabase.setQuest(Quest.MAELSTROM, QuestDatabase.FINISHED);
      var differentZones = new SuburbanDisRequest(ItemPool.VANITY_STONE, ItemPool.LECHEROUS_STONE);
      differentZones.responseText = html("request/test_suburbandis_different_zone_stones.html");
      differentZones.processResults();

      assertThat(Quest.CLUMSINESS, isStep("step2"));
      assertThat(Quest.MAELSTROM, isStep("step2"));
      assertThat(ItemPool.VANITY_STONE, not(isInInventory()));
      assertThat(ItemPool.LECHEROUS_STONE, not(isInInventory()));
    }
  }

  @Test
  void processResultsDealsWithPartialQuestStatus() {
    var cleanups =
        new Cleanups(withItem(ItemPool.VANITY_STONE), withItem(ItemPool.LECHEROUS_STONE));

    try (cleanups) {
      QuestDatabase.setQuest(Quest.CLUMSINESS, "step3");
      QuestDatabase.setQuest(Quest.MAELSTROM, QuestDatabase.FINISHED);
      var differentZones = new SuburbanDisRequest(ItemPool.VANITY_STONE, ItemPool.LECHEROUS_STONE);
      differentZones.responseText = html("request/test_suburbandis_different_zone_stones.html");
      differentZones.processResults();

      assertThat(Quest.CLUMSINESS, isStep("step1"));
      assertThat(Quest.MAELSTROM, isStep("step2"));
      assertThat(ItemPool.VANITY_STONE, not(isInInventory()));
      assertThat(ItemPool.LECHEROUS_STONE, not(isInInventory()));
    }
  }

  @Test
  void processResultsDealsWithStonesFromSameZone() {
    var cleanups = new Cleanups(withItem(ItemPool.FURIOUS_STONE), withItem(ItemPool.VANITY_STONE));

    try (cleanups) {
      QuestDatabase.setQuest(Quest.CLUMSINESS, QuestDatabase.FINISHED);
      var differentZones = new SuburbanDisRequest(ItemPool.FURIOUS_STONE, ItemPool.VANITY_STONE);
      differentZones.responseText = html("request/test_suburbandis_same_zone_stones.html");
      differentZones.processResults();

      assertThat(Quest.CLUMSINESS, isStep("unstarted"));
      assertThat(ItemPool.FURIOUS_STONE, not(isInInventory()));
      assertThat(ItemPool.VANITY_STONE, not(isInInventory()));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "woods.php, false",
    "suburbandis.php, false",
    "suburbandis.php?action=altar, true",
    "suburbandis.php?action=dothis, true",
    "suburbandis.php?action=stoned, true",
    "suburbandis.php?action=stoned&stone1=5448, true",
    "suburbandis.php?action=stoned&stone1=5448&stone2=5449, true",
    "suburbandis.php?action=random, false"
  })
  void registerRequest(String url, String expected) {
    assertEquals(Boolean.parseBoolean(expected), SuburbanDisRequest.registerRequest(url));
  }
}
